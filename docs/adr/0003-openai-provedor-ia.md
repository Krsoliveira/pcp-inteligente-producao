# ADR-0003: OpenAI como provedor de IA, atrás de uma camada de abstração

- **Status**: Aceito
- **Data**: 2026-08-02

## Contexto

As funcionalidades de IA (previsão de demanda, análise de atrasos, recomendações em
linguagem natural) exigem um modelo de linguagem. As opções eram um provedor via API
(OpenAI) ou modelos locais (Ollama). O autor dispõe de chave de API da OpenAI e o
projeto, sendo peça de portfólio, se beneficia de respostas de alta qualidade nas
demonstrações.

## Decisão

Usar a **API da OpenAI** como provedor de IA. O domínio conversa apenas com uma
**porta** (interface própria do projeto, ex.: `GeradorDeRecomendacoes`); o cliente
OpenAI é um detalhe de infraestrutura que a implementa.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Ollama (modelos locais, gratuitos) | Zero custo, mas qualidade inferior nas tarefas de análise/recomendação e exige máquina potente; pior vitrine para demonstrações |
| Acoplar o SDK da OpenAI diretamente nos services | Trocar de provedor (ou mockar nos testes) exigiria reescrever regras de negócio |

## Consequências

- **Positivas**: qualidade alta nas respostas; sem infraestrutura extra para rodar
  modelos; testes usam uma implementação fake da porta, sem custo nem rede.
- **Negativas / riscos**: custo por chamada — mitigado com cache (Redis) das análises
  e limites de uso; a chave de API é segredo e vive **apenas** em variável de
  ambiente (nunca em código ou Git — o Gitleaks no CI vigia isso).
- A abstração deixa a troca por Ollama ou outro provedor como tarefa localizada em
  `infrastructure/`, sem tocar domínio nem casos de uso.
