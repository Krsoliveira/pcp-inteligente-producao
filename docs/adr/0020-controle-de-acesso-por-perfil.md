# ADR-0020: Controle de acesso por perfis de permissões

- **Status**: Aceito
- **Data**: 2026-10-23
- **Substitui**: o campo `usuario.perfil` (enum `PLANEJADOR` / `GERENTE`), que existia mas não era consultado por nenhum endpoint

## Contexto

Hoje qualquer usuário autenticado pode fazer tudo: ativar e retirar de uso listas técnicas, criar ordens,
registrar notas fiscais. O `perfil` do usuário era gravado e colocado nas autoridades do Spring Security,
mas **nenhum endpoint o conferia**, e o autocadastro público dava a qualquer pessoa o mesmo poder de um
planejador. Também não há como o gerente criar usuários nem mudar o que cada um pode fazer. O pedido
(23/10): segregar por tipos de perfil identificados por um id (não por nome de cargo), em que **quem tem
acesso à gestão marca quais acessos cada perfil tem**, com **rastreio de alterações**, começando pelas listas
técnicas e valendo para o sistema todo.

## Decisão

**Modelo (RBAC com permissões granulares).**

- **Permissão**: capacidade fixa, definida no código (`Permissao`), identificada por texto (ex.:
  `LISTA_COMPARAR`). É o catálogo das "caixinhas" que se marcam. Adicionar uma permissão nova é uma mudança
  de código; marcar e desmarcar é dado.
- **Perfil de acesso**: `id` (UUID), nome único, descrição e um conjunto de permissões. Não se amarra a
  cargo: quem decide o nome é o gerente.
- **Usuário**: tem **um** perfil (`usuario.perfil_id`). Não há permissões soltas por usuário.
- As autoridades do usuário são lidas do banco **a cada requisição** (já era assim com `UserDetailsService`):
  mudar um perfil ou o perfil de alguém vale na hora, sem esperar o token expirar.

**Perfis de sistema (nascem na migração).**

| Perfil | Permissões | Regras |
|---|---|---|
| **Administrador** | todas, sempre (inclusive as futuras: não são linhas no banco, é o conjunto completo do código) | não pode ser editado nem excluído; é o único perfil que já nasce. O administrador inicial (`ADMIN_EMAIL`) recebe este perfil |
| **Visualizador** | só consulta (painel, materiais, listas ativas, ordens, estoque, notas) | é o **perfil padrão do autocadastro**; pode ter as permissões editadas, não pode ser excluído |

O Administrador cria o perfil Gerente (e o que mais precisar); o gerente cria o restante.

**Catálogo de permissões.**

| Área | Permissões |
|---|---|
| Painel | `DASHBOARD_VISUALIZAR` |
| Materiais | `MATERIAL_VISUALIZAR`, `MATERIAL_CADASTRAR` |
| Listas técnicas | `LISTA_VISUALIZAR_ATIVAS`, `LISTA_VISUALIZAR_TODAS`, `LISTA_COMPARAR`, `LISTA_CADASTRAR` (inclui nova versão a partir de outra), `LISTA_ATIVAR`, `LISTA_RETIRAR_DE_USO` |
| Ordens | `ORDEM_VISUALIZAR`, `ORDEM_CRIAR`, `ORDEM_ATUALIZAR_STATUS`, `ORDEM_CONCLUIR` |
| Consumos | `CONSUMO_VISUALIZAR`, `CONSUMO_REGISTRAR` |
| Tipos de ordem | `TIPO_ORDEM_VISUALIZAR`, `TIPO_ORDEM_GERENCIAR` |
| Notas fiscais | `NOTA_FISCAL_VISUALIZAR`, `NOTA_FISCAL_REGISTRAR` |
| Estoque e lotes | `ESTOQUE_VISUALIZAR` (estoque, lotes e rastreabilidade) |
| Auditoria | `AUDITORIA_VISUALIZAR` |
| Acessos | `ACESSO_VISUALIZAR` (ver perfis e usuários), `PERFIL_GERENCIAR`, `USUARIO_GERENCIAR` |

**Regras de quem pode conceder o quê.**

1. **Só se concede o que se tem**: ao criar ou editar um perfil, o conjunto resultante tem de estar contido
   nas permissões de quem faz a alteração; o mesmo ao atribuir um perfil a um usuário. É o que faz o gerente
   só criar perfis iguais ou menores que o dele, e impede escalada de privilégio.
2. Ninguém altera **o próprio perfil**.
3. O último usuário com o perfil Administrador não pode ser movido para outro perfil (evita trancar o sistema).
4. Um perfil só pode ser excluído se **nenhum usuário** o usa e se não é de sistema.

**Aplicação no servidor (nunca só na tela).**

- Cada endpoint exige a permissão por `@PreAuthorize("hasAuthority(...)")` (método de segurança habilitado);
  sem permissão, 403 em formato RFC 9457; sem login, 401, como antes.
- **Por objeto** nas listas técnicas: quem só tem `LISTA_VISUALIZAR_ATIVAS` enxerga apenas versões ativas (as
  demais aparecem como inexistentes, 404, sem revelar que existem); a comparação exige `LISTA_COMPARAR` **e**
  que as duas versões sejam visíveis para quem pede.
- Novo `GET /api/v1/acessos/eu` (fora de `/auth/**`, que é público) devolve o usuário logado, o perfil e as
  permissões, para o frontend mostrar ou esconder telas e botões. O frontend nunca é a única barreira.

**Gestão (API nova, contrato só cresce).** `GET /api/v1/acessos/eu`, `GET /api/v1/acessos/permissoes`, `GET|POST /api/v1/acessos/perfis`,
`GET|PUT|DELETE /api/v1/acessos/perfis/{id}`, `GET|POST /api/v1/acessos/usuarios`,
`PATCH /api/v1/acessos/usuarios/{id}/perfil`. O gerente define a senha provisória do usuário que cria (não há
envio de e-mail; troca obrigatória no primeiro acesso fica como pendência).

**Rastreio.** Toda mudança entra na trilha de auditoria (ADR-0011): perfil criado, alterado (com as permissões
adicionadas e removidas), excluído; usuário criado; perfil de um usuário atribuído (de qual para qual). Novas
entidade `PERFIL_ACESSO` e ações `EXCLUIDO` e `PERFIL_ATRIBUIDO`.

**Banco (migração V13).** Cria `perfil_acesso` e `perfil_acesso_permissao`, insere os dois perfis de sistema,
**exclui todos os usuários existentes** (decisão do usuário: recomeçar já com a regra) e troca
`usuario.perfil` (texto) por `usuario.perfil_id` (obrigatório, com chave estrangeira). Com a tabela vazia a
troca é segura e a coluna antiga é removida na mesma migração.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Manter o enum `PLANEJADOR/GERENTE` e checar por papel | Rígido: cada ajuste exige código; o usuário quer criar e editar perfis |
| Permissões marcadas direto em cada usuário | Difícil de manter e de auditar; o pedido fala em perfis. Exceções por usuário podem vir depois |
| Vários perfis por usuário | Mais flexível, mas o resultado (união) é mais difícil de explicar; o usuário escolheu um perfil por usuário |
| Guardar as permissões em listas no JWT | Mudança de perfil só valeria após novo login; hoje as autoridades vêm do banco a cada requisição |
| Preservar os usuários existentes na migração | Os usuários atuais são de teste; recomeçar simplifica e evita herdar poder sem regra |

## Consequências

- **Positivas**: o sistema deixa de ser "todo mundo pode tudo"; o gerente administra acessos sem mexer em
  código; toda mudança de acesso é rastreável; escalada de privilégio contida pela regra de delegação.
- **Negativas / riscos**:
  - A migração **apaga os usuários**. Quem usa o sistema precisa de um administrador inicial
    (`ADMIN_EMAIL`, `ADMIN_NOME`, `ADMIN_SENHA` no ambiente); sem ele ninguém entra.
  - Os endpoints passam a devolver 403: testes de integração e telas precisam de usuário com o perfil certo.
  - Uma permissão nova no código não aparece em perfis existentes até alguém marcá-la (o Administrador a recebe
    automaticamente).
  - Quem tem `PERFIL_GERENCIAR` e `USUARIO_GERENCIAR` concentra poder: a regra "só concede o que tem" e a
    auditoria são a contenção.
- **Rollback**: migrações não se editam; desfazer exige uma migração que volte `usuario.perfil` e remova as
  tabelas novas, além de voltar o código. Os usuários apagados não voltam: **backup antes de aplicar em
  qualquer ambiente com dados reais**. Em desenvolvimento, `docker compose down -v` recria o banco.
- **Validação antes da autorização**: a validação do corpo (400) roda antes da segurança por método (403),
  porque os argumentos são resolvidos antes da chamada do método. Quem não tem permissão e manda um corpo
  inválido recebe 400. Não vaza dado nem permite agir; os testes usam corpos válidos para provar o 403.
- **Pendências**: troca de senha obrigatória no primeiro acesso; exceções de permissão por usuário; vários
  perfis por usuário, se o uso pedir.
