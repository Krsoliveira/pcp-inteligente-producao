#!/usr/bin/env python3
"""
Gerador do dataset sintético do PCP (ADR-0009).

Produz, de forma DETERMINÍSTICA (semente fixa), um conjunto de CSVs coerente com o
modelo de domínio expandido do ADR-0007:

    Material → Lista Técnica (BOM versionada) → Ordem de Produção
             → Consumo de Material (planejado × real, com desvios justificados)
             → Lote (gerado na conclusão da ordem)

Os CSVs são gravados em ``backend/src/main/resources/dados/`` e lidos pelo
``DataLoader`` (perfil Spring ``seed``). O script não é executado em runtime: ele é
rodado pelo desenvolvedor quando o dataset precisa mudar, e o resultado é versionado.

Padrões embutidos de propósito (matéria-prima para a IA da Fase 5c):
  * Sazonalidade + tendência na demanda de produtos acabados (previsão de demanda).
  * Gargalos: Tratamento Térmico e Soldagem atrasam mais (análise de atrasos).
  * Desvios de consumo com causas por centro de trabalho (refugo, setup, porosidade).
  * Troca de versão de BOM no meio do histórico (v1 OBSOLETA → v2 ATIVA).

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


def transform_lotes(rng: random.Random, ordens: list[Ordem]) -> list[dict]:
    lotes = []
    for o in ordens:
        if o.status != "CONCLUIDA":
            continue
        fabricacao = o.data_conclusao
        validade = _somar_meses(fabricacao, o.material.validade_meses)
        if validade < DATA_REFERENCIA:
            status = "VENCIDO"
        elif rng.random() < 0.03:
            status = "BLOQUEADO"
        elif o.material.tipo == "SEMIACABADO" and (DATA_REFERENCIA - fabricacao).days > 45:
            status = "CONSUMIDO"
        else:
            status = "DISPONIVEL"
        lotes.append({"ordem": o, "fabricacao": fabricacao, "validade": validade, "status": status})
    return lotes


def _somar_meses(d: date, meses: int) -> date:
    total = d.month - 1 + meses
    ano, mes = d.year + total // 12, total % 12 + 1
    return date(ano, mes, min(d.day, 28))


def validate_dataset(ordens: list[Ordem], lotes: list[dict], materiais: dict[str, MaterialDef]) -> None:
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
    for lote in lotes:
        if lote["validade"] < lote["fabricacao"]:
            erros.append(f"{lote['ordem'].codigo}: validade antes da fabricação")
        if lote["fabricacao"] > DATA_REFERENCIA:
            erros.append(f"{lote['ordem'].codigo}: lote fabricado no futuro")
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


def export_csvs(ordens: list[Ordem], lotes: list[dict]) -> dict[str, str]:
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
             "quantidade", "quantidade_produzida", "inicio_planejado", "fim_planejado", "status"],
            [[o.codigo, o.material.codigo, o.versao_lista, o.tipo_ordem, o.centro, o.quantidade,
              o.quantidade_produzida, o.inicio.isoformat(), o.fim.isoformat(), o.status]
             for o in ordens]),
        "consumos_material.csv": _csv(
            ["ordem_codigo", "componente_codigo", "quantidade_planejada", "quantidade_consumida",
             "justificativa", "justificado_por"],
            [[o.codigo, c["componente"].codigo, c["planejada"], c["consumida"],
              c["justificativa"], c["justificado_por"]]
             for o in ordens for c in o.consumos]),
        "lotes.csv": _csv(
            ["ordem_codigo", "data_fabricacao", "data_validade", "status"],
            [[lote["ordem"].codigo, lote["fabricacao"].isoformat(), lote["validade"].isoformat(),
              lote["status"]] for lote in lotes]),
    }
    return arquivos


def gerar() -> dict[str, str]:
    rng = random.Random(SEMENTE)
    materiais, boms = load_dados_mestres()
    ordens = transform_ordens(rng, materiais, boms)
    transform_status(rng, ordens)
    transform_consumos(rng, ordens, materiais, boms)
    lotes = transform_lotes(rng, ordens)
    validate_dataset(ordens, lotes, materiais)
    return export_csvs(ordens, lotes)


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
