# ADR-0013: Railway como plataforma de deploy

- **Status**: Aceito
- **Data**: 2026-10-20
- **Substitui**: ADR-0004 (Render como hospedagem do backend)

## Contexto

O projeto atingiu maturidade suficiente para um deploy público de portfólio:
11 migrações Flyway, 10 controllers REST, trilha de auditoria imutável,
módulo de suprimentos e estoque, e frontend com 14 páginas. A decisão anterior
(ADR-0004) apontava para o Render, mas o plano gratuito do Render hiberna após
inatividade (cold start visível para recrutadores) e exige configuração separada
para cada serviço. O Railway Hobby ($5/mês, com $5 de crédito incluso) oferece
deploy de múltiplos serviços em um único projeto, sem cold start e com PostgreSQL,
Redis e RabbitMQ como plugins nativos.

O deploy ainda **não foi executado**: a decisão foi tomada antes de criar os
artefatos de containerização. O objetivo é subir uma versão bem estruturada e
visualmente polida, não um deploy emergencial.

## Decisão

Hospedar o projeto completo no **Railway** (plano Hobby) com os seguintes serviços:

| Serviço | Tipo |
|---|---|
| PostgreSQL | Plugin nativo Railway |
| Redis | Plugin nativo Railway |
| RabbitMQ | Plugin nativo Railway (adiado — ver abaixo) |
| Backend (Spring Boot) | Dockerfile multistage (Maven + JRE 21) |
| Frontend (React/Vite) | Dockerfile com Nginx (build Vite + proxy reverso) |

O **RabbitMQ** será adicionado somente quando o módulo de mensageria estiver
ativo no código. Subir o broker sem consumidores inflaria o custo sem benefício.

## Pré-requisitos antes do deploy

Os seguintes artefatos precisam ser criados na ordem:

1. `backend/Dockerfile` — build multistage: fase Maven gera o JAR, fase JRE 21
   executa. Expõe a porta 8080.
2. `frontend/Dockerfile` — fase Node/Vite gera o `dist/`, fase Nginx serve os
   estáticos e faz proxy reverso de `/api` para a URL do backend no Railway.
3. `frontend/nginx.conf` — configuração do Nginx com `try_files` para SPA e
   `proxy_pass` para o backend.
4. Variável `VITE_API_URL` no build do frontend (ou manter o proxy Nginx —
   decidir na implementação).
5. Variáveis de ambiente no Railway:
   - `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` — injetadas
     automaticamente pelo plugin PostgreSQL.
   - `JWT_SECRET` — segredo forte (≥ 32 chars), gerado na plataforma.
   - `ADMIN_NOME`, `ADMIN_EMAIL`, `ADMIN_SENHA` — primeiro usuário GERENTE;
     nunca versionados no Git.
   - `SPRING_PROFILES_ACTIVE=prod`
   - `REDIS_URL` — injetada automaticamente pelo plugin Redis.
6. Perfil `prod` no Spring Boot (`application-prod.yml`) desabilitando
   configurações de dev (ex.: `show-sql: false`, pool de conexões ajustado).
7. Health check configurado no `railway.toml` apontando para
   `GET /actuator/health`.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Render (ADR-0004) | Cold start no plano gratuito prejudica a experiência do recrutador; configuração de múltiplos serviços mais fragmentada |
| Vercel (frontend) + Railway (backend) | Dividir em duas plataformas adiciona complexidade de CORS e domínios; Railway suporta ambos |
| Azure | Melhor alinhamento com perfil SAP/enterprise, mas complexidade de configuração não justifica para portfólio neste momento; pode ser retomado em contexto de demonstração corporativa |
| Fly.io | Boa alternativa técnica, mas Railway tem DX superior e plugins mais simples para Postgres/Redis |

## Consequências

- **Positivas**: sem cold start; deploy automático a cada push na `main`; todos
  os serviços (DB, cache, broker, app) no mesmo projeto Railway com variáveis
  compartilhadas; custo previsível (~$3–5/mês com tráfego de portfólio).
- **Negativas / riscos**: custo recorrente (mínimo $5/mês) — aceitável após
  upgrade do plano. O deploy deve acontecer somente após a interface estar
  visualmente polida (ver roadmap de qualidade visual, PR #41), para que a
  primeira impressão do portfólio seja positiva.