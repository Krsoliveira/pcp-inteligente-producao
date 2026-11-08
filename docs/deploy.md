# Deploy

Como o PCP roda no ar e como reproduzir isso. A decisão e os motivos estão no [ADR-0033](adr/0033-deploy-railway-pro-com-nginx.md).

```
navegador ──HTTPS──> borda do Railway ──> frontend (Nginx) ──/api──> backend (Spring Boot) ──> Postgres
                     pcp.krsoliveira.com.br     │     rede privada: backend.railway.internal:8080
                                                └─ demais rotas: a aplicação React (index.html)
```

Só o `frontend` tem endereço público. O `backend` e o `Postgres` ficam na rede privada do projeto.

## 1. Ensaio local (antes de qualquer mudança de deploy)

As mesmas imagens, com o perfil `prod`, em `http://localhost:8088`. Precisa do Docker Desktop aberto. Os segredos ficam só na sessão do terminal (PowerShell):

```powershell
$env:JWT_SECRET = -join ((48..57)+(65..90)+(97..122) | Get-Random -Count 48 | ForEach-Object {[char]$_})
$env:ADMIN_SENHA = "<uma senha de teste>"
docker compose -f docker-compose.prod.yml up -d --build
# login: admin@pcp.local com a senha acima
docker compose -f docker-compose.prod.yml down -v
```

O que conferir:

- `/saude` responde `ok`, e recarregar `/ordens` abre a tela;
- o login funciona;
- o console do navegador fica sem erro de CSP;
- seis logins errados seguidos dão `429` na sexta, mesmo variando o cabeçalho `X-Forwarded-For`.

Para varrer as imagens com a régua do CI (HIGH/CRITICAL com correção):

```bash
docker run --rm -v //var/run/docker.sock:/var/run/docker.sock aquasec/trivy image --severity HIGH,CRITICAL --ignore-unfixed pcp-prod-local-frontend
```

Repita para `pcp-prod-local-backend`. O resultado esperado é zero.

## 2. Railway: criar o projeto (uma vez)

No painel do Railway, crie um projeto (ex.: `pcp-inteligente-producao`) com três serviços. **Os nomes importam**: o frontend procura a API em `backend.railway.internal`, e as referências usam `Postgres`.

### Postgres

Em **Create → Database → PostgreSQL**, mantenha o nome `Postgres`. Depois:

- Na aba **Backups** do volume, ligue o **backup agendado** (diário e semanal).
- Ligue também a **recuperação a um instante (PITR)**, se o painel oferecer.

### backend

**Create → GitHub Repo →** este repositório. Em **Settings**:

| Ajuste | Valor |
|---|---|
| Nome do serviço | `backend` |
| Root Directory | `/backend` (o Railway usa o `Dockerfile` da pasta) |
| Branch | `main` |
| Wait for CI | ligado: só implanta depois que o CI do commit passar |
| Watch Paths | `/backend/**`: mudança só no frontend não reimplanta a API |
| Healthcheck Path | `/actuator/health` |
| Domínio público | **nenhum** |
| Memória | limite de 1 GB (a JVM usa até 75%) |

**Variables:**

| Variável | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `PORT` | `8080`: fixa, porque é nela que o Nginx procura a API |
| `DB_HOST` | `${{Postgres.PGHOST}}` |
| `DB_PORT` | `${{Postgres.PGPORT}}` |
| `DB_NAME` | `${{Postgres.PGDATABASE}}` |
| `DB_USER` | `${{Postgres.PGUSER}}` |
| `DB_PASSWORD` | `${{Postgres.PGPASSWORD}}` |
| `JWT_SECRET` | segredo aleatório com 48 caracteres ou mais, gerado na hora; nunca reutilize |
| `ADMIN_EMAIL`, `ADMIN_NOME`, `ADMIN_SENHA` | o primeiro usuário (perfil Administrador); guarde a senha num cofre de senhas |
| `CORS_ORIGINS` | `https://pcp.krsoliveira.com.br` |
| `PROXIES_CONFIAVEIS` | **não defina**: o padrão já cobre a rede privada do Railway (IPv4 `10.*` e IPv6 `fd..`); veja o passo 4 |

Sem `JWT_SECRET` ou `DB_PASSWORD`, o perfil `prod` não sobe, e isso é proposital. O autocadastro fica desligado (ADR-0031); não defina `AUTOCADASTRO_HABILITADO`.

**`ADMIN_*` só valem na criação do administrador.** No primeiro deploy, com o banco vazio, o backend cria a conta com esses valores. Nos deploys seguintes, se o e-mail já existe, ele não mexe na conta; o log diz "e-mail já cadastrado — nenhuma alteração feita".

- **Trocar a senha:** pela tela, em **Alterar minha senha**, ou por outro administrador, em **Redefinir senha**. A troca exige a senha atual, entra na trilha de auditoria e derruba as sessões antigas.
- **Mudar a `ADMIN_SENHA` no Railway não troca a senha de ninguém.** Se a senha do administrador vazar, troque pela tela e gere um `JWT_SECRET` novo.

### frontend

**Create → GitHub Repo →** o mesmo repositório. Em **Settings**:

| Ajuste | Valor |
|---|---|
| Nome do serviço | `frontend` |
| Root Directory | `/frontend` |
| Branch | `main` |
| Wait for CI | ligado |
| Watch Paths | `/frontend/**` |
| Healthcheck Path | `/saude` |
| Domínio público | **Generate Domain** (para o primeiro teste) e o domínio próprio do passo 3 |

**Variables:**

| Variável | Valor |
|---|---|
| `BACKEND_ENDERECO` | `${{backend.RAILWAY_PRIVATE_DOMAIN}}:8080`; com ela, o frontend também espera o backend subir |
| `BORDA_CONFIAVEL` | `100.64.0.0/10`: a faixa interna da borda do Railway, medida (passo 4) |
| `CABECALHO_DO_CLIENTE` | `X-Real-IP`: o cabeçalho em que a borda põe o IP do cliente |

O `PORT` do frontend é o Railway quem informa, e o Nginx escuta nele.

## 3. Domínio `pcp.krsoliveira.com.br`

1. No serviço `frontend`, abra **Settings → Networking → Custom Domain** e informe `pcp.krsoliveira.com.br`. O Railway mostra o registro DNS a criar: um `CNAME` e, às vezes, um `TXT` de verificação.
2. No [Registro.br](https://registro.br), abra o domínio, entre em **DNS** e use **Editar zona**. Crie exatamente o que o Railway mostrou, para o nome `pcp`.
3. Aguarde a propagação, de minutos a algumas horas. O Railway emite o certificado HTTPS sozinho.

Cada projeto seu usa um subdomínio próprio do mesmo jeito. O domínio puro (`krsoliveira.com.br`) fica livre.

## 4. Primeiro deploy: o IP do cliente

O limite de tentativas de login vale por IP. Para ele separar um cliente do outro, o Nginx precisa saber quem é o cliente, e a API precisa confiar no Nginx.

**O caminho medido em 2026-11-07:**

```
cliente → borda pública do Railway (46.151.194.x) → proxy interno (100.64.0.x) → Nginx → API
```

- **Quem conecta no Nginx:** sempre um endereço `100.64.0.0/10`.
- **`X-Forwarded-For`:** a borda descarta o valor que o cliente manda e escreve "cliente, borda pública". Como a borda pública tem mais de uma faixa e pode mudar, ela não é a fonte usada.
- **`X-Real-IP`:** a borda o preenche com o IP do cliente, conforme a documentação do Railway, e o Nginx o lê, aceitando-o só de quem vem de `100.64.0.0/10`.
- **Nginx → API:** a conexão chega ora por IPv4 privado (`10.*`), ora por IPv6 privado (`fd..`). O padrão de `PROXIES_CONFIAVEIS` cobre os dois.

| Variável | Serviço | Valor |
|---|---|---|
| `BORDA_CONFIAVEL` | frontend | `100.64.0.0/10` |
| `CABECALHO_DO_CLIENTE` | frontend | `X-Real-IP` |
| `PROXIES_CONFIAVEIS` | backend | não definir: vale o padrão do perfil `prod` |

Com faixa errada, **ninguém consegue forjar o IP**; o efeito é só que todos os clientes dividem o mesmo limite. **Nunca use `0.0.0.0/0` nem `::/0`**: isso devolve ao cliente o poder de escolher o próprio IP.

**Medir de novo**, se o Railway mudar a rede: nos logs do `frontend` (**Deploy Logs**), o primeiro campo de cada linha de acesso é o IP que o Nginx considerou como cliente. Ele deve ser o seu IP real, mesmo quando você manda um `X-Real-IP` forjado.

**Provar em produção:**

1. Do seu computador, erre a senha seis vezes seguidas: a sexta deve dar "muitas tentativas" (`429`).
2. Repita mandando um `X-Real-IP` e um `X-Forwarded-For` diferentes em cada tentativa (`curl -H "X-Real-IP: 203.0.113.N"`): a sexta deve continuar bloqueada.
3. Logo em seguida, de outra rede (o celular no 4G), tente entrar: deve funcionar.
   - Se o celular também foi bloqueado, a faixa ou o cabeçalho estão errados, e todos dividem o mesmo limite.
4. A resposta da API (`/api/...`) traz `Strict-Transport-Security`: sinal de que a API confiou no Nginx.

## 5. Atualizar e desfazer

- **Atualizar:** o merge na `main` dispara o deploy, depois do CI. O Railway só troca a versão quando o health check da nova responde. Se não responder, a anterior continua no ar.
- **Desfazer:** em **Deployments**, escolha a implantação anterior e use **Redeploy**.
  - **Atenção:** migração do Flyway já aplicada não volta com o redeploy. Desfazê-la é uma migração nova.
- **Imagens base:** gere de novo e varra com o Trivy (passo 1) ao menos uma vez por mês, porque correções de segurança chegam pela base.

## 6. Custo

O plano Pro inclui US$ 20 de uso por mês, somando todos os projetos da conta. Acompanhe em **Usage** o consumo de cada serviço no primeiro mês; o backend Java é o que mais pesa.

## Faixas em uso

| Data | `BORDA_CONFIAVEL` | `PROXIES_CONFIAVEIS` | Medido por |
|---|---|---|---|
| 2026-11-07 | `100.64.0.0/10`, com `CABECALHO_DO_CLIENTE=X-Real-IP` | padrão do perfil `prod` (inclui `10.*` e `fd..`) | logs de acesso do Nginx e fluxos de rede do backend |
