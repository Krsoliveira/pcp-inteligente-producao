# Visão Geral

## O problema

Times de **PCP (Planejamento e Controle da Produção)** — a área da indústria que decide
*o que* produzir, *quanto*, *quando* e *com quais recursos* — trabalham, na prática, com
planilhas desconectadas, dados defasados e pouca visibilidade sobre atrasos. As
consequências são conhecidas: estoque parado (capital imobilizado), atraso de entrega
(cliente insatisfeito) e decisões tomadas no "feeling" em vez de dados.

## A solução

Uma plataforma web que centraliza o planejamento da produção e usa **IA (Inteligência
Artificial)** como copiloto do planejador:

1. **Previsão de demanda** — estimar quanto será vendido nos próximos períodos, para
   planejar a produção antes do pedido chegar.
2. **Análise de atrasos** — identificar ordens de produção em risco e explicar as
   causas prováveis (gargalo de máquina, falta de material, etc.).
3. **Recomendações acionáveis** — sugestões concretas ("antecipe a ordem X",
   "repriorize o centro de trabalho Y") em linguagem natural, geradas por IA.

## Usuários (personas)

| Persona | Necessidade principal |
|---|---|
| **Planejador de PCP** | Visão diária das ordens, alertas de atraso, replanejamento rápido |
| **Gestor de produção** | Indicadores consolidados (OTIF, aderência ao plano, utilização) |
| **Analista de dados** | Exportar dados e validar as previsões da IA |

## Escopo

### Dentro do escopo

- Cadastro e acompanhamento de ordens de produção
- Dashboard com indicadores de produção
- Previsão de demanda e análise de atrasos com IA
- Autenticação com perfis de acesso
- Integrações **simuladas** com SAP e Power BI (demonstram o padrão de integração
  sem exigir licenças reais)

### Fora do escopo

- Controle de chão de fábrica em tempo real (MES — Manufacturing Execution System)
- Integração real com ERP licenciado
- Aplicativo móvel

## Roteiro de fases

| Fase | Entrega | Status |
|---|---|---|
| 0 | Fundação: documentação, estrutura, infraestrutura local (Docker) | Concluída |
| 1 | Backend núcleo: API de ordens de produção ponta a ponta | Concluída |
| 2 | Autenticação e perfis (JWT) | Concluída |
| 3 | Frontend: dashboard inicial | Concluída |
| 4 | Carga de dados reais (Kaggle) | Pendente |
| 5 | Módulo de IA | Pendente |
| 6 | Integrações simuladas e deploy | Pendente |

Cada fase termina com: testes passando no CI, documentação atualizada e, quando houver
decisão relevante, um ADR novo em [`adr/`](adr/).
