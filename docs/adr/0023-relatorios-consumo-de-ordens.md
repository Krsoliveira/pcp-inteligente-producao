# ADR-0023: Relatórios — primeiro, o consumo de ordens por componente

- **Status**: Aceito
- **Data**: 2026-10-23
- **Complementa**: ADR-0020 (acesso por perfis), ADR-0016 (consulta em duas etapas) e ADR-0011 (genealogia de lotes)

## Contexto

Hoje o consumo de materiais só se vê ordem a ordem, na tela de detalhe. Falta uma visão de **várias ordens
de uma vez** para olhar o consumo total por componente num período (quanto se planejou, quanto se gastou e
onde houve desvio), sem entrar em cada ordem.

## Decisão

Nova área **Relatórios** no menu, com o primeiro relatório: **Consumo de ordens**.

- **Filtro prévio obrigatório:** período do **fim planejado** da ordem (início e fim, inclusivos). Sem período a
  tela não consulta; mostra o pedido para escolher. O fim planejado é o mesmo critério do gráfico de entregas
  do dashboard (ADR-0022).
- **Quais ordens:** somente as **concluídas** (todo o consumo está lançado e justificado). Abertas e canceladas
  ficam de fora.
- **Quais linhas:** uma por **ordem e componente**; a ordem se repete em cada componente. Cada linha traz o
  consumo **total** (planejado, consumido e desvio = consumido − planejado). Se o componente saiu de mais de um
  lote, o consumo já é a soma dos lotes; o detalhe por lote continua na tela da ordem (o código da ordem é um
  link para ela).
- **API (nova, somente leitura):** `GET /api/v1/relatorios/consumo-de-ordens?de=&ate=` (datas obrigatórias).
  Período invertido ou com mais de **366 dias** devolve 422; mais de **5000 linhas** devolve 422 pedindo para
  reduzir o período (protege o servidor e o navegador).
- **Permissão nova:** `RELATORIO_CONSUMO_VISUALIZAR` (área "Relatórios"). O Administrador a tem sempre; os demais
  perfis só se um gestor a conceder. Ver o consumo dentro da ordem (`CONSUMO_VISUALIZAR`) **não** dá acesso ao
  relatório.
- **Tela:** o mesmo padrão das listagens (painel de consulta, filtro e ordenação por coluna no cabeçalho). As
  quantidades aparecem com a unidade ("26 kg"); desvio acima do planejado em vermelho e abaixo em laranja.
- **Consulta:** uma única busca dos consumos das ordens do período (sem uma consulta por ordem). Os materiais
  vêm de uma leitura do cadastro inteiro, que é pequeno; se crescer, vira busca por ids.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Período pela data em que o consumo foi lançado | Ordens sem lançamento ficariam invisíveis; o fim planejado é o critério já usado no dashboard |
| Incluir ordens abertas, com pendentes sinalizados | O responsável pediu só as concluídas: o relatório é de consumo realizado |
| Uma linha por lote | O objetivo é o consumo total por componente; o lote está na ordem |
| Paginar no servidor | Relatório se lê inteiro; o limite de 5000 linhas e de 366 dias basta por enquanto |
| Reutilizar `CONSUMO_VISUALIZAR` | Dar acesso ao relatório (visão de várias ordens) é uma decisão de gestão, separada de ver o consumo de uma ordem |

## Consequências

- **Positivas**: o consumo de várias ordens numa tela, no mesmo padrão do restante do sistema; base para os
  próximos relatórios na mesma área e com a mesma permissão por relatório.
- **Negativas / riscos**:
  - Não há exportação (CSV/Excel) ainda; é o próximo passo natural.
  - Perfis já existentes não ganham a permissão sozinhos (só o Administrador); é preciso concedê-la.
  - O relatório de um ano de ordens pode passar de 5000 linhas; o usuário precisa reduzir o período.
- **Rollback**: reverter o PR. Não há migração; nenhum campo existente foi alterado ou removido.
- **Pendências**: exportação; tela de lançamento de consumo por ordem (todos os componentes de uma vez); quantidade
  base da lista técnica (decisão de alto risco, com ADR próprio).
