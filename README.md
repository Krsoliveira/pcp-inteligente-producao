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

Datasets reais do Kaggle:
- Manufacturing Dataset
- Supply Chain Dataset
- Production Planning Dataset

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
├── data/                # Datasets e scripts de carga
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

```bash
# 1. Suba a infraestrutura (PostgreSQL, Redis, RabbitMQ)
cp .env.example .env   # ajuste as senhas
docker compose up -d

# 2. Backend (requer JDK 21 e Maven; Docker ligado para os testes de integração)
cd backend
mvn verify              # compila e roda todos os testes
mvn spring-boot:run     # sobe a API em http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html

# 3. Frontend: instruções serão adicionadas na Fase 3
```
