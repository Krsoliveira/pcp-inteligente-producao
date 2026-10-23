# ADR-0022: Dashboard acionável — indicadores que abrem a consulta e gráficos que dizem algo

- **Status**: Aceito
- **Data**: 2026-10-23
- **Complementa**: ADR-0015 (resumo do dashboard), ADR-0016 (consulta em duas etapas) e ADR-0020 (acesso por perfis)

## Contexto

O dashboard mostrava cinco totais com ícones coloridos, um donut de ordens por status e as cinco ordens mais
recentes. Três problemas:

- O donut é dominado pelas ordens concluídas (746 de 845 na carga): é histórico, não ajuda a decidir nada hoje.
- Nada é clicável: "34 atrasadas" não leva às 34 ordens.
- "Ordens recentes" é uma amostra sem critério de atenção.

## Decisão

O dashboard passa a responder "o que precisa de atenção agora?" e a levar quem o lê até a consulta. Entrega em
três fases, cada uma em seu PR:

1. **Indicadores clicáveis e fila atual** (esta fase). Cada indicador é um link para a listagem **já
   filtrada**; sem ícone decorativo, com número grande e cor só para sinalizar (atrasos). O donut dá lugar à
   **fila de produção**: barras de Planejada → Liberada → Em produção, só ordens abertas, clicáveis por status.
2. **Séries no tempo**: produção por mês (planejado × produzido), atrasos por centro de trabalho e filtro de
   período, com endpoints agregados novos.
3. **Painel de atenção**: ordens mais atrasadas, lotes perto do vencimento e consumos com desvio, cada item com
   link.

### Fase 1 em detalhe

- **API (só cresce):** `GET /api/v1/ordens-producao/pesquisa` ganha o parâmetro opcional `atrasadas=true`. A regra
  é a do domínio (`OrdemProducao.estaAtrasada`: aberta e depois do fim planejado), a mesma do contador do
  dashboard; um teste de API garante que o total da pesquisa é igual a `ordensAtrasadas` do resumo. `false` ou
  ausente não filtra; outro valor devolve 400. O dia de referência é o do servidor, nunca do cliente.
- **Tela:** a URL de resultados aceita `?atrasadas=true`; como o formulário não tem campo para isso, aparece um
  chip "Somente ordens atrasadas" que se remove, e "Limpar" também o remove.
- **Permissões (ADR-0020):** o dashboard continua só com `DASHBOARD_VISUALIZAR`. Cada indicador só vira link se o
  perfil tem a permissão da tela de destino (`ORDEM_VISUALIZAR`, `ESTOQUE_VISUALIZAR`); sem ela, mostra o número
  sem link, e o clique no gráfico fica desligado. O servidor continua sendo a barreira: a pesquisa exige
  `ORDEM_VISUALIZAR`.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Só trocar o visual (ícones, cores) | Não resolve o problema de fundo: números que não levam a lugar nenhum |
| Calcular "atrasadas" no frontend a partir das ordens baixadas | A listagem é paginada no servidor (ADR-0015); a contagem e o filtro divergiriam |
| Link do indicador sempre visível | Levaria quem não tem permissão a uma tela de erro 403 |
| Biblioteca nova de gráficos | O ECharts já está na stack e cobre barras, séries e cliques |
| Tudo de uma vez | PR grande e difícil de revisar; a fase 1 já entrega valor sozinha |

## Consequências

- **Positivas**: da visão geral à lista de ordens em um clique; o gráfico deixa de ser dominado pelo histórico;
  a regra de "atrasada" fica em um só lugar.
- **Negativas / riscos**:
  - A pesquisa de ordens ganhou mais um filtro, que precisa ficar coerente com o contador do resumo (coberto por
    teste).
  - Os cartões de indicador do Estoque ainda usam ícones coloridos; ficam para uma passada de consistência.
- **Rollback**: reverter o PR. O parâmetro novo é opcional e nenhum campo foi alterado nem removido.
- **Pendências** (fases 2 e 3): séries mensais, atrasos por centro de trabalho, painel de atenção e a
  "aderência ao prazo", que depende de a ordem guardar a data real de conclusão (a verificar no modelo antes de
  propor a regra).
