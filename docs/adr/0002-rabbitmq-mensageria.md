# ADR-0002: RabbitMQ como mensageria

- **Status**: Aceito
- **Data**: 2026-08-02

## Contexto

O sistema precisa de comunicação assíncrona entre módulos: quando uma ordem de
produção é criada ou atrasa, módulos como o de IA devem reagir sem travar a resposta
ao usuário. O volume esperado é baixo (é um sistema de planejamento, não de telemetria
de chão de fábrica). O ambiente de desenvolvimento é uma máquina local com Docker.

## Decisão

Usar **RabbitMQ** como broker de mensagens (fila de eventos), rodando em container
no desenvolvimento e como serviço gerenciado no deploy.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Apache Kafka | Projetado para fluxos massivos e retenção longa de eventos; consome muito mais memória localmente e tem curva de aprendizado maior — capacidade que este projeto não usaria |
| Eventos internos do Spring (`ApplicationEventPublisher`) | Não sobrevivem a reinício da aplicação nem permitem consumidores externos; serviriam só no curtíssimo prazo |

## Consequências

- **Positivas**: leve para rodar localmente (container único com interface web de
  administração); modelo de filas e rotas simples de entender; suporte maduro no
  Spring (`spring-amqp`).
- **Negativas / riscos**: se um dia o projeto precisar de reprocessamento histórico de
  eventos em larga escala, seria necessário migrar para Kafka — cenário improvável
  aqui. A porta de publicação de eventos no domínio isola essa eventual troca.
