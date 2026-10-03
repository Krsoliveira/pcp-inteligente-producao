# ADR-0012 — Suprimentos: entrada de nota fiscal, estoque por material e padrões de código e lote

**Status:** Aceito  
**Data:** 2026-10-03  
**Complementa:** ADR-0010 (entrada por compra) e ADR-0011 (rastreabilidade)

---

## Contexto

Depois da rastreabilidade (ADR-0011), três problemas ficaram claros no uso:

- **Recebimento e estoque misturados.** A entrada de material era um lote avulso dentro
  da lista de lotes. Uma nota fiscal com vários itens virava vários lançamentos sem nada
  que os ligasse, e não havia uma visão de "quanto tenho de cada material".
- **Código de material livre.** Qualquer texto de até 30 caracteres (`ACO-1020`,
  `MP-ACO`). Sem padrão, a busca e a integração com ERP (SAP usa códigos numéricos) ficam
  difíceis, e o tipo do material não aparece no código.
- **Número de lote derivado do material.** `MAT-{codigo}-{AAAAMM}-{seq}` tem até 28
  caracteres, repete o código do material e ignora o lote real do fornecedor, que é o que
  vem impresso na embalagem e o que um recall cita.

## Decisão

### 1. Nota fiscal de entrada com itens

Novo agregado `NotaFiscalEntrada`: fornecedor, número, emissão, recebimento e assinatura.
**Os itens da nota são os próprios lotes de compra**: cada lote guarda
`nota_fiscal_entrada_id`. Um caso de uso (`RegistrarEntradaNotaFiscal`) grava a nota e um
lote `DISPONIVEL` por item, numa transação, com um evento na trilha para a nota e um para
cada lote.

Regras:
- a mesma nota (fornecedor sem diferenciar maiúsculas + número) não entra duas vezes;
- pelo menos um item; só matéria-prima entra por nota;
- o mesmo lote do mesmo material não se repete na nota, nem pode já ter sido recebido do
  mesmo fornecedor. Fornecedores diferentes podem usar o mesmo número de lote.

API: `POST/GET /api/v1/notas-fiscais` (filtro `de`/`ate` pelo recebimento) e
`GET /api/v1/notas-fiscais/{id}`. O endpoint `POST /api/v1/lotes/entradas` foi removido.

### 2. Estoque calculado a partir dos lotes

`GET /api/v1/estoque` devolve, por material: saldo disponível (lotes `DISPONIVEL` dentro
da validade), quantidade de lotes, saldo indisponível (bloqueado ou vencido), próximo
vencimento e última entrada. É um **modelo de leitura**: uma consulta agregada
(`GROUP BY` sobre `lote`), sem tabela própria.

### 3. Código do material: 9 dígitos com faixa por tipo

| Faixa | Tipo |
|---|---|
| `103.xxx.xxx` | Produto acabado |
| `105.xxx.xxx` | Semiacabado |
| `110.xxx.xxx` | Matéria-prima |

O código é **gerado pelo sistema** (próximo livre da faixa); o cadastro não pede mais
código. Grava-se só os dígitos (`103000001`) e a tela formata (`103.000.001`). Um
`CHECK` no banco garante que o prefixo corresponde ao tipo. Geração concorrente é
serializada com `pg_advisory_xact_lock` por faixa.

### 4. Número do lote: até 20 caracteres

- **Compra:** o lote do fornecedor, digitado no item da nota (letras, dígitos e `. / -`,
  normalizado em maiúsculas).
- **Produção:** só números, `AAMMDD` da fabricação + sequência de 4 dígitos do dia
  (`2610030001`), único no sistema todo. Sem o código do material: o lote já aponta para
  o material, e um número curto é mais fácil de digitar, ler e buscar. A sequência do dia
  usa advisory lock, como o código do material.

Unicidade no banco: `(material, número, fornecedor)` para qualquer lote e `número` para
lotes de produção. O índice da V8 (`material, fornecedor, nota_fiscal`) foi removido,
porque agora uma nota pode trazer o mesmo material em lotes diferentes.

### 5. Telas e menu

- **Cadastros:** Materiais (sem campo de código), Listas Técnicas, Tipos de Ordem.
- **Suprimentos:** Entrada de notas (lista, filtro por recebimento, lançamento com
  itens dinâmicos, detalhe da nota) e Estoque (posição por material → lotes do material
  → detalhe do lote).
- **Produção:** Ordens.

A lista geral de lotes deixa de existir: `/lotes` redireciona para `/estoque`, e o
detalhe do lote (`/lotes/:id`) continua, agora com link para a nota fiscal.

### 6. Migração dos dados existentes (V11)

- Materiais recebem código na faixa do tipo, em ordem de cadastro (códigos que já
  seguem o padrão são mantidos).
- Uma nota é criada para cada (fornecedor, número) dos lotes de compra existentes, e os
  lotes são ligados a ela.
- Lotes de produção são renumerados para `AAMMDD` + sequência. Lotes de compra, cujo
  número do fornecedor nunca foi registrado, recebem `MIG-AAMMDD-NNN`.
- Cada troca de código ou número entra na trilha como `sistema:migracao-v11`, com
  *de → para*. Nada é apagado.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Tabela `item_nota_fiscal` separada do lote | Duplicaria quantidade, material e datas que o lote já tem; item e lote nasceriam e viveriam juntos (1:1) |
| Tabela de saldo de estoque atualizada a cada movimento | Pode divergir dos lotes; exige atualizar em todo consumo, conclusão e entrada. A agregação sobre `lote` é indexada e barata no volume atual. Se crescer, vira uma *view* materializada ou um cache |
| Código de material digitado pelo usuário, só validado | Sem garantia de sequência nem de faixa livre; o usuário precisaria descobrir o próximo número |
| Sequence do PostgreSQL por faixa | Pula números em rollback e precisa ser recriada para cada faixa nova; `MAX + 1` sob advisory lock é simples e sem buracos |
| Manter o código do material no lote de produção | Número longo (28 caracteres), redundante com o vínculo lote → material e ruim de digitar no chão de fábrica |
| Gerar o lote de compra pelo sistema | Perderia o lote do fornecedor, que é a referência de um recall |

## Consequências

- **Positivas:** recebimento modelado como acontece (uma nota, vários itens); estoque
  sempre coerente com os lotes; códigos e lotes curtos, padronizados e fáceis de buscar;
  migração auditável.
- **Negativas / riscos:** os códigos e números de lote existentes mudam (o de → para fica
  na trilha); limite de 999.999 materiais por faixa e de 9.999 lotes de produção por dia;
  a posição de estoque é calculada a cada consulta (monitorar quando o volume crescer).
