# ADR-0019: Várias versões em uso por lista técnica, escolha na ordem e comparação

- **Status**: Aceito
- **Data**: 2026-10-23
- **Altera**: regra de ativação do ADR-0007 (expansão do domínio: material, lista técnica e consumo)

## Contexto

Até aqui, ativar uma versão de lista técnica **obsoletava automaticamente** a versão ativa anterior do
mesmo material. Isso pressupõe que só existe uma forma de produzir o material. Na prática um material
pode ter **várias formas de ser produzido ao mesmo tempo** (ex.: receita padrão e receita com
substituição de um insumo), e o usuário precisa escolher qual usar em cada produção. Faltavam também
um identificador da lista como um todo, uma forma de comparar versões e a coluna de descrição
separada do código do material na tabela.

## Decisão

**Modelo.** Cada material tem **uma lista técnica**, que tem **várias versões**. A lista é identificada
por um `listaId` (UUID); todas as versões do mesmo material compartilham o mesmo `listaId`. Cada versão
continua com o próprio `id` (UUID) e com o rótulo livre `versao` (ex.: `v1`, `v2-tolerancia-extra`),
único dentro do material.

**Ciclo de vida.** `EM_REVISAO → ATIVA → OBSOLETA`, como antes, mas:

1. **Ativar não obsoleta mais ninguém.** Podem existir várias versões `ATIVA` do mesmo material.
2. **Quem decide quando uma versão sai de uso é o usuário**: nova ação "Retirar de uso"
   (`PATCH /api/v1/listas-tecnicas/{id}/retirar-de-uso`, `ATIVA → OBSOLETA`, auditada). Nada é
   obsoletado por efeito colateral.

**Escolha da versão na ordem.** Ao criar uma ordem, a versão da lista é **obrigatória quando o material
tem mais de uma versão ativa**; com uma só, a tela já a deixa pré-selecionada. O servidor passa a
validar que a versão escolhida **pertence ao material da ordem e está `ATIVA`** (antes isso só era
garantido pela tela).

**Comparação.** `GET /api/v1/listas-tecnicas/comparar?a={id}&b={id}` compara duas versões **da mesma
lista** (mesmo `listaId`) e devolve os componentes adicionados, removidos, com quantidade ou unidade
alterada e iguais. Só leitura. **Acesso liberado a qualquer usuário autenticado por ora**; a
segregação por perfil fica como pendência (hoje existem `PLANEJADOR` e `GERENTE`).

**Tela.** A tabela de listas mostra o código do material e a descrição em colunas separadas.

**Banco.** Migração V12 adiciona `lista_tecnica.lista_id` (UUID, `NOT NULL`), preenchido com um
identificador por material nos dados existentes. As ordens antigas continuam apontando para a mesma
versão (`ordem_producao.lista_tecnica_id` não muda).

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Duas tabelas (`lista` e `versao_da_lista`) | É o modelo "puro", mas obriga a renomear entidades, repositórios e API; o agrupador `lista_id` entrega o mesmo comportamento com mudança bem menor |
| Manter "ativar obsoleta a anterior" e só adicionar a escolha | Impede ter duas receitas válidas ao mesmo tempo, que é o objetivo |
| Obsoletar versões antigas por regra (idade, quantidade) | O usuário quer decidir; regra implícita surpreende |
| Várias listas independentes para o mesmo material | O usuário esclareceu que são várias **versões** de uma lista, não várias listas |

## Consequências

- **Positivas**: receitas alternativas convivem; a versão usada fica explícita e validada; versões são
  comparáveis; nenhuma ordem existente é alterada.
- **Negativas / riscos**:
  - O `PATCH .../ativar` muda de comportamento (não obsoleta mais). Só o nosso frontend o consome, por
    isso não há `/v2`; os testes que afirmavam o comportamento antigo foram reescritos.
  - `lista_id` único por material é garantido pelo caso de uso (`CadastrarListaTecnica` reaproveita o
    `listaId` existente), não por restrição do banco.
  - Versões antigas esquecidas como `ATIVA` aparecem na escolha da ordem; a tela deve deixar claro
    qual é qual.
- **Rollback**: migrações não se editam; desfazer exige uma migração nova que remova `lista_id` (e
  voltar o código). Em desenvolvimento, `docker compose down -v` recria o banco. Em qualquer ambiente
  com dados reais, fazer backup antes.
- **Pendências**: segregar por perfil quem vê as listas e as comparações; comparar mais de duas
  versões de uma vez.
