# ADR-0004: Render como hospedagem do backend

- **Status**: Aceito
- **Data**: 2026-08-02

## Contexto

O backend (Java + Spring Boot) precisa de hospedagem na nuvem para o deploy do
portfólio. O README original listava Railway, Render ou Azure. Requisitos: custo
próximo de zero, deploy simples a partir do GitHub e suporte a container Docker
(necessário para aplicações Java nessas plataformas).

## Decisão

Hospedar o backend no **Render**, via imagem Docker construída a partir do
repositório. O frontend permanece na Vercel e o PostgreSQL em serviço gerenciado.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Railway | Sem plano gratuito permanente; custo recorrente para um portfólio |
| Azure | Alinha com o perfil "enterprise"/SAP do autor, mas a configuração é bem mais complexa e o nível gratuito é limitado no tempo; complexidade não agrega ao objetivo atual |

## Consequências

- **Positivas**: plano gratuito; deploy automático a cada push na `main`; variáveis de
  ambiente gerenciadas pela plataforma (onde viverá a chave da OpenAI).
- **Negativas / riscos**: no plano gratuito o serviço "hiberna" após inatividade e a
  primeira requisição demora (cold start) — aceitável para demonstração. O projeto
  será containerizado (Dockerfile), o que torna uma futura migração para Azure ou
  outra nuvem um exercício de configuração, não de reescrita.
