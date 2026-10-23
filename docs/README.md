# Documentação — Plataforma Inteligente de PCP e Produção

Este diretório concentra toda a documentação do projeto. A regra é simples:
**nenhuma decisão relevante fica só na cabeça de alguém** — ela vira texto aqui.

## Mapa da documentação

| Documento | O que contém | Quando ler |
|---|---|---|
| [`visao-geral.md`](visao-geral.md) | Problema, objetivos, escopo e usuários | Antes de qualquer coisa |
| [`arquitetura.md`](arquitetura.md) | Camadas, fluxo de dados, diagramas e convenções | Antes de escrever ou revisar código |
| [`glossario.md`](glossario.md) | Termos de PCP e termos técnicos usados no projeto | Sempre que um termo for desconhecido |
| [`proximos-passos.md`](proximos-passos.md) | Estado atual do trabalho, pendências e próximos passos | Ao retomar o trabalho |
| [`adr/`](adr/) | ADRs — registros de decisões de arquitetura | Quando quiser saber *por que* algo é como é |

## O que é um ADR?

**ADR (Architecture Decision Record, ou "registro de decisão de arquitetura")** é um
arquivo curto que documenta uma decisão importante: qual era o contexto, o que foi
decidido, quais alternativas foram descartadas e quais as consequências.

É como a ata de uma reunião de obra: meses depois, qualquer pessoa entende por que a
fundação foi feita daquele jeito sem precisar perguntar a quem estava lá.

Regras dos ADRs deste projeto:

1. Numeração sequencial: `0001-`, `0002-`, ...
2. Um ADR nunca é editado depois de aceito — se a decisão mudar, cria-se um novo ADR
   que **substitui** o antigo (e o antigo ganha status `Substituído por ADR-XXXX`).
3. Todo ADR segue o [`adr/template.md`](adr/template.md).

## Índice de ADRs

| ADR | Título | Status |
|---|---|---|
| [0001](adr/0001-clean-architecture-ddd.md) | Clean Architecture + DDD no backend | Aceito |
| [0002](adr/0002-rabbitmq-mensageria.md) | RabbitMQ como mensageria | Aceito |
| [0003](adr/0003-openai-provedor-ia.md) | OpenAI como provedor de IA (com camada de abstração) | Aceito |
| [0004](adr/0004-render-hospedagem-backend.md) | Render como hospedagem do backend | Substituído por ADR-0013 |
| [0005](adr/0005-autenticacao-jwt.md) | Autenticação com JWT | Aceito |
| [0006](adr/0006-dataset-carga-inicial.md) | Dataset e estratégia de carga inicial | Substituído parcialmente por ADR-0009 |
| [0007](adr/0007-modelo-material-lista-tecnica-lote.md) | Material, Lista Técnica (BOM), Consumo e Lote | Aceito |
| [0008](adr/0008-banco-de-dados-provedor.md) | Banco de dados: PostgreSQL local e Neon em produção | Aceito |
| [0009](adr/0009-dataset-sintetico-modelo-expandido.md) | Dataset sintético para o modelo expandido | Aceito |
| [0010](adr/0010-entrada-de-material-por-compra.md) | Entrada de matéria-prima por compra gera lote | Aceito |
| [0011](adr/0011-rastreabilidade-e-auditoria.md) | Rastreabilidade e auditoria: assinatura, trilha de eventos e genealogia de lotes | Aceito |
| [0012](adr/0012-suprimentos-nota-fiscal-estoque.md) | Suprimentos: entrada de nota fiscal, estoque por material e padrões de código e lote | Aceito |
| [0013](adr/0013-deploy-railway.md) | Railway como plataforma de deploy | Aceito |
| [0014](adr/0014-protecao-da-borda-http.md) | Proteção da borda HTTP: rate limiting, IP do cliente e limite de corpo | Aceito |
| [0015](adr/0015-pesquisa-paginada-e-resumo-do-dashboard.md) | Pesquisa paginada de ordens e resumo do dashboard | Aceito |
| [0016](adr/0016-consulta-em-duas-etapas.md) | Consulta em duas etapas nas listagens | Aceito |
| [0017](adr/0017-testes-do-frontend-com-vitest.md) | Testes automáticos do frontend com Vitest e Testing Library | Aceito |
| [0018](adr/0018-filtro-e-ordenacao-por-coluna.md) | Filtro e ordenação por coluna no cabeçalho (estilo planilha) | Aceito |
| [0019](adr/0019-versoes-de-lista-tecnica.md) | Várias versões em uso por lista técnica, escolha na ordem e comparação | Aceito |
| [0020](adr/0020-controle-de-acesso-por-perfil.md) | Controle de acesso por perfis de permissões | Aceito |
| [0021](adr/0021-troca-de-senha-no-primeiro-acesso.md) | Troca de senha no primeiro acesso e redefinição pelo gestor | Aceito |
| [0022](adr/0022-dashboard-acionavel.md) | Dashboard acionável: indicadores que abrem a consulta e gráficos que dizem algo | Aceito |
