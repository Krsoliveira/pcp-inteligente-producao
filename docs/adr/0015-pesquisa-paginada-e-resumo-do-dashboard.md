# ADR-0015: Pesquisa paginada de ordens e resumo do dashboard

- **Status**: Aceito
- **Data**: 2026-10-21
- **Complementa**: ADR-0001 (Clean Architecture) e ADR-0011 (rastreabilidade)

## Contexto

Em teste manual com os dados do seed, a tela de Ordens e o dashboard ficaram lentos. A
medição mostrou que o backend responde rápido (78 ms), mas `GET /api/v1/ordens-producao`
devolve **todas** as ordens (845 registros, 473 KB), `GET /api/v1/lotes` devolve 921
(548 KB), e o frontend desenhava todas as linhas, filtrava no navegador e, no dashboard,
baixava as duas listas só para contar status e lotes disponíveis. O volume cresce com o
tempo de uso do sistema, então o custo também cresceria.

## Decisão

- Novo endpoint **`GET /api/v1/ordens-producao/pesquisa`** (`busca`, `status`, `pagina`,
  `tamanho`): filtra e pagina no banco, das ordens mais recentes para as mais antigas
  (empate por código decrescente). Devolve o `PaginaResponse` já usado pela auditoria.
- Novo endpoint **`GET /api/v1/dashboard/resumo`**: total de ordens, ordens por status
  (todos os status, com zero), ordens atrasadas e lotes disponíveis, calculados com
  `COUNT`/`GROUP BY`.
- `PedidoDePagina` (domínio) **corrige** em vez de recusar: página negativa vira 0 e
  tamanho fora de 1..100 vira o padrão (25) ou o teto (100). Ninguém consegue pedir
  "todos os registros".
- O texto da busca é parâmetro de consulta (Criteria API) e `%`, `_` e `\` digitados são
  tratados como texto comum, não como curingas do `LIKE`.
- **Contrato só cresce:** `GET /api/v1/ordens-producao` continua existindo, sem mudança.
- As listas de notas fiscais e de lotes por material **não** ganharam endpoint novo: a
  busca de notas olha o lote dentro dos itens e as duas listas têm centenas de linhas, não
  milhares. Elas ganharam paginação na tela (25 por vez). Reavaliar se passarem de ~1000.
- Sem migração: os índices de `status`, `centro_de_trabalho`, `material_id` e
  `criada_em` já existem, e `codigo` é único. A busca por trecho (`LIKE '%x%'`) não usa
  índice, o que é aceitável neste volume.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Só paginar na tela | Resolve o desenho de centenas de linhas, mas continua baixando e interpretando todos os registros e não escala |
| Mudar `GET /ordens-producao` para devolver página | Quebra o contrato público; exigiria `/v2` |
| Cache no Redis | Não há consumidor de Redis no código (regra do projeto) e não reduz o payload |
| Índice trigram (`pg_trgm`) para a busca | Migração e extensão para um ganho que só aparece com muito mais linhas |
| Paginação por cursor | Mais complexa; a tela precisa de "ir para a página N" e total de páginas |

## Consequências

- **Positivas**: a tela de Ordens baixa 25 ordens por vez em vez de 845; o dashboard
  baixa 5 ordens e um resumo de poucos bytes; o filtro passa a ser do servidor.
- **Negativas / riscos**:
  - Dois contadores por requisição de resumo (`GROUP BY` e atrasadas), baratos com os
    índices atuais, mas sem cache; o dashboard atualiza a cada 30 s.
  - A ordenação por `criada_em` não é estável se duas ordens forem criadas no mesmo
    instante; o desempate por código mantém a ordem determinística.
  - O tipo `Pagina` mora em `domain.auditoria` por razões históricas; moveríamos para
    `domain.comum` num refactor à parte.
