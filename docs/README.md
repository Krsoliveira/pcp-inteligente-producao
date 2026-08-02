# Documentação — Plataforma Inteligente de PCP e Produção

Este diretório concentra toda a documentação do projeto. A regra é simples:
**nenhuma decisão relevante fica só na cabeça de alguém** — ela vira texto aqui.

## Mapa da documentação

| Documento | O que contém | Quando ler |
|---|---|---|
| [`visao-geral.md`](visao-geral.md) | Problema, objetivos, escopo e usuários | Antes de qualquer coisa |
| [`arquitetura.md`](arquitetura.md) | Camadas, fluxo de dados, diagramas e convenções | Antes de escrever ou revisar código |
| [`glossario.md`](glossario.md) | Termos de PCP e termos técnicos usados no projeto | Sempre que um termo for desconhecido |
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
| [0004](adr/0004-render-hospedagem-backend.md) | Render como hospedagem do backend | Aceito |
