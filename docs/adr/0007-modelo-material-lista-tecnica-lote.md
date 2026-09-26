# ADR-0007 — Expansão do domínio: Material, Lista Técnica (BOM), Consumo e Rastreabilidade por Lote

**Status:** Aceito  
**Data:** 2026-08-23  
**Fase:** 5 — Módulo de IA / Preparação do domínio expandido  
**Supersede parcialmente:** ADR-0006 (dataset `ordens_producao.csv` descartado — ver seção Consequências)

---

## Contexto

O modelo atual de `OrdemProducao` trata o campo `produto` como um `VARCHAR` livre.
Isso foi suficiente para as fases iniciais, mas cria um teto baixo para as próximas:

- O **módulo de IA (Fase 5)** precisa analisar padrões por material, por componente e
  por gargalo de centro de trabalho — impossível sem estrutura formal de produto.
- O **deploy com dado real (Fase 6)** exige que o sistema demonstre rastreabilidade
  industrial, não apenas um CRUD de ordens.
- A **auditoria e conformidade** (contexto SAP/PCP do portfólio) requer saber *exatamente*
  quais materiais, de quais lotes, em quais quantidades, foram consumidos para fabricar
  cada unidade entregue.

O PCP industrial real opera sobre dois conceitos centrais ausentes no modelo:

1. **Lista Técnica (BOM — Bill of Materials)**: a "receita de bolo" que define quais
   componentes e em que quantidade são necessários para fabricar um material. Uma mesma
   peça pode ter versões diferentes de lista (ex.: especificação padrão vs. especificação
   com tolerâncias mais rígidas).

2. **Lote de produção**: cada execução de uma ordem gera um lote rastreável, com número,
   data de fabricação e validade — fundamental para recalls, auditorias e análise de
   qualidade.

---

## Decisão

### 1. Nova entidade: `Material`

Entidade de domínio que unifica produtos acabados, semiacabados e matérias-primas num
único aggregate, distinguidos por tipo:

```
Material
  id              UUID
  codigo          VARCHAR(30)   UNIQUE NOT NULL
  descricao       VARCHAR(200)  NOT NULL
  unidadeMedida   VARCHAR(10)   NOT NULL  (ex.: "un", "kg", "m")
  tipo            ENUM: PRODUTO_ACABADO | SEMIACABADO | MATERIA_PRIMA
```

**Regra de domínio:** `MATERIA_PRIMA` nunca possui `ListaTecnica`. Qualquer tentativa
de associar uma lista a uma matéria-prima lança `RegraDeNegocioException`.

### 2. Nova entidade: `ListaTecnica` com versionamento

Cada material (não matéria-prima) pode ter múltiplas versões de lista técnica. Versões
convivem, mas apenas uma pode estar `ATIVA` por vez para um mesmo material.

```
ListaTecnica
  id              UUID
  material_id     FK → Material
  versao          VARCHAR(20)   NOT NULL  (ex.: "v1", "v2-tolerancia-extra")
  descricao       VARCHAR(300)
  status          ENUM: EM_REVISAO | ATIVA | OBSOLETA
  vigente_de      DATE          NOT NULL
  vigente_ate     DATE          (nullable — sem data fim = indefinidamente ativa)
```

**Regra de domínio:** ao ativar uma versão, qualquer outra versão `ATIVA` do mesmo
material é automaticamente movida para `OBSOLETA`.

### 3. Nova entidade: `ItemListaTecnica`

Os componentes de cada versão de lista, com suas quantidades planejadas:

```
ItemListaTecnica
  id                    UUID
  lista_tecnica_id      FK → ListaTecnica
  material_componente   FK → Material   (qualquer tipo, inclusive outro semiacabado)
  quantidade_planejada  DECIMAL(12,4)   NOT NULL
  unidadeMedida         VARCHAR(10)     NOT NULL
```

Isso habilita **BOM multinível**: um semiacabado que é componente de outro semiacabado
que é componente de um produto acabado.

### 4. `OrdemProducao` referencia `Material` e `ListaTecnica`

O campo `produto VARCHAR` é substituído por duas FKs:

```
OrdemProducao
  + material_id        FK → Material        (o que será fabricado)
  + lista_tecnica_id   FK → ListaTecnica    (a versão da receita usada)
  + quantidade_produzida  INTEGER  (preenchida na conclusão; NULL enquanto aberta)
```

**Regra de domínio:** `lista_tecnica_id` deve referenciar uma versão pertencente ao
mesmo `material_id`. Versões de outros materiais são rejeitadas na criação da ordem.

### 5. Nova entidade: `ConsumoMaterial`

Registra o consumo real de cada componente durante a execução da ordem. Pode ser
informado manualmente pelo operador ou calculado automaticamente a partir da lista
técnica × quantidade produzida.

```
ConsumoMaterial
  id                    UUID
  ordem_producao_id     FK → OrdemProducao
  material_id           FK → Material
  lote_id               FK → Lote           (de qual lote veio o componente)
  quantidade_planejada  DECIMAL(12,4)       NOT NULL
  quantidade_consumida  DECIMAL(12,4)       (NULL enquanto não registrada)
  desvio                DECIMAL(12,4)       GERADO: consumida - planejada
  justificativa         TEXT                (obrigatória se desvio != 0)
  justificado_por       VARCHAR(150)
  justificado_em        TIMESTAMPTZ
```

**Regra de domínio:** a ordem só pode ser concluída quando todos os `ConsumoMaterial`
estiverem com `quantidade_consumida` preenchida. Se qualquer `desvio != 0`, a
`justificativa` é obrigatória — sem ela, a conclusão é bloqueada.

### 6. Nova entidade: `Lote`

Todo material produzido (conclusão de ordem) gera um lote rastreável. Matérias-primas
também podem ter lote (entrada por compra), mas isso está fora do escopo desta fase.

```
Lote
  id                  UUID
  numero_lote         VARCHAR(40)   NOT NULL  (gerado: MAT-{codigo}-{yyyyMM}-{seq})
  material_id         FK → Material
  ordem_producao_id   FK → OrdemProducao   (nullable para entradas manuais futuras)
  quantidade          DECIMAL(12,4) NOT NULL
  unidadeMedida       VARCHAR(10)   NOT NULL
  data_fabricacao     DATE          NOT NULL
  data_validade       DATE          NOT NULL  (obrigatória — operador define prazo mesmo
                                               sem vencimento natural do produto)
  status              ENUM: DISPONIVEL | BLOQUEADO | CONSUMIDO | VENCIDO
  criado_em           TIMESTAMPTZ   NOT NULL
```

**Regra de domínio:** `data_validade >= data_fabricacao`. Lote nasce sempre
`DISPONIVEL`. A transição para `VENCIDO` é disparada por processo agendado quando
`data_validade < hoje`.

---

## Fluxo de vida de uma ordem com o novo modelo

```
1. Planejador cria OrdemProducao
   → seleciona Material + versão de ListaTecnica ativa
   → sistema projeta ConsumoMaterial[] a partir dos itens da lista × quantidade planejada

2. Ordem percorre o ciclo: PLANEJADA → LIBERADA → EM_PRODUCAO

3. Operador registra consumo real (ou confirma o automático)
   → se desvio != 0: sistema exige justificativa antes de permitir próximo passo

4. Planejador conclui a ordem
   → sistema valida que todos os ConsumoMaterial estão registrados (e justificados se desviaram)
   → sistema gera Lote com numero_lote automático e status DISPONIVEL
   → OrdemProducao.status = CONCLUIDA
```

---

## Alternativas consideradas

| Alternativa | Por que descartada |
|---|---|
| Manter `produto` como `VARCHAR` e adicionar BOM separada por texto livre | Sem integridade referencial; impossível agregar consumo por material para a IA |
| Entidades separadas `ProdutoAcabado`, `Semiacabado`, `MateriaPrima` | Tabelas por herança criam joins desnecessários; polimorfismo via tipo no mesmo aggregate é mais limpo e segue o padrão do domínio atual |
| `Cliente` como entidade vinculada à versão da lista | Fora de escopo nesta fase (YAGNI — ver P5); versão da lista é variante técnica; cliente entra quando houver caso de uso concreto |
| Validade opcional para materiais sem vencimento natural | Torna o campo nullable e cria lógica condicional espalhada; campo obrigatório com prazo longo definido pelo operador (ex.: 99 anos) é mais simples e auditável |
| Flyway seed SQL para o novo dataset | Mesma razão do ADR-0006: separar schema de dados de negócio |

---

## Consequências

### Impacto no schema existente

- `ordem_producao.produto VARCHAR(120)` é **removido** — substituído por `material_id` e `lista_tecnica_id`.
- Migração Flyway `V4__remover_produto_adicionar_material.sql` necessária.
- O dataset sintético de 105 ordens (`ordens_producao.csv`) é **descartado** — não tem
  estrutura de material nem lista técnica. Um novo dataset compatível com o modelo
  expandido deverá ser levantado (Kaggle ou geração sintética).

### Novas migrações previstas

| Versão | Conteúdo |
|---|---|
| V4 | Tabela `material` |
| V5 | Tabelas `lista_tecnica` e `item_lista_tecnica` |
| V6 | Tabelas `lote` e `consumo_material` |
| V7 | Alterar `ordem_producao`: remover `produto`, adicionar `material_id`, `lista_tecnica_id`, `quantidade_produzida` |

### Novos casos de uso previstos

- `CadastrarMaterial`
- `CriarListaTecnica` / `AtivarVersaoListaTecnica`
- `RegistrarConsumoMaterial` (manual ou automático)
- `ConcluirOrdemProducao` (substitui `AtualizarStatusOrdemProducao` para o status CONCLUIDA — agora com geração de lote)

### Ganhos para a IA (Fase 5)

Com `ConsumoMaterial` e `Lote` estruturados, o módulo de IA terá acesso a:
- Desvios históricos de consumo por material e por centro de trabalho
- Taxa de retrabalho (ordens com desvio justificado por falha)
- Lead time real (data_fabricacao do lote − inicio_planejado da ordem)
- Análise de gargalo por componente (quais materiais atrasam mais ordens)

### Custo

Aumento significativo de entidades e casos de uso. O escopo de uma única PR de fase
seria grande demais — recomendado dividir em duas fases:

- **Fase 5a:** `Material` + `ListaTecnica` + adaptação de `OrdemProducao`
- **Fase 5b:** `ConsumoMaterial` + `Lote` + caso de uso `ConcluirOrdemProducao`