# Próximos passos

Registro do estado do trabalho para retomar sem perder contexto.
Atualize este arquivo ao encerrar cada sessão de trabalho.

**Última atualização:** 2026-09-26  
**Branch de trabalho:** `feature/dataset-sintetico` (criada a partir de `develop`)

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

## Pendências

- [ ] Apagar a branch antiga `claude/laughing-cannon-a62pda` no GitHub.
- [ ] Testar localmente:
  ```powershell
  # com o Docker Desktop aberto, na raiz do projeto
  docker compose down -v
  docker compose up -d
  cd backend
  mvn spring-boot:run "-Dspring-boot.run.profiles=dev,seed"
  ```

## Próximos passos sugeridos

1. Abrir PR de `feature/dataset-sintetico` para `develop`, para o CI rodar os testes de
   integração (Testcontainers).
2. Decidir sobre os PRs do Dependabot (Spring Boot 4, Vite 8, React Router 7 exigem
   avaliação) e sincronizar `main` com `develop`.
3. **Fase 5c — módulo de IA**: previsão de demanda, análise de atrasos e
   recomendações, usando o histórico do dataset sintético.
4. Depois:
   - tela/endpoint para um `GERENTE` promover outros usuários;
   - **saldo de estoque**: baixar dos lotes de matéria-prima o consumo das ordens;
   - testes automatizados de frontend (Vitest);
   - carregar o ECharts sob demanda (bundle de ~1 MB).

## Observações

- Commits sem linhas de assinatura/coautoria até segunda ordem.
- O projeto não tem `mvnw`: use `mvn`.
- A API fica em `/api/v1`; Swagger em <http://localhost:8080/swagger-ui.html>.
