# ADR-0014: Proteção da borda HTTP — rate limiting, IP do cliente e limite de corpo

- **Status**: Aceito
- **Data**: 2026-10-21
- **Complementa**: ADR-0005 (autenticação JWT) e ADR-0013 (deploy no Railway)

## Contexto

Os endpoints públicos de autenticação (`/api/v1/auth/login` e `/registrar`) precisam
de proteção contra força bruta e enumeração de contas. A primeira versão do rate
limiting (Bucket4j, em memória) tinha quatro falhas, encontradas em revisão:

1. Identificava o cliente pelo primeiro valor de `X-Forwarded-For`, que o próprio
   cliente controla: trocar o header dava um bucket novo e anulava o limite.
2. O mapa de buckets nunca encolhia, e as chaves vinham desse header: um atacante
   podia esgotar a memória.
3. O "limite de tamanho de requisição" (`max-http-form-post-size`) só vale para
   formulários, então a API JSON não tinha limite de payload.
4. O `X-Request-ID` recebido ia sem validação para os logs (forja de linhas de log).

Além disso, atrás do proxy do Railway o Spring vê a conexão como HTTP e o IP do
proxy, o que desliga o HSTS e faria todos os usuários dividirem um único bucket.

## Decisão

- O rate limiting usa **sempre o IP da conexão** (`getRemoteAddr`). O filtro nunca lê
  `X-Forwarded-For`.
- Em `prod`, `server.forward-headers-strategy=native` faz o Tomcat aplicar
  `X-Forwarded-For`/`-Proto` **somente** quando a conexão vem de um proxy confiável
  (`PROXIES_CONFIAVEIS`, padrão: redes privadas). Assim `getRemoteAddr` já é o IP real
  e o HSTS volta a ser emitido.
- Os buckets continuam em memória, mas os **ociosos são descartados** ao passar de
  10 mil chaves (bucket cheio equivale a um novo).
- O 429 devolve `Retry-After` e `detail` (RFC 9457).
- `LimiteTamanhoCorpoFilter` recusa corpo acima de 1 MB (413) em POST/PUT/PATCH:
  pelo `Content-Length`, sem ler o corpo, ou, quando o cliente não o declara
  (chunked), contando os bytes durante a leitura (`CorpoGrandeDemaisException`,
  convertida em 413 pelo `TratadorGlobalDeErros`).
- `LimiteTamanhoCorpoFilter` e `RateLimitingFilter` ficam **dentro da cadeia do Spring
  Security, depois do `CorsFilter`**, e não como filtros de servlet. Como filtros de
  servlet eles respondiam 413/429 antes do CORS, sem `Access-Control-Allow-Origin`; o
  navegador descartava a resposta e o usuário via "erro de conexão". Um
  `FilterRegistrationBean` desabilitado evita o registro duplicado.
- `X-Request-ID` só é aceito com até 64 caracteres `[A-Za-z0-9._-]`.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Confiar em `X-Forwarded-For` | Forjável; é a falha que motivou este ADR |
| Caffeine para expirar buckets | Dependência nova para um problema que 10 linhas resolvem; reavaliar se o filtro crescer |
| Bucket4j distribuído no Redis | Redis não tem consumidor no código (regra do projeto); só faz sentido com mais de uma instância |
| Limitar no proxy/gateway | Não há gateway no deploy atual; não elimina a defesa em profundidade na aplicação |
| Responder 411 a corpo sem `Content-Length` | Tentado e revertido: o cliente HTTP do Spring/Java envia JSON em chunked, e a regra quebrava clientes legítimos (11 testes de integração falharam) |

## Consequências

- **Positivas**: o limite deixa de ser contornável pelo cliente; memória do filtro
  limitada pelo nº de IPs reais ativos; a API JSON ganha limite de payload; o
  frontend passa a exibir a mensagem do 429.
- **Negativas / riscos**:
  - O limite é **por instância**: com mais de uma réplica, cada uma conta
    separado. Revisitar (Redis) quando houver escala horizontal.
  - Usuários atrás do mesmo NAT compartilham o bucket.
  - **Pendente de validação no primeiro deploy:** a faixa de IPs do proxy do Railway.
    Se ela não estiver no padrão de `PROXIES_CONFIAVEIS`, o Tomcat ignora
    `X-Forwarded-*` e o IP visto passa a ser o do proxy. Conferir no deploy e
    ajustar a variável.
  - Em chunked, o corpo é lido até o limite antes de ser recusado (no máximo 1 MB em memória por requisição).
  - Clientes HTTP como o Apache HttpClient 5 repetem respostas 429 respeitando `Retry-After`; isso é desejável, mas deve ser lembrado ao escrever testes.
