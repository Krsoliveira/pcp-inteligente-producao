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
- Mensageria: RabbitMQ ou Kafka

### Frontend (`frontend/`)
- React + TypeScript
- Material UI
- React Query
- Zustand
- ECharts

### IA
- OpenAI ou Ollama
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
- Backend: Railway / Render / Azure
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
├── backend/        # Java + Spring Boot
├── frontend/        # React + TypeScript
├── docs/            # Documentação, decisões de arquitetura
└── data/            # Datasets e scripts de carga
```
