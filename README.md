# Plataforma Inteligente de PCP e Produção

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

## Estrutura

```
01-pcp-inteligente-producao/
├── backend/        # Java + Spring Boot
├── frontend/        # React + TypeScript
├── docs/            # Documentação, decisões de arquitetura
└── data/            # Datasets e scripts de carga
```
