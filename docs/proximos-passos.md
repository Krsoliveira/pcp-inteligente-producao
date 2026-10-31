# Próximos passos

Registro do estado do trabalho para retomar sem perder contexto.
Atualize este arquivo ao encerrar cada sessão de trabalho.

**Última atualização:** 2026-10-03  
**Estado:** rastreabilidade ([ADR-0011](adr/0011-rastreabilidade-e-auditoria.md)) concluída e
mesclada; reestruturação de suprimentos ([ADR-0012](adr/0012-suprimentos-nota-fiscal-estoque.md))
concluída na branch `feature/suprimentos-estoque`.

## Concluído: suprimentos (ADR-0012)

- [x] Código do material com 9 dígitos, gerado na faixa do tipo (103 / 105 / 110), exibido
      como `103.000.001`; cadastro sem campo de código.
- [x] Entrada de nota fiscal com vários itens (`/notas-fiscais`): cada item vira um lote
      com o lote do fornecedor; nota e lote duplicados são recusados (409).
- [x] Estoque por material (`/estoque`) calculado a partir dos lotes, com os lotes de cada
      material (`/estoque/:materialId`). A lista geral de lotes foi substituída.
- [x] Lote de produção `AAMMDD` + sequência do dia (`2610030001`).
- [x] Menu: Cadastros · Suprimentos (Entrada de notas, Estoque) · Produção (Ordens).
- [x] Migração V11: renumera códigos e lotes antigos, cria as notas dos lotes de compra
      existentes e registra cada troca na trilha (`sistema:migracao-v11`).
- [x] Dataset: 170 notas (algumas com vários itens) e lotes do fornecedor.
- [ ] Estoque: exportar CSV e filtro por "abaixo do mínimo" (exige estoque mínimo no material).

## Em andamento: rastreabilidade (ADR-0011)

Objetivo: responder, para qualquer registro, *quem fez, quando, o que mudou* e *de onde veio*.

- [x] **Entrega 1 — base no backend**
  - Assinatura (criado por/em, alterado por/em) em material, lista técnica, tipo de ordem,
    ordem, consumo e lote; o usuário vem do login.
  - Trilha de auditoria imutável (`evento_auditoria`, trigger bloqueia UPDATE/DELETE/TRUNCATE)
    e consulta paginada `GET /api/v1/auditoria/eventos` (entidade, registro, usuário, ação,
    período por dia).
  - Casos de uso transacionais (porta `Transacao`): ação e evento gravados juntos.
  - Entrada de material exige emissão da NF e recebimento.
  - Migração V9 assina os registros antigos como `sistema:migracao-v9`; a carga inicial
    gera o histórico simulado (~5 mil eventos).
  - Frontend: datas da NF no diálogo de entrada; campo "Responsável" removido do consumo.
- [x] **Entrega 2 — detalhe do lote** (branch `feature/detalhe-lote`)
  - Tela `/lotes/:id`: identificação, origem (compra: fornecedor, NF, emissão,
    recebimento e quem deu entrada; produção: ordem, lista técnica e quem gerou),
    materiais consumidos pela ordem de origem e histórico de auditoria.
  - Lista de lotes: linha abre o detalhe, coluna "Registrado" (data/hora e usuário) e
    filtro por período de registro.
  - Componente `HistoricoAuditoria` reutilizável, já usado também no detalhe da ordem.
- [x] **Entrega 3 — genealogia** (branch `feature/genealogia-lotes`)
  - Consumo alocado a lotes (FEFO sugerido na tela), saldo do lote, lote zerado vira
    CONSUMIDO; bloqueio de linha contra consumos simultâneos (V10).
  - Detalhe do lote: saldo, **lotes de origem** de cada material consumido (até a NF) e
    **onde foi usado** (ordens e lotes gerados). `GET /lotes/{id}/rastreabilidade`.
  - Dataset: simulação cronológica do estoque com compras de matéria-prima, ordens de
    reposição de semiacabado e ~2 mil alocações.
- [ ] **Estorno de consumo**: o registro passou a ser único; corrigir um consumo exigirá
      estornar as alocações (devolvendo saldo) com evento próprio na trilha.
- [ ] Aba Histórico nos detalhes de material e lista técnica (reutilizar `HistoricoAuditoria`).

## Backlog: perfis e segregação de funções

Hoje existem só `PLANEJADOR` (autocadastro) e `GERENTE` (administrador inicial), e nenhuma
rota restringe acesso por perfil — a trilha de auditoria fica visível para todos por
decisão. Proposta para quando for implementado:

- Perfis padrão: **Gerente, Analista, Assistente e Auxiliar**.
- Matriz de permissões por perfil (quem cadastra, quem libera/conclui ordem, quem dá
  entrada de material, quem consulta a auditoria), aplicada no backend.
- Tela para o Gerente atribuir perfis (substitui o item "promover usuários").

## Futuro: etapa de qualidade após a produção

Hoje o lote nasce `DISPONIVEL` ao concluir a ordem e já pode ser consumido. A proposta é
incluir uma **inspeção de qualidade** entre a produção e o estoque:

- O lote concluído nasce **em inspeção** (quarentena): aparece no estoque como
  indisponível e não entra na sugestão FEFO.
- **Plano de inspeção por material**: características a verificar (dimensão, dureza,
  aspecto visual…), com valor nominal, tolerância e tamanho de amostra.
- **Laudo de inspeção**: medições, quem inspecionou e quando. O resultado **aprova**
  (lote fica disponível), **reprova** (retrabalho ou sucata, com motivo) ou **aprova
  parcialmente** (separa a quantidade reprovada).
- Trilha de auditoria para inspeções e decisões. O laudo fica visível no detalhe do lote
  e entra na genealogia (o recall mostra também o laudo).
- Indicadores: taxa de aprovação na primeira inspeção, refugo por material, por centro
  de trabalho e por fornecedor.
- Extensão natural: **inspeção de recebimento** para itens da nota fiscal (matéria-prima
  em quarentena até aprovação).
- Depende da etapa de perfis: inspetor e aprovador são papéis próprios.

## Próximo passo de interface: visual com sombras e brilho (neon)

Tornar a interface mais marcante, mantendo a legibilidade:

- **Tema escuro com destaques neon** (ciano, magenta, verde-limão) como opção, mantendo o
  tema claro. Alternância salva por usuário.
- Sombras em camadas e **brilho** (`box-shadow` com glow) em cards, botões principais,
  item ativo do menu e KPIs. Bordas finas luminosas e gradientes discretos.
- Cores de status (disponível, vencendo, bloqueado) com glow suave, sem depender só da
  cor (ícone ou texto junto).
- Centralizado em **tokens do tema MUI** (`theme.ts`: paleta, sombras, raios), sem estilos
  avulsos por tela. Os componentes atuais herdam o novo visual.
- Acessibilidade: contraste WCAG AA nos dois temas, foco visível e animações desligadas
  com `prefers-reduced-motion`.
- Atualizar as imagens do README depois da mudança.

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

_Controle de acesso por perfis de permissões (perfis, usuários, regra "só se concede o que se tem", rastreio e
telas): concluído, ver ADR-0020._

_Troca de senha obrigatória no primeiro acesso, "Alterar minha senha" e redefinição pelo gestor: concluído, ver
ADR-0021._

- [ ] **Acessos, o que sobrou**: desativar um usuário sem excluí-lo; exceções de permissão por usuário e vários
      perfis por usuário, se o uso pedir.
- [ ] **Listas técnicas autossuficientes**: as respostas trazerem `materialCodigo` e `materialDescricao` para que
      `LISTA_VISUALIZAR_*` funcione sem `MATERIAL_VISUALIZAR` (contrato só cresce).
- [ ] **Dashboard acionável, fase 3** (ADR-0022; as fases 1 e 2 estão entregues): painel "precisa de atenção"
      (ordens mais atrasadas, lotes perto do vencimento, consumos com desvio). Os cartões de indicador do Estoque
      ainda usam ícones coloridos.
- [x] **Regra do produto acabado (ADR-0024)** e **item adicionado ao consumo (ADR-0025, V16)**: entregues no
      backend (endpoint, permissão `CONSUMO_ADICIONAR_ITEM`, auditoria). Falta a tela (abaixo).
- [ ] **Dívida**: o `PATCH` de registro de consumo não confere se a ordem está em produção (o item adicionado confere).
- [x] **Tela de lançamento de consumo por ordem** (ADR-0026): entregue, com "Adicionar item fora da lista" e lançamento
      tudo ou nada.
- [x] **Quantidade base da lista técnica** (ADR-0027, V17): entregue; padrão 1, comparação por unidade produzida.
- [x] **Etiqueta do lote com QR code** (ADR-0028): entregue; QR com o link do lote, 100 × 50 mm, login volta para a página pedida. Pendente: várias etiquetas de uma vez e leitura do QR dentro do sistema.
- [ ] **Prévia do consumo planejado** ao escolher a versão da lista na criação da ordem (hoje só se vê depois de criada).
- [ ] **Relatórios, próximos passos**: exportar o consumo de ordens (CSV/Excel) e outros relatórios na mesma área.
- [ ] **Autorização antes da validação** (opcional): hoje um corpo inválido de quem não tem permissão devolve
      400, não 403. Para inverter, a regra teria de ficar no filtro de segurança por caminho em vez de na
      anotação do método.

### Para 2026-10-22 (feedback do teste manual de 21/10)

- [ ] **Redesenhar os filtros das listagens** (Ordens, Materiais, Listas Técnicas, Notas Fiscais,
      Estoque). O desenho atual ficou pouco intuitivo: os campos parecem filtros das colunas da
      tabela. A direção pedida:
      1. **Primeiro filtro (vai ao banco):** poucos critérios simples e óbvios, só para **reduzir a
         quantidade de dados** buscados (ex.: período, status, tipo). Definir, tela a tela, quais.
      2. **Segundo filtro (só dentro da página já carregada):** em vez de um campo de texto livre
         sobre as colunas, oferecer ao usuário **classificar** (ordenar) os dados da página, por
         exemplo clicando no cabeçalho da coluna.
      Pontos a decidir: critérios de cada tela; se a classificação vale só para a página ou também
      passa a ser feita no servidor; o que fazer com o subfiltro de texto atual (`SubfiltroTabela`).
- [ ] **Atualizar o README do projeto** (telas e fluxo de consulta em duas etapas, novos
      endpoints `/pesquisa` e `/resumo`, testes do frontend com Vitest, ADRs 0014 a 0017,
      estado do deploy).
- [x] Notas Fiscais e Estoque mostravam "Não foi possível carregar...": era o backend de
      desenvolvimento rodando sem os endpoints novos (iniciado antes do commit). Após reiniciar
      (`mvn spring-boot:run "-Dspring-boot.run.profiles=dev,seed"`), os endpoints respondem 200.
      Conferir de novo na tela.

### Gerais

_Testes do frontend (Vitest) e correção do script `lint`: concluídos, ver ADR-0017._
_Filtro e ordenação por coluna no cabeçalho (estilo planilha) nas 5 listagens e período do início planejado em Ordens: concluídos, ver ADR-0018._

- [ ] **Filtros das listagens, o que sobrou** (decisão de 2026-10-22): decidir se Materiais, Ordens e
      Estoque também tiram a busca por texto do primeiro filtro (hoje é o único meio de achar um item
      entre várias páginas) e se vale ordenar o resultado inteiro no servidor, não só a página.
- [ ] **README**: atualizar com as telas, a consulta em duas etapas, os endpoints `/pesquisa` e
      `/resumo`, o Vitest, os ADRs 0014–0018 e o estado do deploy.
- [ ] Conferir no navegador, por inteiro, as telas de Notas Fiscais e Estoque.

_Várias versões em uso por lista técnica, retirar de uso, escolha da versão na ordem e comparação de versões: concluídos, ver ADR-0019._

- [ ] **Listas técnicas, o que sobrou do ADR-0019**: segregar por perfil quem pode ver as listas e
      suas comparações (hoje liberado a qualquer usuário autenticado); comparar mais de duas versões
      de uma vez; criar uma nova versão a partir de uma existente (copiar os componentes).
- [ ] **Rastreio de alterações das listas técnicas** (decisão de 2026-10-21; o versionamento em si já foi feito, ADR-0019):
      a lista técnica passou a ter tela de detalhe com URL própria (`/listas-tecnicas/:id`)
      justamente para receber isso. Falta definir como versionar (nova versão a partir de
      uma existente, comparação entre versões, o que acontece com ordens abertas) e um
      **"caminho de migalhas" de alterações**: linha do tempo de quem mudou o quê e quando,
      navegável a partir do registro. A trilha de auditoria (ADR-0011) e o componente
      `HistoricoAuditoria` já existem em ordem e lote e são o ponto de partida. Exige ADR.
- [x] Subir o Node do CI para 22 (o Node 20 saiu de suporte em abril de 2026).
- [x] Atualizar jsdom (27 → 30), Vite (6 → 8, com `@vitejs/plugin-react` 6), Vitest (4 → 5) e jest-dom
      (6 → 7), que estavam presos ao Node 20 (ADR-0017). O jsdom 30 exige Node 22.22+ ou 24.15+ também na
      máquina local. O Vite 8 empacota com Rolldown: a divisão em pedaços foi para `codeSplitting`.
- [ ] Os lotes de um material (tela de estoque do material) e a posição de estoque ainda
      baixam o conjunto inteiro: lotes por material paginam só na tela, e a agregação do estoque
      roda sobre todos os materiais. Migrar para pesquisa/filtro em SQL se passarem de ~1000
      registros (ADR-0015, ADR-0016).

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

1. **Interface com visual neon** (tema escuro, sombras e brilho), ver a seção acima.
2. **Fase 5d — módulo de IA**: previsão de demanda, análise de atrasos e
   recomendações, usando o histórico do dataset sintético.
3. **Deploy** — adiado por decisão: será feito em planos pagos (provedor a definir).
   Ver a seção [Deploy: preparação pendente](#deploy-preparação-pendente).
4. **Migrações de versão principal** (fechadas no Dependabot, a fazer em PRs próprios):
   Spring Boot 4 + springdoc 3, React 19 (com react-dom e os tipos), MUI 6 → 9 (com ADR).
   Já feitas: React Router 7, ECharts 6, Vite 8 + plugin-react 6, Vitest 5, jsdom 30, jest-dom 7.
5. Depois:
   - etapa de qualidade após a produção (inspeção e laudo do lote);
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
