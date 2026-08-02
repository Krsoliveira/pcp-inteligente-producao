# Glossário

Termos usados no projeto, sempre no formato: **termo (equivalente cotidiano)** — explicação.

## Termos de negócio (PCP)

- **PCP — Planejamento e Controle da Produção (o "cérebro logístico" da fábrica)** —
  área que decide o que produzir, quanto, quando e com quais recursos, e acompanha se
  o plano está sendo cumprido.
- **Ordem de produção (ordem de serviço da fábrica)** — documento que autoriza a
  fabricação de uma quantidade de um produto, com datas planejadas de início e fim.
- **Plano mestre de produção / MPS (agenda-mãe da fábrica)** — visão consolidada do que
  será produzido por período, que dá origem às ordens de produção.
- **Centro de trabalho (posto ou linha de produção)** — recurso onde uma operação é
  executada: uma máquina, uma célula, uma equipe.
- **Lead time (tempo de atravessamento)** — tempo total entre iniciar e concluir algo
  (produzir uma peça, receber um material).
- **OTIF — On Time In Full (entrega no prazo e completa)** — indicador que mede o % de
  pedidos entregues na data combinada e na quantidade combinada.
- **Aderência ao plano (o quanto o combinado foi cumprido)** — % das ordens executadas
  conforme o planejado.
- **Gargalo (o caixa mais lento do supermercado)** — recurso cuja capacidade limita a
  produção do sistema inteiro.
- **Previsão de demanda (estimar o pedido antes de ele chegar)** — projeção de quanto
  será vendido por período, base para planejar produção e compras.

## Termos técnicos

- **API — Application Programming Interface (balcão de atendimento entre sistemas)** —
  contrato pelo qual um sistema pede dados ou ações a outro.
- **REST (padrão de conversa via web)** — estilo de API sobre HTTP usando verbos
  (GET, POST, PUT, DELETE) e URLs que representam recursos (`/api/ordens`).
- **Clean Architecture (arquitetura em camadas de cebola)** — organização em anéis onde
  as regras de negócio ficam no centro e os detalhes técnicos nas bordas; dependências
  apontam sempre para dentro.
- **DDD — Domain-Driven Design (desenho guiado pelo negócio)** — modelar o código com o
  vocabulário real da área (aqui, o PCP), para que especialistas reconheçam os termos.
- **Entidade (ficha com identidade própria)** — objeto do domínio identificado por um ID
  que perdura no tempo (ex.: `OrdemProducao`).
- **Value Object (valor sem identidade)** — objeto definido apenas pelos seus valores
  (ex.: um período de datas); dois iguais são intercambiáveis.
- **Porta / Port (tomada padronizada)** — interface declarada pelo domínio dizendo o que
  ele precisa; a infraestrutura pluga a implementação real.
- **JPA — Jakarta Persistence API (tradutor objeto ↔ tabela)** — especificação Java que
  mapeia classes para tabelas do banco; Hibernate é a implementação usada.
- **Migração de banco (reforma versionada do banco)** — script numerado que altera a
  estrutura do banco; o Flyway aplica na ordem e registra o histórico.
- **Cache (post-it de resposta rápida)** — cópia temporária de dados caros de calcular,
  guardada no Redis para responder mais rápido.
- **Mensageria (esteira de recados entre módulos)** — comunicação assíncrona por
  eventos; quem publica não espera quem consome. Aqui, RabbitMQ.
- **JWT — JSON Web Token (crachá digital assinado)** — token que o usuário recebe no
  login e apresenta a cada requisição para provar quem é.
- **CI — Continuous Integration (esteira de verificação automática)** — a cada push, o
  GitHub compila, testa e roda varreduras de segurança automaticamente.
- **DevSecOps (segurança embutida na esteira)** — prática de integrar verificações de
  segurança ao fluxo de desenvolvimento, e não deixá-las para o final.
- **SAST — Static Application Security Testing (revisão de segurança do código parado)**
  — análise do código-fonte em busca de padrões vulneráveis, sem executá-lo.
- **ADR — Architecture Decision Record (ata de decisão de arquitetura)** — documento
  curto registrando contexto, decisão, alternativas e consequências.
- **Testcontainers (laboratório descartável)** — biblioteca que sobe um banco real em
  container Docker só durante os testes e o destrói ao final.
