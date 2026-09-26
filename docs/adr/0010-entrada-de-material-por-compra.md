# ADR-0010 — Entrada de matéria-prima por compra gera lote rastreável

**Status:** Aceito  
**Data:** 2026-09-26  
**Complementa:** ADR-0007 (que deixou a entrada por compra fora de escopo)

---

## Contexto

Até aqui, lotes só nasciam da conclusão de uma ordem de produção. Não havia como
registrar a chegada de matéria-prima comprada — a rastreabilidade começava no meio da
cadeia (sem saber de qual fornecedor e nota fiscal veio o insumo).

## Decisão

1. **Lote passa a ter duas origens**: produção (`ordem_producao_id`) ou compra
   (`fornecedor` + `nota_fiscal`). A migração V8 garante com `CHECK` que todo lote tem
   exatamente uma origem.
2. **Somente matéria-prima** pode dar entrada por compra (regra no caso de uso
   `RegistrarEntradaMaterial`, que conhece o `Material`).
3. **Fornecedor e nota fiscal são obrigatórios** (nota: até 44 caracteres, cabe a
   chave de acesso da NF-e).
4. **Sem duplicidade**: a mesma nota fiscal do mesmo fornecedor não entra duas vezes no
   mesmo material (checagem no caso de uso → HTTP 409, e índice único parcial no banco).
5. **Mesmo formato de número de lote** dos lotes de produção
   (`MAT-{codigo}-{yyyyMM}-{seq}`), reutilizando `Lote.prefixoNumeroLote`/`numeroLote`.
6. Endpoint `POST /api/v1/lotes/entradas`; tela em **Lotes → Entrada de Material**.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Entidade separada `RecebimentoMaterial` | Duplicaria o ciclo de vida do lote (validade, status, vencimento); o lote já é a unidade rastreável |
| Fornecedor como entidade cadastrada | YAGNI nesta fase — texto livre atende o registro; vira entidade quando houver caso de uso (ex.: avaliação de fornecedor) |
| Permitir semiacabado comprado | Fora do escopo pedido; basta relaxar a regra do caso de uso quando necessário |

## Consequências

- **Positivas**: rastreabilidade de ponta a ponta (fornecedor/NF → lote de MP); o
  banco impede lote sem origem ou com origem ambígua.
- **Negativas / riscos**: ainda **não há saldo de estoque** — a entrada gera o lote,
  mas o consumo das ordens não baixa quantidade de lotes de matéria-prima. Próximo passo
  natural quando o controle de estoque entrar no escopo.
