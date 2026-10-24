# ADR-0021: Troca de senha no primeiro acesso e redefinição pelo gestor

- **Status**: Aceito
- **Data**: 2026-10-23
- **Complementa**: ADR-0020 (controle de acesso por perfis), que deixou a troca de senha como pendência

## Contexto

Quando um gestor cria um usuário (ADR-0020), ele define a **senha provisória**, e por isso a conhece. Hoje
essa senha vale para sempre: o usuário não tem como trocá-la, e o gestor (ou quem ele a repassou) continua
podendo entrar com a conta dele. Também não há como recuperar quem esquece a senha provisória.

## Decisão

**Marca de troca obrigatória.** O usuário ganha `deve_trocar_senha` (migração V14, padrão `false`). Fica
**ligada** para quem é criado por um gestor e para quem teve a senha redefinida por um gestor; fica
**desligada** no autocadastro (a pessoa escolheu a própria senha) e no administrador inicial (a senha vem
do ambiente, controlada por quem opera o sistema).

**Enquanto a marca estiver ligada, o servidor recusa tudo (403)**, menos consultar quem sou (`/acessos/eu`)
e trocar a senha. Isso é feito pelas autoridades: um usuário com a marca ligada não recebe nenhuma
permissão (só a autoridade interna `SENHA_PROVISORIA`), então toda anotação `@PreAuthorize` falha. O token
continua o mesmo; como as autoridades são lidas do banco a cada requisição, a liberação vale assim que a
senha é trocada. A resposta 403 desse caso leva o código `TROCAR_SENHA` para a tela saber o que fazer.

**API nova (o contrato só cresce).**

| Endpoint | Quem | O que faz |
|---|---|---|
| `PUT /api/v1/acessos/minha-senha` | qualquer usuário autenticado, mesmo com a marca ligada | `{senhaAtual, novaSenha}`; valida a atual com BCrypt, desliga a marca, 204 |
| `POST /api/v1/acessos/usuarios/{id}/redefinir-senha` | `USUARIO_GERENCIAR` | `{senhaProvisoria}`; troca a senha do usuário e **liga a marca**, 204 |
| `GET /api/v1/acessos/eu` | autenticado | ganha `deveTrocarSenha`; com a marca ligada devolve `permissoes: []` |

**Regras.**
1. Nova senha: **mínimo de 8 e máximo de 72 bytes** (limite do BCrypt) e **diferente da atual**. Sem exigência
   de complexidade.
2. Senha atual errada: **422** (nunca 401, porque o frontend desloga em 401 e o usuário perderia o que digitou).
3. **Quem redefine só alcança quem está ao seu alcance**: o perfil do alvo não pode ter permissões que o gestor
   não tem. Sem essa regra, um gerente redefiniria a senha de um Administrador e assumiria a conta dele.
4. Ninguém redefine a **própria** senha por esse caminho (usa "Alterar minha senha", que exige a atual).
5. Trocar a senha pelo próprio usuário e redefinir pelo gestor entram na trilha de auditoria (`SENHA_ALTERADA`,
   `SENHA_REDEFINIDA`), **sem a senha** nem o hash.
6. `PUT /acessos/minha-senha` tem **limite de requisições por IP** (como o login), porque aceita adivinhar a
   senha atual com um token roubado.

**Tela.** "Defina sua senha" (forçada, sem menu, enquanto a marca estiver ligada) e "Alterar minha senha"
(voluntária, no menu), com o mesmo formulário. Na tela de usuários, "Redefinir senha" para quem tem
`USUARIO_GERENCIAR`.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Expirar a senha provisória por tempo | Exige relógio, job e um caminho de reenvio; a marca resolve o essencial |
| Só o frontend forçar a troca | O servidor precisa ser a barreira: sem isso a senha provisória continuaria valendo chamando a API direto |
| Filtro novo que bloqueia por caminho | Duplica a fonte da verdade das permissões; as autoridades já fazem isso com uma linha |
| Enviar link de redefinição por e-mail | Não há envio de e-mail no sistema; o gestor entrega a senha provisória |
| Forçar a troca do administrador inicial | A senha dele vem do ambiente e é de quem opera o sistema; trocar a deixaria divergente do ambiente |

## Consequências

- **Positivas**: a senha provisória deixa de ser permanente e de ser conhecida pelo gestor; quem a esquece é
  recuperado por um gestor; a regra vale mesmo chamando a API direto.
- **Negativas / riscos**:
  - O gestor continua conhecendo a senha provisória até o primeiro acesso (inevitável sem e-mail).
  - Se o usuário perder a provisória, só um gestor com alcance sobre o perfil dele a redefine.
  - **Recuperação do último Administrador** (verificada em teste e no ambiente de dev): `ADMIN_*` com um
    e-mail que já existe não altera nada (nem a senha); com um e-mail **novo** cria um segundo Administrador,
    que redefine a senha do primeiro pela tela de Usuários (o primeiro troca a provisória ao entrar). Exige
    acesso ao ambiente de deploy; `ADMIN_EMAIL` sem `ADMIN_SENHA` impede a aplicação de subir. Depois da
    recuperação, remover o Administrador extra se não for mais necessário.
  - Bloqueio por autoridades faz o 403 aparecer em todos os endpoints; o código `TROCAR_SENHA` evita que
    pareça falta de permissão.
- **Rollback**: a V14 só adiciona uma coluna. Desfazer é uma migração nova que a remove e voltar o código; sem
  perda de dados.
- **Pendências**: desativar usuário sem excluí-lo; exceções de permissão por usuário; recuperação de senha por
  e-mail, quando houver envio.
