#!/usr/bin/env python3
"""
Gerador do dataset sintético do PCP (ADR-0009).

Produz, de forma DETERMINÍSTICA (semente fixa), um conjunto de CSVs coerente com o
modelo de domínio expandido do ADR-0007:

    Material → Lista Técnica (BOM versionada) → Ordem de Produção
             → Consumo de Material (planejado × real, com desvios justificados)
             → Lote (gerado na conclusão da ordem)
    Compra de matéria-prima (NF) → Lote de compra
    Consumo ← Alocação → Lote (genealogia, ADR-0011)

Os CSVs são gravados em ``backend/src/main/resources/dados/`` e lidos pelo
``DataLoader`` (perfil Spring ``seed``). O script não é executado em runtime: ele é
rodado pelo desenvolvedor quando o dataset precisa mudar, e o resultado é versionado.

Padrões embutidos de propósito (matéria-prima para a IA da Fase 5c):
  * Sazonalidade + tendência na demanda de produtos acabados (previsão de demanda).
  * Gargalos: Tratamento Térmico e Soldagem atrasam mais (análise de atrasos).
  * Desvios de consumo com causas por centro de trabalho (refugo, setup, porosidade).
  * Troca de versão de BOM no meio do histórico (v1 OBSOLETA → v2 ATIVA).

Genealogia (ADR-0011): o estoque é simulado em ordem cronológica. Cada consumo é
alocado aos lotes disponíveis do componente pela regra FEFO (vence primeiro, sai
primeiro). Faltando matéria-prima, gera-se uma compra (lote com NF); faltando
semiacabado, uma ordem de reposição concluída pouco antes. O status final de cada lote
vem do saldo (zerado → CONSUMIDO).

Uso:
    python3 data/scripts/gerar_dataset_sintetico.py            # grava os CSVs
    python3 data/scripts/gerar_dataset_sintetico.py --verificar # falha se CSVs divergirem

Somente biblioteca padrão: a geração é linha a linha (sem agregações pesadas), então
pandas/Polars não trariam ganho e adicionariam dependência ao repositório.
"""

from __future__ import annotations

import argparse
import csv
import io
import math
import random
import sys
from dataclasses import dataclass, field
from datetime import date, timedelta
from decimal import ROUND_HALF_UP, Decimal
from pathlib import Path

# --------------------------------------------------------------------------------------
# Configuração
# --------------------------------------------------------------------------------------

SEMENTE = 42
# "Hoje" do dataset. O DataLoader desloca todas as datas para que esta data
# coincida com a data real da carga — o dashboard sempre mostra um cenário atual.
DATA_REFERENCIA = date(2026, 9, 30)
MESES_DE_HISTORICO = 18
DIAS_DE_HORIZONTE_FUTURO = 60
# Ordens vencidas há até N dias podem seguir abertas (atrasadas) — mais em gargalos.
JANELA_ATRASO_DIAS = 45

RAIZ = Path(__file__).resolve().parents[2]
DIRETORIO_SAIDA = RAIZ / "backend" / "src" / "main" / "resources" / "dados"

QUATRO_CASAS = Decimal("0.0001")

# --------------------------------------------------------------------------------------
# Dados mestres
# --------------------------------------------------------------------------------------

USINAGEM = "Usinagem CNC"
MONTAGEM = "Montagem"
SOLDAGEM = "Soldagem MIG/TIG"
PINTURA = "Pintura Industrial"
INSPECAO = "Inspeção de Qualidade"
FUNDICAO = "Fundição Sob Pressão"
ESTAMPARIA = "Estamparia"
TRATAMENTO = "Tratamento Térmico"


@dataclass(frozen=True)
class MaterialDef:
    codigo: str
    descricao: str
    tipo: str
    unidade: str
    centro: str | None = None          # centro que FABRICA o material (PA/SA)
    validade_meses: int | None = None  # prazo de validade do lote produzido
    demanda_base: int = 0              # unidades/mês (apenas PA)


MATERIAIS: list[MaterialDef] = [
    # Matérias-primas
    MaterialDef("MP-ACO-1045", "Barra redonda aço SAE 1045 Ø50mm", "MATERIA_PRIMA", "kg"),
    MaterialDef("MP-CHAPA-3MM", "Chapa aço carbono 3mm", "MATERIA_PRIMA", "kg"),
    MaterialDef("MP-ALU-A380", "Lingote de alumínio A380", "MATERIA_PRIMA", "kg"),
    MaterialDef("MP-ARAME-MIG", "Arame de solda MIG ER70S-6 1,0mm", "MATERIA_PRIMA", "kg"),
    MaterialDef("MP-TINTA-EPOXI", "Tinta epóxi industrial cinza", "MATERIA_PRIMA", "L"),
    MaterialDef("MP-OLEO-TEMPERA", "Óleo mineral para têmpera", "MATERIA_PRIMA", "L"),
    MaterialDef("MP-PARAF-M8", "Parafuso sextavado M8x25 classe 8.8", "MATERIA_PRIMA", "un"),
    MaterialDef("MP-ROLAM-6205", "Rolamento rígido de esferas 6205-2RS", "MATERIA_PRIMA", "un"),
    MaterialDef("MP-ORING-40", "Anel O-ring NBR 40x3mm", "MATERIA_PRIMA", "un"),
    MaterialDef("MP-GAXETA-NBR", "Gaxeta de vedação NBR", "MATERIA_PRIMA", "un"),
    # Semiacabados
    MaterialDef("SA-EIXO-USIN", "Eixo usinado Ø40mm", "SEMIACABADO", "un", USINAGEM, 60),
    MaterialDef("SA-EIXO-TEMP", "Eixo temperado e revenido Ø40mm", "SEMIACABADO", "un", TRATAMENTO, 60),
    MaterialDef("SA-CARC-FUND", "Carcaça bruta fundida em alumínio", "SEMIACABADO", "un", FUNDICAO, 60),
    MaterialDef("SA-CARC-USIN", "Carcaça usinada em alumínio", "SEMIACABADO", "un", USINAGEM, 60),
    MaterialDef("SA-SUPORTE-EST", "Suporte estampado em aço", "SEMIACABADO", "un", ESTAMPARIA, 36),
    MaterialDef("SA-ESTRUT-SOLD", "Estrutura soldada da base", "SEMIACABADO", "un", SOLDAGEM, 36),
    # Produtos acabados
    MaterialDef("PA-MOTOR-A200", "Conjunto motor A200", "PRODUTO_ACABADO", "un", MONTAGEM, 60, 90),
    MaterialDef("PA-REDUTOR-R50", "Redutor de velocidade R50", "PRODUTO_ACABADO", "un", MONTAGEM, 60, 60),
    MaterialDef("PA-VALVULA-V10", "Válvula de controle V10", "PRODUTO_ACABADO", "un", MONTAGEM, 48, 120),
    MaterialDef("PA-BASE-B30", "Base de fixação pintada B30", "PRODUTO_ACABADO", "un", PINTURA, 12, 70),
    MaterialDef("PA-SUPORTE-PINT", "Suporte pintado para painel", "PRODUTO_ACABADO", "un", PINTURA, 12, 150),
]

# BOM: (material, versão, status, [(componente, quantidade por unidade)])
# A unidade de medida do item é sempre a unidade do componente.
# Mudança de versão: a v2 entra em vigor em DATA_TROCA_BOM (v1 fica OBSOLETA).
LISTAS_TECNICAS: list[tuple[str, str, str, list[tuple[str, str]]]] = [
    ("SA-EIXO-USIN", "v1", "OBSOLETA", [("MP-ACO-1045", "2.6000")]),
    ("SA-EIXO-USIN", "v2", "ATIVA", [("MP-ACO-1045", "2.3500")]),  # otimização do corte
    ("SA-EIXO-TEMP", "v1", "ATIVA", [("SA-EIXO-USIN", "1"), ("MP-OLEO-TEMPERA", "0.1500")]),
    ("SA-CARC-FUND", "v1", "ATIVA", [("MP-ALU-A380", "3.2000")]),
    ("SA-CARC-USIN", "v1", "ATIVA", [("SA-CARC-FUND", "1")]),
    ("SA-SUPORTE-EST", "v1", "ATIVA", [("MP-CHAPA-3MM", "1.8500")]),
    ("SA-ESTRUT-SOLD", "v1", "ATIVA", [("SA-SUPORTE-EST", "4"), ("MP-ARAME-MIG", "0.2200")]),
    ("PA-MOTOR-A200", "v1", "OBSOLETA", [
        ("SA-CARC-USIN", "1"), ("SA-EIXO-TEMP", "1"), ("MP-ROLAM-6205", "2"), ("MP-PARAF-M8", "8"),
    ]),
    ("PA-MOTOR-A200", "v2", "ATIVA", [  # passou a levar O-ring de vedação
        ("SA-CARC-USIN", "1"), ("SA-EIXO-TEMP", "1"), ("MP-ROLAM-6205", "2"),
        ("MP-PARAF-M8", "6"), ("MP-ORING-40", "1"),
    ]),
    ("PA-REDUTOR-R50", "v1", "ATIVA", [
        ("SA-CARC-USIN", "1"), ("SA-EIXO-TEMP", "2"), ("MP-ROLAM-6205", "4"), ("MP-PARAF-M8", "10"),
    ]),
    ("PA-VALVULA-V10", "v1", "ATIVA", [
        ("SA-CARC-USIN", "1"), ("MP-ORING-40", "2"), ("MP-GAXETA-NBR", "1"), ("MP-PARAF-M8", "4"),
    ]),
    ("PA-BASE-B30", "v1", "ATIVA", [("SA-ESTRUT-SOLD", "1"), ("MP-TINTA-EPOXI", "0.8000")]),
    ("PA-SUPORTE-PINT", "v1", "ATIVA", [("SA-SUPORTE-EST", "1"), ("MP-TINTA-EPOXI", "0.1200")]),
]
DATA_TROCA_BOM = DATA_REFERENCIA - timedelta(days=150)

TIPOS_ORDEM: list[tuple[str, str, str]] = [
    ("Produção Normal", "Ordem planejada pelo MRP para atender a demanda prevista.", "#1565c0"),
    ("Reposição de Estoque", "Reposição de semiacabados para o estoque intermediário.", "#2e7d32"),
    ("Pedido Urgente", "Ordem encaixada fora do plano para atender cliente prioritário.", "#ef6c00"),
    ("Retrabalho", "Correção de peças reprovadas pela inspeção de qualidade.", "#c62828"),
]


@dataclass(frozen=True)
class PerfilCentro:
    lead_time_dias: tuple[int, int]  # duração planejada (mín, máx)
    atraso_medio_dias: float         # média do atraso real na conclusão
    prob_desvio: float               # chance de um consumo ter desvio
    causas_desvio: tuple[str, ...]


PERFIS_CENTRO: dict[str, PerfilCentro] = {
    USINAGEM: PerfilCentro((5, 10), 0.5, 0.25, (
        "Perda de material no setup da máquina",
        "Refugo por dimensional fora de tolerância",
        "Quebra de ferramenta durante o corte",
    )),
    MONTAGEM: PerfilCentro((4, 9), 0.8, 0.15, (
        "Componente danificado no manuseio",
        "Parafuso com rosca danificada substituído",
    )),
    SOLDAGEM: PerfilCentro((4, 8), 2.5, 0.35, (
        "Retrabalho de cordão de solda",
        "Consumo extra de arame por ajuste de parâmetro",
        "Peça refugada por trinca na solda",
    )),
    PINTURA: PerfilCentro((2, 4), 0.3, 0.30, (
        "Variação na espessura da camada de tinta",
        "Repintura por falha de aderência",
    )),
    INSPECAO: PerfilCentro((1, 3), 0.2, 0.10, ("Peça destruída em ensaio de amostragem",)),
    FUNDICAO: PerfilCentro((4, 8), 1.2, 0.40, (
        "Refugo por porosidade na fundição",
        "Perda de metal no canal de alimentação",
    )),
    ESTAMPARIA: PerfilCentro((2, 5), 0.4, 0.20, (
        "Perda de chapa no ajuste da ferramenta",
        "Rebarba acima do limite gerou refugo",
    )),
    TRATAMENTO: PerfilCentro((3, 6), 3.5, 0.20, (
        "Reprocesso por dureza abaixo da especificação",
        "Troca parcial do óleo de têmpera",
    )),
}

RESPONSAVEIS = ("supervisor.turno.a", "supervisor.turno.b", "supervisor.turno.c", "analista.pcp")

# Compras de matéria-prima: fornecedores, validade do lote e embalagem (múltiplo de compra).
FORNECEDORES: dict[str, tuple[str, ...]] = {
    "MP-ACO-1045": ("Aços Brasil Ltda", "Siderúrgica Vale do Aço S.A."),
    "MP-CHAPA-3MM": ("Aços Brasil Ltda", "Laminados Paulista Ltda"),
    "MP-ALU-A380": ("Alumínio Nordeste Ltda", "Metais Leves do Sul S.A."),
    "MP-ARAME-MIG": ("Soldas Técnicas Ltda",),
    "MP-TINTA-EPOXI": ("Tintas Industriais Cores Ltda", "Revestimentos Proteq S.A."),
    "MP-OLEO-TEMPERA": ("Lubrificantes Petroquímica Ltda",),
    "MP-PARAF-M8": ("Fixadores Paraná Ltda", "Parafusos Garra S.A."),
    "MP-ROLAM-6205": ("Rolamentos Precisão Ltda",),
    "MP-ORING-40": ("Vedações Borrachas Ltda",),
    "MP-GAXETA-NBR": ("Vedações Borrachas Ltda",),
}
VALIDADE_COMPRA_MESES = {"MP-TINTA-EPOXI": 12, "MP-OLEO-TEMPERA": 24, "MP-ORING-40": 36, "MP-GAXETA-NBR": 36}
VALIDADE_COMPRA_PADRAO_MESES = 60
EMBALAGEM = {"kg": Decimal("25"), "L": Decimal("20"), "un": Decimal("50")}

# --------------------------------------------------------------------------------------
# Estruturas geradas
# --------------------------------------------------------------------------------------


@dataclass
class Ordem:
    codigo: str
    material: MaterialDef
    versao_lista: str
    tipo_ordem: str
    centro: str
    quantidade: int
    inicio: date
    fim: date
    status: str = "PLANEJADA"
    quantidade_produzida: Decimal | None = None
    data_conclusao: date | None = None
    consumos: list[dict] = field(default_factory=list)


@dataclass
class LoteSim:
    """Lote na simulação de estoque: de produção (ordem) ou de compra (nota fiscal)."""
    material: MaterialDef
    quantidade: Decimal
    fabricacao: date
    validade: date
    disponivel_a_partir: date          # conclusão da ordem ou recebimento da compra
    ordem: Ordem | None = None
    compra: dict | None = None         # fornecedor, nota_fiscal, emissao, recebimento
    bloqueado: bool = False
    saldo: Decimal = Decimal("0")
    status: str = "DISPONIVEL"

    def __post_init__(self) -> None:
        self.saldo = self.quantidade


# --------------------------------------------------------------------------------------
# Pipeline: load → transform → validate → export
# --------------------------------------------------------------------------------------


def load_dados_mestres() -> tuple[dict[str, MaterialDef], dict[tuple[str, str], list[tuple[str, Decimal]]]]:
    materiais = {m.codigo: m for m in MATERIAIS}
    boms = {(mat, versao): [(comp, Decimal(qtd)) for comp, qtd in itens]
            for mat, versao, _status, itens in LISTAS_TECNICAS}
    return materiais, boms


def _q4(valor: Decimal) -> Decimal:
    return valor.quantize(QUATRO_CASAS, rounding=ROUND_HALF_UP)


def _versao_vigente(codigo_material: str, inicio: date) -> str:
    versoes = [v for m, v, _s, _i in LISTAS_TECNICAS if m == codigo_material]
    if len(versoes) > 1 and inicio < DATA_TROCA_BOM:
        return versoes[0]
    return versoes[-1]


def _fator_sazonal(mes: int) -> float:
    # Pico no 2º semestre (safra industrial / fechamento de ano), vale em jan–fev.
    return 1.0 + 0.25 * math.sin((mes - 4) / 12 * 2 * math.pi)


def transform_ordens(rng: random.Random, materiais: dict[str, MaterialDef],
                     boms: dict[tuple[str, str], list[tuple[str, Decimal]]]) -> list[Ordem]:
    """Gera as ordens de PA (demanda) e as de SA (explosão da BOM em 1 nível por vez)."""
    inicio_historico = (DATA_REFERENCIA.replace(day=1)
                        - timedelta(days=30 * MESES_DE_HISTORICO)).replace(day=1)
    fim_horizonte = DATA_REFERENCIA + timedelta(days=DIAS_DE_HORIZONTE_FUTURO)

    pedidos: list[tuple[date, MaterialDef, int, str]] = []
    total_meses = MESES_DE_HISTORICO + 3
    for pa in (m for m in MATERIAIS if m.tipo == "PRODUTO_ACABADO"):
        for i in range(total_meses):
            ano = inicio_historico.year + (inicio_historico.month - 1 + i) // 12
            mes = (inicio_historico.month - 1 + i) % 12 + 1
            tendencia = 1.0 + 0.015 * i  # crescimento de ~1,5% ao mês
            demanda = pa.demanda_base * _fator_sazonal(mes) * tendencia * rng.uniform(0.85, 1.15)
            # A demanda do mês é quebrada em 1–3 ordens.
            partes = rng.choice((1, 2, 2, 3))
            for p in range(partes):
                dia = min(28, 1 + p * 10 + rng.randint(0, 6))
                inicio = date(ano, mes, dia)
                if inicio < inicio_historico or inicio > fim_horizonte:
                    continue
                qtd = max(5, round(demanda / partes / 5) * 5)
                tipo = "Pedido Urgente" if rng.random() < 0.08 else "Produção Normal"
                pedidos.append((inicio, pa, qtd, tipo))

    # Explosão da BOM: cada ordem pai gera ordens dos semiacabados antes do seu início.
    fila = list(pedidos)
    todas: list[tuple[date, MaterialDef, int, str]] = []
    while fila:
        inicio, mat, qtd, tipo = fila.pop()
        todas.append((inicio, mat, qtd, tipo))
        versao = _versao_vigente(mat.codigo, inicio)
        for comp_codigo, qtd_item in boms[(mat.codigo, versao)]:
            comp = materiais[comp_codigo]
            if comp.tipo != "SEMIACABADO":
                continue
            lead = PERFIS_CENTRO[comp.centro].lead_time_dias[1]
            inicio_comp = inicio - timedelta(days=lead + rng.randint(1, 4))
            if inicio_comp < inicio_historico:
                continue
            qtd_comp = int(qtd * qtd_item)
            fila.append((inicio_comp, comp, qtd_comp, "Reposição de Estoque"))

    todas.sort(key=lambda t: (t[0], t[1].codigo))
    ordens: list[Ordem] = []
    sequenciais: dict[int, int] = {}
    for inicio, mat, qtd, tipo in todas:
        perfil = PERFIS_CENTRO[mat.centro]
        fim = inicio + timedelta(days=rng.randint(*perfil.lead_time_dias))
        sequenciais[inicio.year] = sequenciais.get(inicio.year, 0) + 1
        codigo = f"OP-{inicio.year}-{sequenciais[inicio.year]:04d}"
        ordens.append(Ordem(codigo, mat, _versao_vigente(mat.codigo, inicio), tipo,
                            mat.centro, qtd, inicio, fim))

    _adicionar_retrabalhos(rng, ordens, sequenciais)
    ordens.sort(key=lambda o: (o.inicio, o.codigo))
    return ordens


def _adicionar_retrabalhos(rng: random.Random, ordens: list[Ordem], sequenciais: dict[int, int]) -> None:
    """Algumas ordens de PA geram retrabalho pequeno na Inspeção de Qualidade."""
    for origem in [o for o in ordens if o.material.tipo == "PRODUTO_ACABADO"]:
        if rng.random() >= 0.06:
            continue
        inicio = origem.fim + timedelta(days=rng.randint(1, 3))
        if inicio > DATA_REFERENCIA + timedelta(days=DIAS_DE_HORIZONTE_FUTURO):
            continue
        sequenciais[inicio.year] = sequenciais.get(inicio.year, 0) + 1
        codigo = f"OP-{inicio.year}-{sequenciais[inicio.year]:04d}"
        fim = inicio + timedelta(days=rng.randint(*PERFIS_CENTRO[INSPECAO].lead_time_dias))
        qtd = max(1, round(origem.quantidade * rng.uniform(0.03, 0.10)))
        ordens.append(Ordem(codigo, origem.material, origem.versao_lista, "Retrabalho",
                            INSPECAO, qtd, inicio, fim))


def transform_status(rng: random.Random, ordens: list[Ordem]) -> None:
    """Define o status de cada ordem a partir das datas e do perfil do centro."""
    for o in ordens:
        perfil = PERFIS_CENTRO[o.centro]
        atraso = max(-2, round(rng.gauss(perfil.atraso_medio_dias, 1.5 + perfil.atraso_medio_dias / 2)))
        conclusao_prevista = o.fim + timedelta(days=atraso)

        if o.inicio > DATA_REFERENCIA:
            o.status = "LIBERADA" if (o.inicio - DATA_REFERENCIA).days <= 7 and rng.random() < 0.5 else "PLANEJADA"
        elif rng.random() < 0.035:
            o.status = "CANCELADA"
        elif ((DATA_REFERENCIA - o.fim).days <= JANELA_ATRASO_DIAS
              and rng.random() < 0.20 + 0.10 * perfil.atraso_medio_dias):
            o.status = "EM_PRODUCAO"  # travada no gargalo: aberta após o fim planejado
        elif conclusao_prevista <= DATA_REFERENCIA - timedelta(days=1):
            o.status = "CONCLUIDA"
            o.data_conclusao = max(conclusao_prevista, o.inicio)
        else:
            o.status = "EM_PRODUCAO" if rng.random() < 0.8 else "LIBERADA"


def transform_consumos(rng: random.Random, ordens: list[Ordem], materiais: dict[str, MaterialDef],
                       boms: dict[tuple[str, str], list[tuple[str, Decimal]]]) -> None:
    """Projeta consumos pela BOM e registra o consumo real das ordens concluídas."""
    for o in ordens:
        perfil = PERFIS_CENTRO[o.centro]
        registrar_todos = o.status == "CONCLUIDA"
        registrar_parcial = o.status == "EM_PRODUCAO"
        for comp_codigo, qtd_item in boms[(o.material.codigo, o.versao_lista)]:
            comp = materiais[comp_codigo]
            planejada = _q4(qtd_item * o.quantidade)
            consumo = {"componente": comp, "planejada": planejada, "consumida": None,
                       "justificativa": "", "justificado_por": ""}
            if registrar_todos or (registrar_parcial and rng.random() < 0.5):
                consumida = planejada
                if rng.random() < perfil.prob_desvio:
                    fator = Decimal(str(round(rng.uniform(1.01, 1.08), 4)))
                    if rng.random() < 0.15:  # economia pontual
                        fator = Decimal(str(round(rng.uniform(0.96, 0.995), 4)))
                    consumida = planejada * fator
                    if comp.unidade == "un":
                        consumida = consumida.to_integral_value(rounding=ROUND_HALF_UP)
                    consumida = _q4(consumida)
                    if consumida != planejada:
                        consumo["justificativa"] = (rng.choice(perfil.causas_desvio)
                                                    if consumida > planejada
                                                    else "Aproveitamento de sobra do lote anterior")
                        consumo["justificado_por"] = rng.choice(RESPONSAVEIS)
                consumo["consumida"] = consumida
            o.consumos.append(consumo)

        if o.status == "CONCLUIDA":
            perda = Decimal("1")
            if rng.random() < perfil.prob_desvio:
                perda = Decimal(str(round(rng.uniform(0.92, 0.99), 4)))
            produzida = (Decimal(o.quantidade) * perda).to_integral_value(rounding=ROUND_HALF_UP)
            o.quantidade_produzida = max(Decimal(1), produzida)


def transform_lotes(rng: random.Random, ordens: list[Ordem]) -> list[LoteSim]:
    """Um lote por ordem concluída; ~3% ficam retidos pela qualidade (BLOQUEADO)."""
    return [_lote_de_producao(o, bloqueado=rng.random() < 0.03)
            for o in ordens if o.status == "CONCLUIDA"]


def _lote_de_producao(o: Ordem, bloqueado: bool = False) -> LoteSim:
    return LoteSim(o.material, Decimal(o.quantidade_produzida), o.data_conclusao,
                   _somar_meses(o.data_conclusao, o.material.validade_meses), o.data_conclusao,
                   ordem=o, bloqueado=bloqueado)


def _data_do_consumo(o: Ordem) -> date:
    """Concluída: no dia da conclusão. Em produção: até o fim planejado, nunca no futuro."""
    if o.data_conclusao:
        return o.data_conclusao
    return max(o.inicio, min(o.fim, DATA_REFERENCIA - timedelta(days=1)))


class SimuladorEstoque:
    """Aloca os consumos aos lotes (FEFO), comprando matéria-prima e produzindo
    semiacabado sob demanda quando falta saldo."""

    def __init__(self, rng: random.Random, ordens: list[Ordem], lotes: list[LoteSim],
                 materiais: dict[str, MaterialDef],
                 boms: dict[tuple[str, str], list[tuple[str, Decimal]]]) -> None:
        self.rng = rng
        self.ordens = ordens
        self.lotes = lotes
        self.materiais = materiais
        self.boms = boms
        self.alocacoes: list[dict] = []
        self.sequenciais: dict[int, int] = {}
        for o in ordens:
            ano, seq = o.codigo.split("-")[1:]
            self.sequenciais[int(ano)] = max(self.sequenciais.get(int(ano), 0), int(seq))
        self.proxima_nf = 100_001
        self.demanda_diaria = self._demanda_diaria_mp()

    def _demanda_diaria_mp(self) -> dict[str, Decimal]:
        total: dict[str, Decimal] = {}
        for o in self.ordens:
            for c in o.consumos:
                if c["componente"].tipo == "MATERIA_PRIMA" and c["consumida"]:
                    total[c["componente"].codigo] = total.get(c["componente"].codigo, Decimal(0)) + c["consumida"]
        dias = Decimal(30 * MESES_DE_HISTORICO)
        return {codigo: qtd / dias for codigo, qtd in total.items()}

    def executar(self) -> None:
        pendentes = [(o, c) for o in self.ordens for c in o.consumos if c["consumida"]]
        pendentes.sort(key=lambda oc: (_data_do_consumo(oc[0]), oc[0].codigo, oc[1]["componente"].codigo))
        for o, c in pendentes:
            c["data_registro"] = _data_do_consumo(o)
            self._alocar(o, c, c["data_registro"])
        self._repor_para_carteira()
        for lote in self.lotes:
            if lote.saldo == 0:
                lote.status = "CONSUMIDO"
            elif lote.validade < DATA_REFERENCIA:
                lote.status = "VENCIDO"
            elif lote.bloqueado:
                lote.status = "BLOQUEADO"
            else:
                lote.status = "DISPONIVEL"

    def _repor_para_carteira(self) -> None:
        """Garante saldo para os consumos ainda não registrados das ordens em produção —
        o cenário "atual" precisa permitir registrar consumo pela tela. Semiacabados
        primeiro: a reposição deles consome matéria-prima."""
        pendente: dict[str, Decimal] = {}
        for o in self.ordens:
            if o.status == "EM_PRODUCAO":
                for c in o.consumos:
                    if c["consumida"] is None:
                        codigo = c["componente"].codigo
                        pendente[codigo] = pendente.get(codigo, Decimal(0)) + c["planejada"] * Decimal("1.10")
        for tipo in ("SEMIACABADO", "MATERIA_PRIMA"):
            for codigo in sorted(pendente):
                material = self.materiais[codigo]
                if material.tipo != tipo:
                    continue
                disponivel = sum((lote.saldo for lote in self._elegiveis(material, DATA_REFERENCIA)), Decimal(0))
                if disponivel < pendente[codigo]:
                    falta = pendente[codigo] - disponivel
                    if tipo == "MATERIA_PRIMA":
                        self._comprar(material, falta, DATA_REFERENCIA)
                    else:
                        self._produzir(material, falta, DATA_REFERENCIA)

    def _elegiveis(self, material: MaterialDef, data: date) -> list[LoteSim]:
        # Disponível antes do dia do consumo (estritamente) e válido até ele.
        lotes = [lote for lote in self.lotes
                 if lote.material is material and lote.saldo > 0 and not lote.bloqueado
                 and lote.disponivel_a_partir < data <= lote.validade]
        return sorted(lotes, key=lambda lote: (lote.validade, lote.fabricacao))

    def _alocar(self, o: Ordem, consumo: dict, data: date) -> None:
        restante = consumo["consumida"]
        material = consumo["componente"]
        for lote in self._elegiveis(material, data):
            restante = self._retirar(o, consumo, lote, restante, data)
            if restante == 0:
                return
        novo = self._comprar(material, restante, data) if material.tipo == "MATERIA_PRIMA" \
            else self._produzir(material, restante, data)
        restante = self._retirar(o, consumo, novo, restante, data)
        assert restante == 0, f"{o.codigo}: falta de {material.codigo}"

    def _retirar(self, o: Ordem, consumo: dict, lote: LoteSim, restante: Decimal, data: date) -> Decimal:
        quantidade = min(lote.saldo, restante)
        lote.saldo -= quantidade
        self.alocacoes.append({"ordem": o, "consumo": consumo, "lote": lote,
                               "quantidade": quantidade, "data": data})
        return restante - quantidade

    def _comprar(self, material: MaterialDef, falta: Decimal, data: date) -> LoteSim:
        """Compra que cobre a falta e algumas semanas de demanda, em embalagens fechadas."""
        cobertura = self.demanda_diaria.get(material.codigo, Decimal(0)) * Decimal(self.rng.randint(20, 45))
        embalagem = EMBALAGEM[material.unidade]
        quantidade = (max(falta, cobertura) / embalagem).to_integral_value(rounding="ROUND_CEILING") * embalagem
        recebimento = data - timedelta(days=self.rng.randint(1, 6))
        emissao = recebimento - timedelta(days=self.rng.randint(0, 4))
        fabricacao = emissao - timedelta(days=self.rng.randint(3, 40))
        validade = _somar_meses(fabricacao, VALIDADE_COMPRA_MESES.get(material.codigo,
                                                                     VALIDADE_COMPRA_PADRAO_MESES))
        compra = {"fornecedor": self.rng.choice(FORNECEDORES[material.codigo]),
                  "nota_fiscal": f"{self.proxima_nf:06d}", "emissao": emissao, "recebimento": recebimento}
        self.proxima_nf += 1
        lote = LoteSim(material, _q4(quantidade), fabricacao, validade, recebimento, compra=compra)
        self.lotes.append(lote)
        return lote

    def _produzir(self, material: MaterialDef, falta: Decimal, data: date) -> LoteSim:
        """Ordem de reposição concluída pouco antes do consumo, com seus próprios consumos alocados."""
        quantidade = int((falta * Decimal(str(round(self.rng.uniform(1.05, 1.25), 2))))
                         .to_integral_value(rounding="ROUND_CEILING"))
        conclusao = data - timedelta(days=self.rng.randint(1, 3))
        inicio = conclusao - timedelta(days=self.rng.randint(*PERFIS_CENTRO[material.centro].lead_time_dias))
        self.sequenciais[inicio.year] = self.sequenciais.get(inicio.year, 0) + 1
        codigo = f"OP-{inicio.year}-{self.sequenciais[inicio.year]:04d}"
        versao = _versao_vigente(material.codigo, inicio)
        o = Ordem(codigo, material, versao, "Reposição de Estoque", material.centro, quantidade,
                  inicio, conclusao, status="CONCLUIDA", quantidade_produzida=Decimal(quantidade),
                  data_conclusao=conclusao)
        for comp_codigo, qtd_item in self.boms[(material.codigo, versao)]:
            planejada = _q4(qtd_item * quantidade)
            consumo = {"componente": self.materiais[comp_codigo], "planejada": planejada,
                       "consumida": planejada, "justificativa": "", "justificado_por": "",
                       "data_registro": conclusao}
            o.consumos.append(consumo)
            self._alocar(o, consumo, conclusao)
        self.ordens.append(o)
        lote = _lote_de_producao(o)
        self.lotes.append(lote)
        return lote


def transform_genealogia(ordens: list[Ordem], lotes: list[LoteSim], materiais: dict[str, MaterialDef],
                         boms: dict[tuple[str, str], list[tuple[str, Decimal]]]) -> list[dict]:
    """Simula o estoque e devolve as alocações consumo → lote. Semente própria: não
    altera a sequência aleatória das etapas anteriores."""
    simulador = SimuladorEstoque(random.Random(SEMENTE + 1), ordens, lotes, materiais, boms)
    simulador.executar()
    ordens.sort(key=lambda o: (o.inicio, o.codigo))
    return simulador.alocacoes


def _somar_meses(d: date, meses: int) -> date:
    total = d.month - 1 + meses
    ano, mes = d.year + total // 12, total % 12 + 1
    return date(ano, mes, min(d.day, 28))


def validate_dataset(ordens: list[Ordem], lotes: list[LoteSim], alocacoes: list[dict],
                     materiais: dict[str, MaterialDef]) -> None:
    """Garante as invariantes do domínio ANTES de exportar (falha rápido)."""
    erros: list[str] = []
    codigos = [o.codigo for o in ordens]
    if len(codigos) != len(set(codigos)):
        erros.append("códigos de ordem duplicados")
    for o in ordens:
        if o.fim < o.inicio:
            erros.append(f"{o.codigo}: fim antes do início")
        if o.quantidade <= 0:
            erros.append(f"{o.codigo}: quantidade não positiva")
        if o.status == "CONCLUIDA":
            if any(c["consumida"] is None for c in o.consumos):
                erros.append(f"{o.codigo}: concluída com consumo não registrado")
            if not o.quantidade_produzida or o.quantidade_produzida <= 0:
                erros.append(f"{o.codigo}: concluída sem quantidade produzida")
        for c in o.consumos:
            if c["consumida"] is not None and c["consumida"] != c["planejada"] and not c["justificativa"]:
                erros.append(f"{o.codigo}: desvio sem justificativa em {c['componente'].codigo}")
    alocado_por_lote: dict[int, Decimal] = {}
    alocado_por_consumo: dict[int, Decimal] = {}
    for a in alocacoes:
        lote, consumo = a["lote"], a["consumo"]
        alocado_por_lote[id(lote)] = alocado_por_lote.get(id(lote), Decimal(0)) + a["quantidade"]
        alocado_por_consumo[id(consumo)] = alocado_por_consumo.get(id(consumo), Decimal(0)) + a["quantidade"]
        if lote.material is not consumo["componente"]:
            erros.append(f"{a['ordem'].codigo}: lote de outro material em {consumo['componente'].codigo}")
        if not lote.disponivel_a_partir < a["data"] <= lote.validade:
            erros.append(f"{a['ordem'].codigo}: lote fora da janela de uso em {a['data']}")
        if a["quantidade"] <= 0:
            erros.append(f"{a['ordem'].codigo}: alocação não positiva")
    for o in ordens:
        for c in o.consumos:
            if c["consumida"] and alocado_por_consumo.get(id(c), Decimal(0)) != c["consumida"]:
                erros.append(f"{o.codigo}: alocações ≠ consumido em {c['componente'].codigo}")
    for lote in lotes:
        ref = lote.ordem.codigo if lote.ordem else f"NF {lote.compra['nota_fiscal']}"
        if lote.validade < lote.fabricacao:
            erros.append(f"{ref}: validade antes da fabricação")
        if lote.fabricacao > DATA_REFERENCIA:
            erros.append(f"{ref}: lote fabricado no futuro")
        if lote.saldo < 0 or lote.saldo + alocado_por_lote.get(id(lote), Decimal(0)) != lote.quantidade:
            erros.append(f"{ref}: saldo inconsistente")
        if (lote.saldo == 0) != (lote.status == "CONSUMIDO"):
            erros.append(f"{ref}: status {lote.status} com saldo {lote.saldo}")
        if lote.compra and not (lote.fabricacao <= lote.compra["recebimento"]
                                and lote.compra["emissao"] <= lote.compra["recebimento"]):
            erros.append(f"{ref}: datas da compra incoerentes")
    for mat, _v, _s, itens in LISTAS_TECNICAS:
        if materiais[mat].tipo == "MATERIA_PRIMA":
            erros.append(f"{mat}: matéria-prima não pode ter lista técnica")
        for comp, _q in itens:
            if comp not in materiais:
                erros.append(f"{mat}: componente desconhecido {comp}")
    if erros:
        raise SystemExit("Dataset inválido:\n  - " + "\n  - ".join(erros[:20]))


def _csv(cabecalho: list[str], linhas: list[list]) -> str:
    buffer = io.StringIO()
    escritor = csv.writer(buffer, lineterminator="\n")
    escritor.writerow(cabecalho)
    escritor.writerows(["" if v is None else v for v in linha] for linha in linhas)
    return buffer.getvalue()


def export_csvs(ordens: list[Ordem], lotes: list[LoteSim], alocacoes: list[dict]) -> dict[str, str]:
    producao = sorted((lote for lote in lotes if lote.ordem), key=lambda lote: (lote.fabricacao, lote.ordem.codigo))
    compras = sorted((lote for lote in lotes if lote.compra), key=lambda lote: lote.compra["nota_fiscal"])
    arquivos = {
        "dataset.properties": (
            "# Gerado por data/scripts/gerar_dataset_sintetico.py — não edite à mão.\n"
            f"data_referencia={DATA_REFERENCIA.isoformat()}\n"
            f"semente={SEMENTE}\n"
        ),
        "materiais.csv": _csv(
            ["codigo", "descricao", "tipo", "unidade_de_medida"],
            [[m.codigo, m.descricao, m.tipo, m.unidade] for m in MATERIAIS]),
        "tipos_ordem.csv": _csv(["nome", "descricao", "cor"], [list(t) for t in TIPOS_ORDEM]),
        "listas_tecnicas.csv": _csv(
            ["material_codigo", "versao", "status"],
            [[m, v, s] for m, v, s, _i in LISTAS_TECNICAS]),
        "itens_lista_tecnica.csv": _csv(
            ["material_codigo", "versao", "componente_codigo", "quantidade_planejada"],
            [[m, v, comp, qtd] for m, v, _s, itens in LISTAS_TECNICAS for comp, qtd in itens]),
        "ordens_producao.csv": _csv(
            ["codigo", "material_codigo", "versao_lista", "tipo_ordem", "centro_de_trabalho",
             "quantidade", "quantidade_produzida", "inicio_planejado", "fim_planejado", "status",
             "data_conclusao"],
            [[o.codigo, o.material.codigo, o.versao_lista, o.tipo_ordem, o.centro, o.quantidade,
              o.quantidade_produzida, o.inicio.isoformat(), o.fim.isoformat(), o.status,
              o.data_conclusao.isoformat() if o.data_conclusao else None]
             for o in ordens]),
        "consumos_material.csv": _csv(
            ["ordem_codigo", "componente_codigo", "quantidade_planejada", "quantidade_consumida",
             "justificativa", "justificado_por", "data_registro"],
            [[o.codigo, c["componente"].codigo, c["planejada"], c["consumida"],
              c["justificativa"], c["justificado_por"],
              c["data_registro"].isoformat() if c.get("data_registro") else None]
             for o in ordens for c in o.consumos]),
        "lotes.csv": _csv(
            ["ordem_codigo", "data_fabricacao", "data_validade", "status", "saldo"],
            [[lote.ordem.codigo, lote.fabricacao.isoformat(), lote.validade.isoformat(),
              lote.status, _q4(lote.saldo)] for lote in producao]),
        "lotes_compra.csv": _csv(
            ["material_codigo", "fornecedor", "nota_fiscal", "data_emissao_nf", "data_recebimento",
             "quantidade", "data_fabricacao", "data_validade", "status", "saldo"],
            [[lote.material.codigo, lote.compra["fornecedor"], lote.compra["nota_fiscal"],
              lote.compra["emissao"].isoformat(), lote.compra["recebimento"].isoformat(),
              lote.quantidade, lote.fabricacao.isoformat(), lote.validade.isoformat(),
              lote.status, _q4(lote.saldo)] for lote in compras]),
        "alocacoes_lote.csv": _csv(
            ["ordem_codigo", "componente_codigo", "lote_ordem_codigo", "lote_nota_fiscal", "quantidade"],
            [[a["ordem"].codigo, a["consumo"]["componente"].codigo,
              a["lote"].ordem.codigo if a["lote"].ordem else None,
              a["lote"].compra["nota_fiscal"] if a["lote"].compra else None,
              _q4(a["quantidade"])]
             for a in sorted(alocacoes, key=lambda a: (a["data"], a["ordem"].codigo,
                                                       a["consumo"]["componente"].codigo))]),
    }
    return arquivos


def gerar() -> dict[str, str]:
    rng = random.Random(SEMENTE)
    materiais, boms = load_dados_mestres()
    ordens = transform_ordens(rng, materiais, boms)
    transform_status(rng, ordens)
    transform_consumos(rng, ordens, materiais, boms)
    lotes = transform_lotes(rng, ordens)
    alocacoes = transform_genealogia(ordens, lotes, materiais, boms)
    validate_dataset(ordens, lotes, alocacoes, materiais)
    return export_csvs(ordens, lotes, alocacoes)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--verificar", action="store_true",
                        help="não grava; retorna erro se os CSVs versionados estiverem desatualizados")
    args = parser.parse_args()

    arquivos = gerar()
    if args.verificar:
        divergentes = [nome for nome, conteudo in arquivos.items()
                       if not (DIRETORIO_SAIDA / nome).exists()
                       or (DIRETORIO_SAIDA / nome).read_text(encoding="utf-8") != conteudo]
        if divergentes:
            print("CSVs desatualizados: " + ", ".join(divergentes), file=sys.stderr)
            return 1
        print("Dataset sintético em dia.")
        return 0

    DIRETORIO_SAIDA.mkdir(parents=True, exist_ok=True)
    for nome, conteudo in arquivos.items():
        (DIRETORIO_SAIDA / nome).write_text(conteudo, encoding="utf-8")
    print(f"{len(arquivos)} arquivos gravados em {DIRETORIO_SAIDA.relative_to(RAIZ)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
