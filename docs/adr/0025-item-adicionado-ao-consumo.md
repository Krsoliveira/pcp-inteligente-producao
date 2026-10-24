# ADR-0025: Item adicionado ao consumo de uma ordem, fora da lista técnica

- **Status**: Aceito
- **Data**: 2026-10-23
- **Complementa**: ADR-0007 (consumo), ADR-0011 (genealogia de lotes), ADR-0020 (acesso por perfis) e ADR-0024 (produto
  acabado só na Conversão)

## Contexto

O consumo de uma ordem é projetado a partir da lista técnica: uma linha por componente, com a quantidade
planejada. Na prática o operador às vezes consome um material que **não está na lista** (uma cola, um
lubrificante). Hoje isso não é possível: só existem as linhas projetadas.

## Decisão

O usuário pode **adicionar um item** ao consumo de uma ordem em produção ao lançar o consumo.

- **A linha nasce não planejada:** quantidade planejada **0**. O desvio passa a ser tudo o que foi consumido, então a
  justificativa é **obrigatória**, pela mesma regra que já vale para qualquer desvio (o responsável é o usuário
  logado, nunca um nome digitado).
- **Já nasce registrada:** a linha é criada e o consumo registrado na mesma transação, com a distribuição por lotes
  (genealogia, ADR-0011) e a baixa de saldo. Não existe "pendente" para item adicionado.
- **Condições (422 quando não atendidas):**
  - a ordem precisa estar **em produção**;
  - o material não pode ser o que a ordem produz;
  - **produto acabado só em ordem do tipo Conversão** (ADR-0024);
  - se o material **já está na ordem**, o sistema recusa e manda incluir os lotes na linha dele (mais de um lote do
    mesmo componente continua sendo uma linha, e o relatório soma);
  - a quantidade consumida é maior que zero e a soma dos lotes é igual a ela.
- **API (só cresce):** `POST /api/v1/ordens-producao/{id}/consumos` (201). `ConsumoMaterialResponse` ganha
  `naoPlanejado`. A permissão é **`CONSUMO_ADICIONAR_ITEM`**, separada de `CONSUMO_REGISTRAR`: o consumo não planejado é
  mais sensível; o Administrador a tem sempre e os demais perfis só se um gestor a conceder.
- **Auditoria:** evento novo `CONSUMO_ADICIONADO` (no lugar de `CONSUMO_REGISTRADO` para essas linhas), com material,
  quantidade, justificativa e lotes.
- **Migração V16:** `ck_consumo_planejada` passa de `> 0` para `>= 0`. As linhas projetadas continuam exigindo
  planejado maior que zero (regra do domínio); planejado 0 identifica o item adicionado.
- **Reuso:** o registro de consumo foi extraído para `registrarNoContexto`, usado pelo registro de uma linha e pelo
  item adicionado, para que a regra dos lotes e do saldo seja uma só.
- **Interface (nesta fatia):** a tabela de consumo da ordem mostra o selo "Não planejado". A tela de lançamento por
  ordem, com o botão "Adicionar item", vem no PR seguinte.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Usar `CONSUMO_REGISTRAR` | Quem lança o planejado passaria a poder incluir qualquer material |
| Coluna `nao_planejado` em vez de planejado 0 | Mais um campo que pode divergir; planejado 0 já diz o mesmo e o desvio fica correto sem regra extra |
| Segunda linha para o mesmo material (outro lote) | A linha do componente já aceita vários lotes; duplicar quebraria a soma do relatório |
| Criar a linha e registrar em duas chamadas | Uma falha no meio deixaria uma linha pendente sem justificativa; uma transação evita isso |

## Consequências

- **Positivas**: o consumo real, inclusive o que a lista não previa, entra no sistema com rastreabilidade e
  justificativa; o relatório de consumo mostra essas linhas (planejado 0).
- **Negativas / riscos**:
  - Restaurar o `CHECK > 0` só é possível se não houver linhas com planejado 0; depois que existirem, a volta é
    manual, com decisão sobre essas linhas.
  - Perfis existentes não ganham a permissão sozinhos.
  - O endpoint antigo de registro de consumo (`PATCH`) não confere se a ordem está em produção; o novo confere.
    Registrado como dívida, fora do escopo desta mudança.
- **Rollback**: reverter o código é seguro. Desfazer a V16 exige tratar as linhas com planejado 0 antes.
- **Pendências**: tela de lançamento por ordem (todos os componentes de uma vez, tudo ou nada), com "Adicionar item".
