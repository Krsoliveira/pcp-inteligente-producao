# Plataforma Inteligente de PCP e Produção

[![CI](https://github.com/Krsoliveira/pcp-inteligente-producao/actions/workflows/ci.yml/badge.svg)](https://github.com/Krsoliveira/pcp-inteligente-producao/actions/workflows/ci.yml)
<!-- Badge assume que o repo no GitHub se chamará "pcp-inteligente-producao"; ajuste a URL se o nome final for outro. -->

Projeto "estrela" do portfólio. Dashboard de planejamento e controle de produção com IA, unindo os diferenciais raros do autor: Engenharia Civil, PCP, Auditoria, TI e interesse em SAP.

## Problema

Dashboard para planejamento da produção com apoio de IA: previsão de demanda, análise de atrasos e recomendações acionáveis para o time de PCP.

## Stack

### Backend (`backend/`)
- Java + Spring Boot
- Clean Architecture / DDD
- JWT + OAuth2
- Swagger/OpenAPI
- JPA/Hibernate + PostgreSQL
- Redis (cache)
- Mensageria: RabbitMQ ([ADR-0002](docs/adr/0002-rabbitmq-mensageria.md))

### Frontend (`frontend/`)
- React + TypeScript
- Material UI
- React Query
- Zustand
- ECharts

### IA
- OpenAI, atrás de camada de abstração ([ADR-0003](docs/adr/0003-openai-provedor-ia.md))
- Previsão de demanda
- Análise de atrasos
- Geração de recomendações

## Dados (`data/`)

Dataset **sintético** e determinístico de um fabricante de peças mecânicas — materiais,
listas técnicas (BOM) versionadas, ~740 ordens, consumos com desvios justificados e lotes
([ADR-0009](docs/adr/0009-dataset-sintetico-modelo-expandido.md)). Nenhum dataset público
cobre BOM + consumo + lote com coerência referencial. Detalhes em [`data/`](data/README.md).

## Diferencial

Integrações simuladas com SAP, Power BI e APIs externas.

## Testes

- Backend: JUnit, Mockito, Testcontainers
- Frontend: Vitest, Cypress

## Deploy

- Frontend: Vercel
- Backend: Render ([ADR-0004](docs/adr/0004-render-hospedagem-backend.md))
- Banco: PostgreSQL Cloud

## DevSecOps

Pipeline de CI em `.github/workflows/ci.yml`, ativado automaticamente ao dar push para o GitHub:

- **SAST** — Semgrep (backend + frontend) e CodeQL (Java, JavaScript)
- **Dependency scanning** — OWASP Dependency-Check (Maven) + `npm audit`, com Dependabot atualizando as duas árvores de dependências semanalmente
- **Secret scanning** — Gitleaks em todo o histórico
- **Container scanning** — Trivy (filesystem scan)

## Estrutura

```
01-pcp-inteligente-producao/
├── backend/             # Java + Spring Boot (Clean Architecture)
├── frontend/            # React + TypeScript
├── docs/                # Documentação: visão, arquitetura, glossário e ADRs
├── data/                # Gerador do dataset sintético (CSVs em backend/src/main/resources/dados)
├── docker-compose.yml   # Infra local: PostgreSQL, Redis, RabbitMQ
└── .env.example         # Modelo de variáveis de ambiente (copiar para .env)
```

## Documentação

Toda a documentação vive em [`docs/`](docs/README.md):

- [Visão geral](docs/visao-geral.md) — problema, escopo e roteiro de fases
- [Arquitetura](docs/arquitetura.md) — camadas, diagramas e convenções
- [Glossário](docs/glossario.md) — termos de PCP e termos técnicos
- [ADRs](docs/README.md#índice-de-adrs) — registro das decisões de arquitetura

## Como rodar localmente

### Pré-requisitos

| Ferramenta | Versão mínima | Observação |
|---|---|---|
| Docker Desktop | 24+ | Necessário para a infraestrutura e para os testes de integração (Testcontainers) |
| JDK | 21 | Recomendado via [SDKMAN](https://sdkman.io/) |
| Node.js | 20 LTS | Recomendado via [nvm](https://github.com/nvm-sh/nvm) |

### 1. Variáveis de ambiente

```bash
cp .env.example .env
# Edite .env se quiser trocar senhas ou portas.
# O arquivo .env não é commitado (.gitignore).
```

### 2. Infraestrutura (PostgreSQL · Redis · RabbitMQ)

```bash
docker compose up -d
```

Aguarde todos os serviços ficarem saudáveis (healthcheck automático):

```bash
docker compose ps   # todos devem exibir "healthy"
```

| Serviço | Endereço local |
|---|---|
| PostgreSQL | `localhost:5433` (host 5433 → container 5432) |
| Redis | `localhost:6379` |
| RabbitMQ | `localhost:5672` · painel: <http://localhost:15672> (pcp / pcp_dev) |

### 3. Backend

```bash
cd backend

# Compilar e rodar todos os testes (unitários + integração via Testcontainers)
mvn verify

# Subir a API
mvn spring-boot:run

# Subir a API carregando o dataset sintético (idempotente — só carrega em banco vazio)
mvn spring-boot:run -Dspring-boot.run.profiles=dev,seed
```

| Recurso | URL |
|---|---|
| API REST | <http://localhost:8080/api/v1> |
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| Actuator (health) | <http://localhost:8080/actuator/health> |

### 4. Frontend

```bash
cd frontend
npm install
npm run dev
```

Acesse em <http://localhost:5173> e use **Criar conta** (o autocadastro cria o perfil
Planejador).

### 5. Usuário administrador (opcional)

A API cria um usuário **GERENTE** na inicialização a partir de variáveis de ambiente.
As credenciais nunca vão para o código nem para o Git. No PowerShell, antes de subir a API:

```powershell
$env:ADMIN_NOME  = "Seu Nome"
$env:ADMIN_EMAIL = "seu@email.com"
$env:ADMIN_SENHA = "uma-senha-forte"
mvn spring-boot:run "-Dspring-boot.run.profiles=dev,seed"
```

É idempotente: se o e-mail já existir, nada muda (uma conta existente nunca é promovida).
Em produção (Render), defina as mesmas variáveis no painel do serviço.

A variável `VITE_API_BASE_URL` em `frontend/.env.example` pode ser deixada vazia em
desenvolvimento — o Vite usa proxy automático para `localhost:8080`.
