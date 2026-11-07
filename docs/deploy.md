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
| `PROXIES_CONFIAVEIS` | não defina no início; veja o passo 4 |

Sem `JWT_SECRET` ou `DB_PASSWORD`, o perfil `prod` não sobe, e isso é proposital. O autocadastro fica desligado (ADR-0031); não defina `AUTOCADASTRO_HABILITADO`.

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
| `BORDA_CONFIAVEL` | não defina no início; veja o passo 4 |

O `PORT` do frontend é o Railway quem informa, e o Nginx escuta nele.

## 3. Domínio `pcp.krsoliveira.com.br`

1. No serviço `frontend`, abra **Settings → Networking → Custom Domain** e informe `pcp.krsoliveira.com.br`. O Railway mostra o registro DNS a criar: um `CNAME` e, às vezes, um `TXT` de verificação.
2. No [Registro.br](https://registro.br), abra o domínio, entre em **DNS** e use **Editar zona**. Crie exatamente o que o Railway mostrou, para o nome `pcp`.
3. Aguarde a propagação, de minutos a algumas horas. O Railway emite o certificado HTTPS sozinho.

Cada projeto seu usa um subdomínio próprio do mesmo jeito. O domínio puro (`krsoliveira.com.br`) fica livre.

## 4. Primeiro deploy: o IP do cliente

O limite de tentativas de login vale por IP. Para ele separar um cliente do outro, duas faixas precisam estar certas:

| Variável | Serviço | O que é |
|---|---|---|
| `BORDA_CONFIAVEL` | frontend | Faixa CIDR da borda do Railway: o Nginx só aceita `X-Forwarded-For` vindo dela |
| `PROXIES_CONFIAVEIS` | backend | Expressão regular com o IP privado do Nginx: a API só aceita o cabeçalho vindo dele. O padrão cobre `10.*`, `192.168.*`, `169.254.*` e `127.*` |

Enquanto não estiverem certas, **ninguém consegue forjar o IP**. O efeito é só que todos os clientes dividem o mesmo limite.

**Medir a faixa da borda.** Abra o site e veja os logs do `frontend`, na aba **Deploy Logs**. O primeiro campo de cada linha de acesso é o endereço da borda. Com alguns acessos, a faixa fica clara.

- Defina `BORDA_CONFIAVEL` com a faixa mais estreita que cubra esses endereços, por exemplo `100.64.0.0/10` se for a faixa de CGNAT.
- **Nunca use `0.0.0.0/0` nem `::/0`**: isso devolve ao cliente o poder de escolher o próprio IP.

**Conferir o IP privado do Nginx.** Se ele não estiver na faixa padrão do backend, ajuste `PROXIES_CONFIAVEIS`. Um endereço IPv6 privado, por exemplo, pede algo como `fd[0-9a-f]{2}:.*`.

**Provar em produção:**

1. Do seu computador, erre a senha seis vezes seguidas: a sexta deve dar "muitas tentativas" (`429`).
2. Logo em seguida, de outra rede (o celular no 4G), tente entrar: deve funcionar.
   - Se o celular também foi bloqueado, uma das faixas ainda está errada, e todos dividem o mesmo limite.
3. Repita o passo 1 mandando um `X-Forwarded-For` diferente em cada tentativa (`curl -H "X-Forwarded-For: 203.0.113.N"`). A sexta deve continuar bloqueada.

Depois de medidas, registre as faixas usadas na seção "Faixas em uso" no fim deste guia.

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
| (preencher no primeiro deploy) | | | |
