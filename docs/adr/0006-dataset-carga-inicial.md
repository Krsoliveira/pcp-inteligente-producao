# ADR-0006 — Dataset e estratégia de carga inicial de ordens de produção

**Status:** Aceito  
**Data:** 2026-08-05  
**Fase:** 4 — Carga de dados reais

---

## Contexto

A Fase 4 exige dados de produção realistas para duas finalidades:

1. **Dashboard funcional** — o recrutador precisa ver indicadores reais (OTIF, ordens em atraso, distribuição por centro de trabalho) em vez de tabelas vazias.
2. **Treino/demonstração do módulo de IA (Fase 5)** — os modelos de análise de atrasos e recomendações precisam de histórico com variação de produto, centro de trabalho e status.

O dataset precisa ter cobertura temporal (ao menos 12 meses), distribuição realista de status, e granularidade suficiente para análise por centro de trabalho.

---

## Decisão

### Fonte dos dados

Dataset **sintético** gerado especificamente para o projeto, modelado sobre os padrões operacionais de um fabricante de peças mecânicas industriais de médio porte (fabricação discreta, múltiplos centros de trabalho, ciclos de ordens de 10–30 dias).

O dataset público do Kaggle avaliado ([ziya07/manufacturing-production-data](https://www.kaggle.com/datasets/ziya07/manufacturing-production-data)) foi descartado: seus 1.000 registros cobrem apenas 7 dias (18–25/mar/2023), não contêm campo de produto nem quantidade, e representam agendamento de operações de máquina — nível chão de fábrica (MES), não ordens de produção no nível PCP. O mapeamento forçado produziria dados sem coerência semântica.

O arquivo `backend/src/main/resources/dados/ordens_producao.csv` contém **105 ordens** cobrindo janeiro/2024 a outubro/2025 com:

| Dimensão | Valores |
|---|---|
| Centros de trabalho | 8 (Usinagem CNC, Montagem, Soldagem MIG/TIG, Pintura Industrial, Inspeção de Qualidade, Fundição Sob Pressão, Estamparia, Tratamento Térmico) |
| Produtos | 20 (peças mecânicas: eixos, válvulas, engrenagens, etc.) |
| Status | CONCLUIDA (87), EM_PRODUCAO (3), LIBERADA (2), PLANEJADA (7), CANCELADA (3) |

### Estratégia de carga

`DataLoader` (Spring `CommandLineRunner`, `@Profile("seed")`) lê o CSV via `ClassPathResource` e insere as ordens usando `OrdemProducao.reconstituir()` + porta `OrdemProducaoRepository.salvar()`.

**Por que `reconstituir()` e não o caso de uso `CriarOrdemProducao`?**

O caso de uso cria ordens que nascem sempre `PLANEJADA`. Dados históricos importados têm status arbitrário (CONCLUIDA, EM_PRODUCAO, etc.) — forçar transições de estado uma a uma seria lento e semanticamente errado. `reconstituir()` existe exatamente para esse caso: recompor um objeto de domínio válido a partir de dados persistidos/importados, sem reexecutar as invariantes de criação.

**Por que não Flyway seed (V4__seed.sql)?**

O Flyway é dono do schema — mesclar schema com dados de negócio na mesma ferramenta cria dois problemas: (a) o seed roda em produção se não houver guarda; (b) dados de teste inflam o histórico de migrações de schema. O `DataLoader` com `@Profile("seed")` é explícito: o operador decide quando carregar os dados.

**Idempotência:** o loader verifica `repositorio.contarTodas() > 0` antes de qualquer insert. Segunda execução é no-op.

---

## Alternativas consideradas

| Alternativa | Por que descartada |
|---|---|
| Dataset Kaggle ziya07/manufacturing-production-data | Nível MES (operações de máquina), não PCP (ordens de produção); sem campo produto nem quantidade; cobre apenas 7 dias — incompatível com o modelo de domínio |
| Flyway V4 seed SQL | Acoplamento schema/dados; roda em produção sem controle explícito |
| Testcontainers seed no teste de integração | Escopo diferente: carga funcional não é teste, é operação de ambiente |
| `CriarOrdemProducao` use case no loader | Semanticamente incorreto para dados históricos; forçaria status PLANEJADA para todas as ordens |

---

## Consequências

- **Campo `centro_de_trabalho` adicionado ao domínio** (Flyway V3): fundamental para análise por gargalo na Fase 5; tornar o campo obrigatório na entidade agora evita refatoração posterior.
- **`OrdemProducaoRepository.contarTodas()`** adicionada à porta: método mínimo, sem vazamento de infraestrutura.
- O dataset cobre padrões sazonais e mix de produtos — suficiente para demonstrar previsão de demanda e análise de atrasos na Fase 5.
- Em produção, substituir o CSV por uma integração real (SAP/ERP) sem alterar o domínio: basta uma nova implementação da porta.
