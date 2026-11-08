# ADR-0036: Número legível da lista técnica (LT-0001)

- **Status**: Aceito
- **Data**: 2026-11-08

## Contexto

Cada lista técnica tem um `listaId` (UUID da lista do material) e cada versão tem o próprio `id` (UUID). Nenhum dos dois
é legível: na Auditoria a referência era "código do material + versão", que identifica a versão mas não a lista, e não
dava para citar "a lista LT-0007" numa conversa, num log ou num documento.

## Decisão

Cada lista recebe um **número sequencial** (`numero_lista`, `BIGINT`), tirado da sequência `lista_tecnica_numero_seq`
na criação da primeira versão. As demais versões herdam o número da lista. O banco guarda o inteiro; o formato
`LT-0007` (mínimo de 4 dígitos, sem limite superior) é montado em um só lugar, `NumeroDaLista`.

- Migração **V22**: cria a sequência e a coluna, numera as listas existentes (uma por `lista_id`, na ordem de criação
  da primeira versão), ajusta a sequência e torna a coluna `NOT NULL`.
- API: `numeroLista` (texto) é acrescentado a `ListaTecnicaResponse` e ao resumo da comparação. O contrato só cresce.
- Auditoria: eventos novos usam a referência `LT-0007 · 103000001 v1` e o detalhe `numeroLista`.
- Telas: coluna "Lista" na listagem; número no subtítulo e no cartão do detalhe.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Mostrar o `listaId` encurtado | Continua ilegível e não é estável para citar; equivale ao que já existe |
| Número por versão | O que o usuário quer citar é a lista; versões já têm o rótulo (v1, v2) |
| Calcular o número na aplicação (`MAX + 1`) | Corrida entre criações simultâneas; a sequência do banco é atômica |
| Tabela `lista` separada, com o número único | Refatoração grande do modelo sem necessidade hoje |

## Consequências

- **Positivas**: lista citável em conversa, log e Auditoria; criação simultânea nunca repete número (teste com 10
  requisições paralelas no banco real).
- **Negativas / riscos**:
  - A numeração pode ter buracos: se a criação falhar depois de pegar o número, ele se perde. Se um dia houver
    exigência de numeração contínua, esta solução não serve.
  - O banco não impede duas versões da mesma lista com números diferentes (a coluna se repete de propósito); a
    garantia está na aplicação (versão herda o número) e nos testes.
  - Eventos de auditoria antigos são imutáveis e mantêm a referência antiga; o `listaId` continua nos detalhes.
  - Com `NOT NULL`, a versão anterior da aplicação não consegue criar lista nova. Rollback: subir a imagem anterior
    serve para leitura; para criar listas durante o rollback, aplicar migração nova com
    `ALTER TABLE lista_tecnica ALTER COLUMN numero_lista DROP NOT NULL`. Fazer backup antes de aplicar em produção.
