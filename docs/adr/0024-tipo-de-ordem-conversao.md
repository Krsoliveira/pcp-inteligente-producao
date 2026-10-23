# ADR-0024: Produto acabado só é consumido em ordem do tipo Conversão

- **Status**: Aceito
- **Data**: 2026-10-23
- **Complementa**: ADR-0007 (material, lista técnica, consumo e lote) e ADR-0020 (acesso por perfis)

## Contexto

A ordem de produção existe para fabricar um material; o único vínculo dela com o material é dizer o que foi
produzido. A lista técnica pertence ao material produzido e diz o que se consome. O consumo deve vir de
**matéria-prima** e **semiacabado**.

Hoje nada impede um **produto acabado** de ser componente de uma lista (só se validava o material dono da
lista). Consumir produto acabado numa ordem comum desvirtua o controle: o acabado já é o produto final do
processo. Quando há necessidade real de usá-lo, o caminho é **converter** o acabado em semiacabado numa ordem
própria.

## Decisão

Regra inegociável: **produto acabado nunca é consumido numa ordem comum.** Para consumi-lo, abre-se uma ordem do
tipo **Conversão**, que converte produto acabado em semiacabado.

- **Tipo de ordem de sistema "Conversão"** (`tipo_ordem.conversao`, migração V15). É criado pela migração, é único
  (índice parcial) e **não pode ser alterado** (422). Tipos criados pelos usuários nunca são de conversão. Se já
  existisse um tipo chamado "Conversão", ele passa a ser o tipo de sistema.
- **Lista técnica:** a lista de **produto acabado** só aceita matéria-prima e semiacabado como componentes. A lista
  de **semiacabado** pode ter produto acabado (é a conversão).
- **Criação da ordem:**
  - uma lista que consome produto acabado só gera ordem do tipo **Conversão** (sem tipo ou com outro tipo, 422);
  - o tipo **Conversão** só vale para ordens que produzem **semiacabado** (422).
  Isso fecha a brecha de uma lista de semiacabado com produto acabado ser usada numa ordem comum.
- **Onde a regra vive:** `PoliticaDeComponentes` (domínio puro), chamada ao cadastrar a lista e ao criar a ordem.
- **Interface:** o tipo Conversão aparece como "Sistema" e sem o botão de editar; na ordem de Conversão só se
  escolhem semiacabados; na lista, produto acabado só é oferecido como componente quando o dono é semiacabado.
- **API (só cresce):** `TipoOrdemResponse` ganha `conversao`. Nenhum campo foi alterado ou removido.
- **Dados existentes:** conferido no banco de desenvolvimento: 0 componentes de produto acabado e 0 consumos de
  produto acabado; nenhuma correção de dados.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Regra só pelo tipo do material produzido (acabado só em ordem de semiacabado) | Qualquer lista de semiacabado com acabado viraria ordem comum: era a brecha |
| Tipo de ordem "Conversão" criado pelo usuário, com uma caixa de seleção | A regra não pode depender de alguém marcar corretamente; é um tipo de sistema |
| Proibir o consumo de produto acabado em qualquer ordem | A conversão deixaria de ser possível |
| Validar só no frontend | A regra é inegociável: o servidor é a barreira |

## Consequências

- **Positivas**: produto acabado deixa de ser consumido por engano; a conversão fica explícita, rastreável e
  separada do restante da produção.
- **Negativas / riscos**:
  - Os testes que usavam materiais inexistentes passaram a cadastrá-los (a regra precisa ler os tipos reais).
  - A V15 cria uma linha de dados em `tipo_ordem`; reverter exige removê-la (e a coluna) em migração nova.
- **Rollback**: reverter o código é seguro (a coluna tem padrão FALSE). Desfazer o schema é uma migração nova que
  remove o tipo de sistema e a coluna, o que só é possível se nenhuma ordem usar o tipo Conversão.
- **Pendências**: tela de lançamento de consumo por ordem com "adicionar item" (V16, `CONSUMO_ADICIONAR_ITEM`), que
  reaplicará a regra às linhas adicionadas; quantidade base da lista técnica.
