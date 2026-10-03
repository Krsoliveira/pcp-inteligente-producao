# ADR-0011 — Rastreabilidade e auditoria: assinatura, trilha de eventos e genealogia de lotes

**Status:** Aceito  
**Data:** 2026-10-03  
**Complementa:** ADR-0007 (rastreabilidade por lote) e ADR-0010 (entrada por compra)

---

## Contexto

O sistema registra *o quê* foi feito, mas não *quem* fez, *quando* e *o que mudou*:

- nenhuma ação guarda o usuário responsável;
- não há histórico — ao mudar o status de uma ordem ou ativar uma lista técnica, o
  estado anterior se perde;
- a entrada de material não guarda as datas do documento (emissão da NF) nem do
  recebimento físico, e não há como filtrar entradas por período;
- o consumo de uma ordem sabe *qual material* foi usado, mas não *de qual lote* — não
  é possível rastrear um produto acabado até a NF da matéria-prima (recall);
- os casos de uso não são transacionais: uma operação com várias gravações (ex.:
  concluir ordem e gerar lote) pode ficar pela metade se uma delas falhar.

Para uma plataforma de PCP com foco em auditoria, rastreabilidade é requisito central.

## Decisão

### 1. Assinatura em todo registro

Cada agregado (`Material`, `ListaTecnica`, `OrdemProducao`, `Lote`, `ConsumoMaterial`,
`TipoOrdem`) carrega um value object `Assinatura` com **criado por / criado em** e
**alterado por / alterado em**. As fábricas e os métodos que mudam estado **exigem o
usuário** — é impossível criar ou alterar um registro sem autor.

O usuário vem do login (porta `UsuarioAtual`, implementada com o contexto do Spring
Security); processos sem usuário logado (carga inicial, administrador inicial) usam
identificadores de sistema explícitos.

### 2. Trilha de auditoria imutável

Cada ação de negócio gera um `EventoAuditoria` — entidade, registro, ação, usuário,
data/hora e **detalhes** (incluindo valores *antes → depois*). A tabela
`evento_auditoria` é **append-only**: um trigger no PostgreSQL bloqueia `UPDATE` e
`DELETE`. Os eventos são gravados pelos casos de uso, na mesma transação da ação.

Eventos são de **negócio** ("ordem liberada", "lista técnica v2 ativada", "entrada
registrada"), não um espelho técnico de linhas — é o que um auditor lê.

### 3. Casos de uso transacionais

Porta `Transacao` na camada de aplicação, implementada com `TransactionTemplate`:
a ação e seu evento de auditoria são gravados juntos ou nenhum dos dois. Os casos de
uso continuam livres de anotações de framework.

### 4. Datas da nota fiscal na entrada

A entrada de material passa a exigir **data de emissão da NF** e **data de
recebimento**, além do registro automático (data/hora e usuário). Todas permitem
filtro por período.

### 5. Genealogia de lotes

O consumo de material passa a ser **alocado a lotes** (um consumo pode vir de vários
lotes). O **saldo do lote** é a quantidade inicial menos o que foi alocado — não há
alocação acima do saldo, e o lote zerado passa a `CONSUMIDO`. Isso permite rastrear
para trás (produto acabado → lotes consumidos → NF/fornecedor) e para frente (lote de
matéria-prima → ordens e lotes que o usaram).

### 6. Genealogia — decisões de implementação (entrega 3)

- **`AlocacaoLote`** (consumo, lote, quantidade, quem, quando) é um fato imutável, criado
  só por `Lote.alocar`, que valida material, unidade, status `DISPONIVEL`, validade na
  data do consumo e saldo.
- **Saldo no lote** (coluna `saldo`, com `CHECK 0 ≤ saldo ≤ quantidade`) em vez de somar
  as alocações a cada leitura: a busca de lotes disponíveis fica simples e indexada.
- **Concorrência**: o caso de uso lê cada lote com `SELECT … FOR UPDATE` dentro da
  transação. Dois consumos simultâneos no mesmo lote são serializados — o segundo vê o
  saldo já baixado e é recusado se não couber.
- **Registro único do consumo**: depois de registrado (e de baixar saldo), não pode ser
  sobrescrito. Correções exigirão um **estorno** explícito (pendente), para a genealogia
  nunca ficar inconsistente.
- **FEFO** como sugestão: a tela distribui pelo que vence primeiro; o usuário pode
  ajustar, e a soma precisa bater com o consumido.
- Consulta **um nível por vez** (`GET /lotes/{id}/rastreabilidade`: origens e destinos);
  a árvore completa se percorre navegando de lote em lote.
- Lotes anteriores à V10 ficam com saldo derivado do status (consumido = 0) e sem
  alocações — a tela indica "sem rastreio" nesses casos.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Hibernate Envers (versionamento automático de tabelas) | Registra mudanças de linha, não ações de negócio; acopla a auditoria ao JPA e produz uma tabela de revisão por entidade, difícil de ler para um auditor |
| Spring Data Auditing (`@CreatedBy` nas entidades JPA) | As entidades JPA são recriadas a partir do domínio a cada gravação — os campos de criação seriam perdidos; e o autor ficaria fora do domínio, sem garantia de que sempre existe |
| Só assinatura, sem histórico | Responde "quem criou", mas não "o que aconteceu desde então" — insuficiente para auditoria |
| `@Transactional` nos controllers | Coloca a fronteira transacional na camada web; a atomicidade é regra do caso de uso |
| Um lote por consumo | Na prática um consumo pode usar o fim de um lote e o início de outro |
| Saldo calculado (soma das alocações) a cada consulta | Consulta de lotes disponíveis vira agregação sobre toda a genealogia; o saldo persistido com `CHECK` é simples e auditável pelos eventos |
| Bloqueio otimista (`@Version`) | Exigiria versionar o lote no domínio e repetir a operação em conflito; o bloqueio de linha é curto (dura a transação) e o volume de consumos simultâneos no mesmo lote é baixo |

## Consequências

- **Positivas**: toda ação tem autor e data; histórico imutável por registro;
  rastreabilidade ponta a ponta para recall; operações atômicas.
- **Negativas / riscos**: mais campos e eventos por operação (volume da trilha cresce
  com o uso — índices por entidade, data e usuário, e consulta paginada); assinaturas de
  fábricas e métodos de domínio mudam (testes atualizados).
- **Entregas**: (1) base de auditoria no backend; (2) telas — colunas, filtros e aba
  Histórico; (3) genealogia de lotes e saldo.
