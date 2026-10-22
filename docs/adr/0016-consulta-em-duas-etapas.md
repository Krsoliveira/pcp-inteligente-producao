# ADR-0016: Consulta em duas etapas nas listagens

- **Status**: Aceito
- **Data**: 2026-10-21
- **Complementa**: ADR-0015 (pesquisa paginada e resumo do dashboard)

## Contexto

Mesmo com paginação no servidor (ADR-0015), abrir uma listagem já mostrando todos os
registros induz a olhar tudo e não ajuda quem quer um recorte. O usuário vem de um fluxo
de "tela de seleção" (estilo SAP): escolhe o que quer ver, e só então a lista aparece, e
pode refinar o que já está na tela.

## Decisão

Cada listagem de volume crescente (Ordens, Materiais, Listas Técnicas, Notas Fiscais e Estoque) tem duas telas:

1. **Consulta** (`/ordens`, `/materiais`, `/listas-tecnicas`): painel de filtros
   (Pesquisar/Limpar) e o cartão **"10 mais recentes"** com atalho "Ver todos". Quem só
   quer o último registro não precisa filtrar.
2. **Resultados** (`.../resultados`): os mesmos filtros, o resultado **paginado e filtrado
   no servidor** e um **subfiltro** que refina, no navegador, as linhas da página atual.

Regras:

- **Filtros e página ficam na URL** (`useParametrosDaUrl`): Voltar, F5 e link copiado
  reproduzem a consulta. Os campos editam um rascunho; a consulta muda ao clicar em
  Pesquisar (ou Enter). Valores inválidos na URL são ignorados.
- O subfiltro **não consulta o servidor**: ele só filtra as linhas já carregadas. Vira um
  texto livre sobre as colunas visíveis.
- Telas com pouquíssimos dados (Tipos de Ordem) continuam mostrando tudo direto.
- Novos endpoints `GET .../pesquisa` (materiais, listas técnicas, notas fiscais e estoque), no
  mesmo padrão das ordens: filtro opcional, `PedidoDePagina` que corrige valores fora do limite, `%`, `_` e
  `\` como texto comum. Os endpoints de listagem completa continuam iguais.
- **Notas fiscais:** a busca olha o número da nota, o fornecedor e o número do lote de qualquer
  item (subconsulta `EXISTS`), mais o período de recebimento. Período invertido: 422 no servidor e
  aviso na tela, sem consultar.
- **Estoque:** a posição de um material é um agregado dos lotes (não há tabela de estoque, ADR-0012).
  O servidor calcula as posições e aplica filtro, ordenação (entrada mais recente primeiro) e página
  sobre elas; o banco continua agregando todos os materiais. O aviso de materiais vencendo e
  matérias-primas sem saldo, que a tela calculava baixando tudo, vem de `GET /api/v1/estoque/resumo`.
  Se o catálogo de materiais crescer para milhares, a agregação precisará de filtro em SQL.
- A **lista técnica ganha tela de detalhe** (`/listas-tecnicas/:id`) com a "receita"
  (componentes, tipo e quantidade), os dados da versão e o botão de ativar.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Abrir a listagem direto com a primeira página (sem tela de consulta) | Funciona e custa pouco (14 KB), mas não orienta a consulta; o usuário preferiu escolher o recorte primeiro |
| Tela de seleção obrigatória, sem listar nada antes | Obriga um passo a quem só quer ver o mais recente; os "10 mais recentes" resolvem |
| Subfiltro no servidor | Cada tecla viraria uma consulta; no navegador é instantâneo e as linhas já estão lá |
| Detalhe da lista técnica expandido na própria tabela | Sem URL própria, e o versionamento e o rastreio de alterações precisam de uma página (decisão do usuário) |

## Consequências

- **Positivas**: consultas focadas, links compartilháveis, base para versionar listas
  técnicas e acompanhar alterações.
- **Negativas / riscos**:
  - Um clique a mais para ver "tudo" (mitigado pelos 10 mais recentes e pelo "Ver todos").
  - O subfiltro só enxerga a página atual; é preciso dizer isso na tela (já diz).
  - O comportamento de tela é coberto por testes de componente (ADR-0017), não por E2E.
  - Os lotes de um material (dentro de Estoque) ainda baixam a lista do material e paginam só na
    tela (ver `proximos-passos.md`).
