# ADR-0008 — Estratégia de banco de dados: ambiente local e provedor de produção

**Status:** Aceito  
**Data:** 2026-08-23  
**Fase:** Infraestrutura — preparação para deploy das Fases 5a/5b e além

---

## Contexto

O ADR-0004 decidiu hospedar o backend no Render e mencionou "PostgreSQL em serviço
gerenciado" sem especificar o provedor. Com o crescimento do modelo de domínio
(ADR-0007 — Material, ListaTecnica, ConsumoMaterial, Lote) e as migrações Flyway
previstas (V4–V7), este ADR formaliza:

1. **Qual banco de dados** será usado (SGBD).
2. **Como o ambiente de desenvolvimento local** se conecta ao banco.
3. **Qual provedor gerenciado** hospedará o banco em produção.

Restrições aplicáveis:

- Custo próximo de zero (portfólio, sem receita).
- Sem instalação de software no SO do desenvolvedor — o ambiente já usa Docker.
- Compatibilidade com Testcontainers (já usado nos testes de integração).
- Suporte a migrações Flyway com controle de versão estrito.
- Integração simples com o Render via variável de ambiente `DATABASE_URL`.

---

## Decisão

### 1. SGBD: PostgreSQL 16

PostgreSQL será o único banco de dados do projeto — sem SQL Server, sem H2 em memória.

**Motivo:** stack Java/Spring Boot open-source usa PostgreSQL como padrão de mercado;
SQL Server exige licença e não tem plano gratuito viável em nuvem; H2 em memória foi
descartado pelos testes de integração (Testcontainers já resolve o isolamento sem
fingir ser outro banco).

### 2. Ambiente de desenvolvimento local: Docker Compose

Um `docker-compose.yml` na raiz do projeto sobe o PostgreSQL localmente. Desenvolvedores
não instalam nenhum cliente PostgreSQL no SO — apenas Docker.

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: pcp_dev
      POSTGRES_USER: pcp
      POSTGRES_PASSWORD: pcp
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

A aplicação lê a conexão via variável de ambiente:

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/pcp_dev
SPRING_DATASOURCE_USERNAME=pcp
SPRING_DATASOURCE_PASSWORD=pcp
```

Os testes de integração continuam usando **Testcontainers** — não dependem do compose
e rodam de forma isolada em CI (cada teste sobe e derruba seu próprio container).

### 3. Provedor de produção: Neon (PostgreSQL serverless)

O banco de produção será hospedado no **[Neon](https://neon.tech)** — PostgreSQL
serverless no plano gratuito.

Integração com o Render: a variável de ambiente `DATABASE_URL` é configurada no painel
do Render apontando para a string de conexão do Neon. O Flyway executa as migrações
automaticamente no startup do container.

---

## Alternativas consideradas

| Alternativa | Por que descartada |
|---|---|
| **SQL Server (local ou Azure)** | Licença paga; imagem Docker pesada; incomum no ecossistema Java/Spring Boot open-source; Render não hospeda SQL Server no plano gratuito |
| **H2 em memória** | Dialeto diferente de PostgreSQL; falhas de compatibilidade mascaram bugs de migração; já descartado pelo uso de Testcontainers |
| **Render PostgreSQL (gerenciado)** | Banco gratuito **expira em 90 dias** e é deletado automaticamente — inaceitável para um portfólio que precisa ter dados reais disponíveis a qualquer momento |
| **Supabase** | Plano gratuito tem 500 MB e pausa após 1 semana de inatividade; o dashboard embutido (REST, auth) adiciona superfície que não pertence ao portfólio e pode confundir o avaliador técnico sobre o que foi construído |
| **Railway PostgreSQL** | Sem plano gratuito permanente — custo recorrente inaceitável para um portfólio |
| **ElephantSQL** | Limite de 20 MB no plano gratuito; incompatível com o volume de dados dos datasets sintéticos previstos |
| **PostgreSQL instalado direto no SO** | Viola o princípio de ambiente reproduzível; versão diverge entre máquinas; descartado em favor de Docker Compose |

---

## Consequências

### Positivas

- **Ambiente 100% reproduzível**: qualquer colaborador clona o repositório, roda
  `docker compose up -d` e já tem banco local com schema atualizado pelo Flyway.
- **Sem divergência de dialeto**: o mesmo PostgreSQL 16 roda em dev, em CI
  (Testcontainers) e em produção (Neon) — o que passa localmente passa em prod.
- **Neon não expira**: banco de produção permanece disponível indefinidamente no plano
  gratuito, com 512 MB de armazenamento — suficiente para todos os datasets previstos.
- **Database branching (Neon)**: recurso que permite criar um branch isolado do banco
  para testar migrações das Fases 5a/5b sem afetar o schema de produção.
- **Integração direta com Render**: `DATABASE_URL` única é suficiente; sem configuração
  extra de rede ou SSL.

### Negativas / riscos

- **Cold start do Neon**: o banco serverless pode ter latência na primeira query após
  período de inatividade. Aceitável para demonstração em portfólio; mitigado pelo
  cold start já existente no Render (ADR-0004).
- **Docker obrigatório em dev**: quem não tiver Docker instalado não consegue rodar
  localmente — risco nulo, pois o projeto já usa Testcontainers e o Dockerfile para
  build.
- **Flyway em produção no startup**: se uma migração falhar, o container não sobe.
  Mitigação: testar toda migração nova localmente e em CI antes do merge.

### Impacto no repositório

| Arquivo | Ação |
|---|---|
| `docker-compose.yml` | Criar na raiz do projeto |
| `.env.example` | Criar com as variáveis de conexão local (sem valores reais) |
| `.gitignore` | Garantir que `.env` local não seja commitado |
| `README.md` | Adicionar seção "Como rodar localmente" com instruções do Docker Compose |
| Render (painel) | Configurar `DATABASE_URL` apontando para Neon |