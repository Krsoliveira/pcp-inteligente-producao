# ADR-0009 — Dataset sintético para o modelo expandido (Material, BOM, Consumo, Lote)

**Status:** Aceito  
**Data:** 2026-09-26  
**Fase:** 5b (encerramento) — pré-requisito da Fase 5c (IA)  
**Substitui parcialmente:** ADR-0006 (seção "Fonte dos dados"; a estratégia `DataLoader` + perfil `seed` continua valendo)

---

## Contexto

O ADR-0007 trocou `ordem_producao.produto` (texto livre) por `material_id` +
`lista_tecnica_id`, e a Fase 5b adicionou `ConsumoMaterial`, `Lote` e `TipoOrdem`.
O dataset do ADR-0006 (105 ordens com `produto` VARCHAR) ficou incompatível e o
`DataLoader` foi desativado. Consequências práticas:

- O dashboard subia **vazio** em qualquer ambiente novo.
- A Fase 5c (IA: previsão de demanda, análise de atrasos, recomendações) não tinha
  histórico para trabalhar.

O dataset precisa ser coerente em **todas** as entidades ao mesmo tempo: uma ordem
concluída exige consumos registrados (e justificados quando há desvio) e exatamente um
lote; a lista técnica da ordem precisa ser do mesmo material; e assim por diante.

## Decisão

1. **Gerador Python determinístico** em `data/scripts/gerar_dataset_sintetico.py`
   (somente biblioteca padrão, semente fixa). Pipeline `load → transform → validate →
   export`; a etapa `validate_dataset` aplica as invariantes do domínio antes de gravar.
2. **CSVs versionados** em `backend/src/main/resources/dados/` — o Python **não** roda em
   runtime. `--verificar` falha se os CSVs divergirem do gerador (usado no CI).
3. **Chaves naturais** nos CSVs (código do material, `material+versão` da BOM, código da
   ordem). Os UUIDs são gerados na carga — o CSV fica legível e independente do banco.
4. **Novo `DataLoader`** (perfil `seed`, transacional e idempotente):
   - dados mestres (material, tipo, BOM) passam por `criar()`/`ativar()`/`obsoleter()`
     — mesmas regras da API;
   - histórico (ordens, consumos, lotes) usa `reconstituir()` (fatos passados, ADR-0006);
   - o número do lote é gerado pela regra do domínio (`Lote.prefixoNumeroLote` /
     `Lote.numeroLote`), a mesma usada por `ConcluirOrdemProducao`.
5. **Ancoragem de datas**: `dataset.properties` guarda a `data_referencia` ("hoje" do
   dataset). Na carga, todas as datas são deslocadas para que ela coincida com a data
   real — o cenário (carteira aberta, atrasos, lotes a vencer) é sempre atual.

### Padrões embutidos (matéria-prima para a Fase 5c)

| Padrão | Para qual análise |
|---|---|
| Demanda de PA com sazonalidade (pico no 2º semestre) + tendência de ~1,5%/mês | Previsão de demanda |
| Tratamento Térmico e Soldagem com atraso médio maior; ordens "travadas" em gargalo | Análise de atrasos |
| Desvios de consumo com causas por centro (porosidade, setup, retrabalho de solda) | Recomendações / perdas |
| Troca de versão de BOM (v1 OBSOLETA → v2 ATIVA) no meio do histórico | Rastreabilidade / auditoria |
| Explosão de BOM multinível gera ordens de SA antes das de PA | Planejamento (MRP) |

Volume (semente 42): 21 materiais, 13 listas técnicas, 743 ordens (~18 meses de histórico
+ ~2 meses de carteira), ~1.360 consumos, ~640 lotes. Carga em ~1,5 s no PostgreSQL local.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Dataset público do Kaggle | Mesmo problema do ADR-0006: nenhum dataset público cobre BOM + consumo + lote com coerência referencial |
| Gerar os dados em Java no startup | Mistura geração (evolui com a análise de dados) com carga (infraestrutura); o CSV versionado permite revisar o dataset em PR |
| pandas / Polars no gerador | Geração linha a linha, sem agregação pesada — não há ganho que justifique a dependência |
| Flyway seed SQL | Mantida a razão do ADR-0006: schema ≠ dados de negócio |
| Datas absolutas sem ancoragem | Em poucas semanas toda a carteira viraria "atrasada" e os lotes "vencidos" — demonstração irreal |

## Consequências

- **Positivas**: ambiente novo sobe com dados coerentes em um comando
  (`--spring.profiles.active=dev,seed`); a Fase 5c tem histórico com sinal real;
  `DataLoaderTest` carrega o dataset verdadeiro e verifica as invariantes.
- **Negativas / riscos**: mudar o dataset exige rodar o gerador e versionar os CSVs
  (o CI acusa esquecimento via `--verificar`). Entidades JPA com UUID atribuído fazem
  `merge` (SELECT + INSERT por registro) — aceitável para uma carga única de ~2,7 mil
  linhas; revisitar se o volume crescer uma ordem de grandeza.
- O arquivo `ordens_producao.csv` antigo foi substituído pelo novo formato.
