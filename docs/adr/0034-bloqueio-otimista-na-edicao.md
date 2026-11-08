# ADR-0034: Bloqueio otimista na edição de perfil de acesso e tipo de ordem

- **Status**: Aceito
- **Data**: 2026-11-07

## Contexto

Duas telas editam um registro inteiro, que o usuário abre, altera e salva: o **perfil de acesso** (nome, descrição e permissões) e o **tipo de ordem** (nome, descrição e cor). O PUT grava o formulário inteiro por cima do que está no banco.

Se duas pessoas abrem o mesmo registro e salvam uma depois da outra, a segunda apaga a alteração da primeira sem que ninguém seja avisado. Exemplo:

1. Ana tira uma permissão de um perfil e salva.
2. Bruno, que abriu o perfil antes disso, troca só a descrição e salva.
3. A permissão que Ana tirou volta.

A trilha de auditoria registra a mudança do Bruno como se ele tivesse devolvido a permissão de propósito. É o problema clássico da **atualização perdida** (*lost update*).

As mutações com pré-condição de estado já estão protegidas: ordem, consumo e lote usam `SELECT … FOR UPDATE` (prova em `ConcorrenciaDaOrdemApiTest`), e a unicidade fica no banco (409). O `FOR UPDATE` sozinho não resolve a edição. Ele faz as duas gravações entrarem em fila, mas a segunda continua gravando o formulário antigo. O que falta é saber **qual versão a tela leu**.

## Decisão

**Bloqueio otimista com número de versão, conferido no caso de uso:**

- **Banco (V20):** coluna `versao BIGINT NOT NULL DEFAULT 0` em `perfil_acesso` e `tipo_ordem`.
- **Domínio:** `PerfilAcesso` e `TipoOrdem` carregam a versão, e cada alteração (`redefinir`, `atualizar`) soma 1. A regra continua sem framework.
- **Caso de uso:**
  - lê o registro com `buscarPorIdParaAtualizar` (`FOR UPDATE`), para que duas edições simultâneas entrem em fila;
  - compara a versão enviada pela tela com a do banco;
  - se forem diferentes, lança `EdicaoConcorrenteException` (uma `ConflitoException`), e nada é alterado nem auditado.
- **Ordem das conferências no perfil:** autorização ("só se concede o que se tem") primeiro, versão depois. Quem não alcança o perfil recebe 403, não 409.
- **API:**
  - as respostas de perfil e de tipo de ordem ganham `versao`;
  - o PUT aceita `versao` **opcional**: sem ela, a edição segue como antes, porque o contrato de `/api/v1` só cresce;
  - o 409 de edição simultânea traz `codigo: "EDICAO_CONCORRENTE"` no Problem Details, como o `TROCAR_SENHA` do 403 (ADR-0021). Assim a tela o diferencia do 409 de nome repetido.
- **Telas:**
  - guardam a versão ao abrir a edição e a enviam ao salvar;
  - no 409 de edição simultânea, mostram o aviso, mantêm o que foi digitado e oferecem **Carregar versão atual**, que troca o formulário pelo registro atual com a versão nova.

## Alternativas consideradas

- **`@Version` do JPA.** O adaptador monta uma entidade JPA nova a cada gravação (`JpaEntity.de(dominio)`). Para o `@Version` valer, a versão teria que atravessar o domínio do mesmo jeito. Somaria ainda um segundo mecanismo (`ObjectOptimisticLockingFailureException` no `merge`) ao `FOR UPDATE` que o projeto já usa. Com a conferência no caso de uso, existe um único mecanismo, visível e testável sem banco.
- **`ETag` + `If-Match` (412 Precondition Failed).** É o padrão HTTP para isso. Exigiria cabeçalhos em todas as respostas e no cliente Axios, e um status novo para o frontend tratar, enquanto o projeto já usa 409 para conflito. O campo no corpo aparece no Swagger e é mais simples de explicar.
- **`versao` obrigatória no PUT.** Fecha a brecha para qualquer cliente, mas quebra quem já chama a API sem ela. Pela regra do projeto, isso exigiria uma `/v2`. O único cliente é o frontend, que sempre envia a versão.
- **Só `FOR UPDATE`, sem versão.** Coloca as gravações em fila, mas a segunda continua sobrescrevendo a primeira com dados antigos. Não resolve.

## Consequências

- **Positivas:**
  - nenhuma edição de perfil ou de tipo de ordem apaga a de outra pessoa em silêncio;
  - a trilha de auditoria deixa de registrar "desfazimentos" que ninguém pediu;
  - provado em banco real com requisições paralelas (`EdicaoConcorrenteApiTest`, repetido 5 vezes por tela).
- **Negativas:**
  - um cliente que não envia a versão continua sem proteção, por compatibilidade;
  - a tela que guardar dados velhos de cache recebe 409 sem que ninguém tenha editado. A mensagem e o **Carregar versão atual** cobrem esse caso.
- **Rollback:** reverter o código basta. O código anterior ignora a coluna `versao`. A V20 não se edita; remover a coluna seria uma migração nova.
- **Próximo passo:** outros registros editáveis entram no mesmo padrão. Fica registrada a varredura dos CRUDs com edição ou mudança de estado, incluindo a ativação da lista técnica, que ainda não trava o registro.
