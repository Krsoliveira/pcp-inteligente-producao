# Próximos passos

Registro do estado do trabalho para retomar sem perder contexto.
Atualize este arquivo ao encerrar cada sessão de trabalho.

**Última atualização:** 2026-09-26  
**Estado:** Fases 5a e 5b concluídas e publicadas na `main` (PRs #27 e #28); `main` e `develop` sincronizadas.

## Concluído nesta etapa

1. **Correção da migração V7** — `TRUNCATE TABLE ordem_producao CASCADE`. Sem o
   `CASCADE`, a aplicação não subia em nenhum banco novo (a V6 já cria FKs para
   `ordem_producao`).
2. **Dataset sintético** ([ADR-0009](adr/0009-dataset-sintetico-modelo-expandido.md)) —
   gerador Python em `data/scripts/` + novo `DataLoader` (perfil `seed`): 743 ordens,
   consumos com desvios justificados e lotes; datas ancoradas no dia da carga.
3. **Documentação e CI** — ADR-0009, índice de ADRs completo, roteiro com as Fases 5a e
   5b concluídas; o CI passou a rodar também em `develop` e verifica se os CSVs do
   dataset estão em dia.
4. **Tela de cadastro** (`/cadastro`) — o autocadastro cria sempre o perfil
   `PLANEJADOR`, e o backend garante essa regra (um `perfil` enviado é ignorado).
5. **Administrador inicial** — a API cria um usuário `GERENTE` na inicialização a partir
   de `ADMIN_NOME`, `ADMIN_EMAIL` e `ADMIN_SENHA` (variáveis de ambiente, fora do Git).
   Idempotente e nunca promove conta existente. Ver README, seção 5.
6. **Correção da tela de Listas Técnicas** — ela deslogava o usuário: a API exigia
   `materialId` e o erro 400 virava um falso 401. Agora `materialId` é opcional e a rota
   interna `/error` é pública (erros voltam com o status real).
7. **Entrada de material** ([ADR-0010](adr/0010-entrada-de-material-por-compra.md)) —
   recebimento de matéria-prima com fornecedor e nota fiscal obrigatórios gera lote
   rastreável (`POST /api/v1/lotes/entradas`, tela Lotes → Entrada de Material).
8. **README de apresentação** — telas reais, diagramas Mermaid, destaques técnicos e
   roadmap; a stack lista só o que está implementado.
9. **Publicação** — #27 (`feature/dataset-sintetico` → `develop`) e #28 (`develop` → `main`)
   mesclados com CI verde; PRs do Dependabot organizados (Actions do CI atualizadas,
   versões principais fechadas com justificativa).

## Pendências

- [ ] Apagar as branches já mescladas `claude/laughing-cannon-a62pda` e
      `feature/dataset-sintetico` no GitHub.
- [ ] Preencher descrição e tópicos do repositório (⚙️ ao lado de **About**) e fixá-lo
      no perfil (**Customize your pins**).
- [ ] Testar localmente:
  ```powershell
  # com o Docker Desktop aberto, na raiz do projeto
  docker compose down -v
  docker compose up -d
  cd backend
  mvn spring-boot:run "-Dspring-boot.run.profiles=dev,seed"
  ```

## Próximos passos sugeridos

1. **Fase 5c — módulo de IA**: previsão de demanda, análise de atrasos e
   recomendações, usando o histórico do dataset sintético.
2. **Deploy** — adiado por decisão: será feito em planos pagos (provedor a definir).
   Ver a seção [Deploy: preparação pendente](#deploy-preparação-pendente).
3. **Migrações de versão principal** (fechadas no Dependabot, a fazer em PRs próprios):
   Spring Boot 4 + springdoc 3, React 19, React Router 7, Vite 8 + plugin-react 6,
   ECharts 6.
4. Depois:
   - tela/endpoint para um `GERENTE` promover outros usuários;
   - **saldo de estoque**: baixar dos lotes de matéria-prima o consumo das ordens;
   - testes automatizados de frontend (Vitest);
   - carregar o ECharts sob demanda (bundle de ~1 MB).

## Deploy: preparação pendente

Lacunas identificadas no código, **independentes do provedor** escolhido:

| Lacuna | Impacto | O que fazer |
|---|---|---|
| Sem `Dockerfile` no backend | Plataformas de container não conseguem rodar a API | `Dockerfile` multi-stage (build Maven + JRE 21 enxuto, usuário não-root) |
| Frontend com `baseURL` fixo em `/api` | Com frontend e API em domínios diferentes, o frontend não encontra a API | `client.ts` passar a ler `VITE_API_BASE_URL` (já documentada em `frontend/.env.example`, mas não usada) |
| Sem CORS no backend | O navegador bloqueia chamadas do domínio do frontend para o da API | Configurar CORS no Spring Security com origens vindas de variável de ambiente |
| Sem regra de rotas para SPA | Recarregar `/ordens`, `/lotes` etc. dá 404 no host estático | Rewrite de todas as rotas para `index.html` (ex.: `vercel.json`) |
| Sem perfil de produção | Banco gerenciado exige SSL; `JWT_SECRET` tem valor padrão de dev | Perfil `prod`: conexão com `sslmode=require` e `JWT_SECRET` obrigatório (sem padrão) |

Depois do código pronto: arquivo de configuração do provedor escolhido, guia
`docs/deploy.md`, variáveis de ambiente (banco, `JWT_SECRET`, `ADMIN_*`, origens do CORS)
e link "ver online" no README.

Pontos de atenção por provedor:

- **Vercel** hospeda só o **frontend** — não roda o backend Spring Boot (processo Java
  contínuo).
- **Supabase** (banco): é PostgreSQL padrão, compatível com Flyway/JPA. Usar a conexão
  **direta** ou o pooler em modo **session** (porta 5432); o modo *transaction* (porta
  6543) quebra prepared statements do JDBC. A conexão direta é IPv6 — em hosts só IPv4,
  usar o pooler *session* ou o add-on de IPv4.
- **Render** (plano gratuito) hiberna após inatividade; em plano pago, não.

## Observações

- Commits sem linhas de assinatura/coautoria até segunda ordem.
- O projeto não tem `mvnw`: use `mvn`.
- A API fica em `/api/v1`; Swagger em <http://localhost:8080/swagger-ui.html>.
