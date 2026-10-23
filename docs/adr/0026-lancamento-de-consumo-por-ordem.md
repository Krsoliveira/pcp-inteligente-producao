# ADR-0026: Lançamento do consumo por ordem, com todos os componentes e tudo ou nada

- **Status**: Aceito
- **Data**: 2026-10-23
- **Complementa**: ADR-0025 (item adicionado ao consumo), ADR-0011 (genealogia de lotes) e ADR-0024 (produto acabado só
  na Conversão)

## Contexto

O consumo era lançado um componente por vez, numa caixa aberta pelo ícone de lápis da tabela de consumo da ordem
em produção. Para uma ordem com vários componentes isso é lento e, pior, deixa o lançamento pela metade quando algo
falha no meio: alguns componentes lançados (e lotes baixados), outros não.

## Decisão

Uma **tela de lançamento por ordem** (`/ordens/{id}/lancar-consumo`) com todos os componentes numa tabela, aberta pelo
botão "Lançar consumo" da ordem em produção.

- **Preenchimento:** para cada linha da lista técnica pendente, a quantidade consumida; se há desvio, a justificativa
  (obrigatória); os lotes vêm sugeridos pela regra FEFO e podem ser ajustados (a soma tem que bater). O que ficar em
  branco **não é lançado agora**. Linhas já lançadas aparecem só como resumo.
- **Itens fora da lista:** o botão "Adicionar item fora da lista" (permissão `CONSUMO_ADICIONAR_ITEM`, ADR-0025)
  acrescenta linhas com material, quantidade, justificativa obrigatória e lotes. Não são oferecidos o material que a
  ordem produz, o que já está na ordem nem produto acabado (salvo ordem do tipo Conversão).
- **Tudo ou nada:** o lançamento vai numa única requisição, `POST /api/v1/ordens-producao/{id}/consumos/lancamento`,
  e o servidor o executa **numa transação**: se qualquer linha for recusada (justificativa, lote sem saldo, regra do
  produto acabado, ordem fora de produção...), **nada** é lançado e **nenhum** saldo de lote é baixado. O erro informa
  o material da linha. O teste de API com PostgreSQL real prova a volta atrás: depois da recusa, o saldo do lote da
  linha válida continua intacto.
- **Permissões:** o lançamento exige `CONSUMO_REGISTRAR`; se vier item adicional, exige também `CONSUMO_ADICIONAR_ITEM`
  (um perfil sem ela recebe 403 ao enviar itens, mas pode lançar as linhas da lista).
- **Regras reaproveitadas:** cada linha usa o mesmo código do registro individual (`registrarNoContexto`) e do item
  adicionado, então a regra dos lotes e do saldo é uma só. O lançamento em lote confere que a ordem está em produção
  (o `PATCH` individual antigo não confere: dívida registrada no ADR-0025).
- **API (só cresce):** endpoint novo; nada foi alterado ou removido. O registro individual (ícone de lápis) continua
  existindo.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Várias chamadas, uma por linha, a partir do navegador | Uma falha no meio deixaria metade lançada e lotes já baixados; não há como desfazer pelo cliente |
| Exigir todas as linhas preenchidas | O operador nem sempre consumiu tudo ao mesmo tempo; o que está em branco fica para depois |
| Tela só para itens adicionados | Duas telas para a mesma tarefa; o lançamento é um só |
| Reescrever o diálogo de registro existente | Risco desnecessário; ele continua como está (a distribuição de lotes tem um componente novo reutilizável) |

## Consequências

- **Positivas**: lançamento rápido e consistente; sem estado intermediário; itens fora da lista no mesmo fluxo; o erro
  diz onde está o problema.
- **Negativas / riscos**:
  - O lançamento é atômico, então uma linha com problema impede as outras até ser corrigida (é o desejado).
  - A distribuição de lotes existe em dois lugares (o diálogo antigo e o componente novo); unificar fica como melhoria.
  - Lançamentos grandes concorrem pelos mesmos lotes com bloqueio de linha; o limite é de 200 linhas por lançamento.
- **Rollback**: reverter o PR; sem migração (usa a V16 do ADR-0025).
- **Pendências**: quantidade base da lista técnica.
