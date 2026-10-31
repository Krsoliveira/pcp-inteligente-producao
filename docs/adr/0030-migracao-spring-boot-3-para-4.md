# ADR-0030: Migração do Spring Boot 3.5 para o 4.1

- **Status**: Aceito
- **Data**: 2026-10-31

## Contexto

O backend usa o Spring Boot **3.5.4** (julho de 2025). A linha 3.5 teve a última versão gratuita, a 3.5.16, em
25/06/2026 e não recebe mais correções abertas: o projeto está numa linha sem suporte e, dentro dela, 12 correções
atrás. A linha atual é a 4.1 (4.1.2, de 22/10/2026), com Spring Framework 7, Spring Security 7, Jakarta EE 11,
Hibernate 7 e Jackson 3. O Dependabot propôs Spring Boot 4.1.2 e springdoc 3.2.0 (#88, #87), que falhavam no CI.

Levantamento no código (`backend/src`):

| Mudança no Boot 4 | Onde aparece no projeto |
|---|---|
| Jackson 3 (`com.fasterxml.jackson` → `tools.jackson`) | **Nenhum import** no código de produção nem nos testes; o Jackson só é usado pelo Spring (JSON da API) e pelo `jjwt-jackson` (JWT, continua no Jackson 2) |
| Padrões de serialização do Jackson 3 | **Toda resposta JSON da API** — risco ao contrato público `/api/v1` |
| `spring-boot-starter-web` → `spring-boot-starter-webmvc`; Flyway passa a ter starter próprio | `pom.xml` |
| springdoc 2.8 → 3.x (exigido pelo Boot 4) | `pom.xml` e a sobreposição manual do `swagger-ui` 5.34 (ADR/lição do springdoc) |
| `TestRestTemplate` sai do `@SpringBootTest` (anotação e dependência próprias, pacote novo) | 16 arquivos de teste de integração, 31 usos |
| Testcontainers 2 (artefatos e pacotes renomeados) | os mesmos 16 arquivos (`PostgreSQLContainer`, `@ServiceConnection`) |
| Hibernate 7, Spring Security 7 | entidades JPA, consultas e `ConfiguracaoSeguranca` — confirmados pelos 542 testes |

Não usamos: `@MockBean`/`@SpyBean`, MockMvc, `@EntityScan`, `HttpMessageConverters`, `org.springframework.lang.Nullable`,
Undertow, WAR, nem propriedades de Jackson. O `forward-headers-strategy` de produção continua valendo (jar, não WAR).

## Decisão

Migrar para o **Spring Boot 4.1** numa branch, em passos que deixam `mvn verify` verde a cada commit:

1. **Spring Boot 3.5.4 → 3.5.16** (correções da própria linha). O guia oficial pede partir da última 3.5; é também
   o ganho de segurança imediato se o resto demorar.
2. **Referência do contrato**: com a API no ar, gravar as respostas JSON dos principais `GET` de `/api/v1` (ordens,
   materiais, listas, consumo, lotes, estoque, notas, relatório, dashboard, acessos, erros RFC 9457). É o equivalente
   das capturas de tela da migração do MUI (ADR-0029), agora para o contrato.
3. **Spring Boot 4.1** com os starters modulares (`webmvc`, `flyway`, `security-test`), springdoc 3 e o
   `spring-boot-properties-migrator` só durante a migração. Testes: `@AutoConfigureTestRestTemplate` e o pacote novo,
   Testcontainers 2.
4. **Jackson 3 e contrato**: comparar as respostas com a referência do passo 2. Diferença de formato (datas, números,
   nulos, enums) é quebra de contrato: corrige-se na configuração do Jackson, nunca no cliente.
5. **Conferência de ponta a ponta**: o frontend contra o backend novo — varredura das 19 telas e 8 diálogos sem
   erro de API —, Swagger UI abrindo, e os scanners do CI (CodeQL, Trivy, OWASP) verdes.
6. **Documentação**: este ADR passa a Aceito, com o que a execução revelar; README e `proximos-passos.md`.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Ficar no 3.5.4 | Linha sem suporte e 12 correções atrás |
| Só ir para o 3.5.16 | Resolve o atraso de hoje, mas a linha já não recebe correções; adia o problema |
| Spring Boot 4.0 em vez de 4.1 | A 4.1 é a linha atual; a 4.0 sai de suporte antes |
| Starters "classic" do Boot 4 | Caminho temporário oficial, mas só adia a troca para os modulares; o projeto tem poucos starters |
| Manter o Jackson 2 (`spring-boot-jackson2`) | Módulo já nasce obsoleto; só se justifica se o contrato não puder ser preservado no Jackson 3 |

## Consequências

- **Positivas**: backend numa linha com suporte; Java 21, Spring 7 e Hibernate 7 atuais; Dependabot volta a propor só
  correções.
- **Negativas / riscos**:
  - **Contrato da API**: o Jackson 3 muda padrões de serialização; uma diferença passaria pelos testes que só checam
    campos. Mitigação: a comparação do passo 4, campo a campo.
  - Hibernate 7 e Spring Security 7 podem mudar comportamento em consultas ou na cadeia de filtros (JWT, rate limit,
    limite de corpo, CORS). Os 542 testes de integração com PostgreSQL real cobrem esses caminhos.
  - springdoc 3 é recente; o Swagger UI só existe fora de produção.
  - O CI do backend pode ficar mais lento na primeira execução (dependências novas).
- **Rollback**: reverter o PR. Nenhuma migração Flyway nova; o banco não muda.

## Execução (2026-10-31)

Dois commits de dependência (3.5.4 → 3.5.16 → 4.1.2) e dois de teste. Feita numa pasta de trabalho separada (git
worktree), com o backend novo na porta 8081 e o mesmo banco, sem parar o ambiente em uso.

- **Referência do contrato**: 37 rotas (32 leituras e 5 erros) gravadas byte a byte no 3.5.4, conferidas duas vezes
  para separar o que varia sozinho (o `timestamp` de um erro e a ordem dos campos de validação). O 3.5.16 saiu igual.
- **Jackson 3 não mudou nenhuma resposta de sucesso**: as 32 leituras saíram idênticas byte a byte, inclusive datas,
  números e nulos.
- **O que mudou nos erros** (RFC 9457): no Spring Framework 7 o `ProblemDetail` nasce sem `type`, e o JSON deixava de
  trazê-lo — remoção de campo no `/api/v1`. O `TratadorGlobalDeErros` passou a devolver `about:blank` (como no
  Spring 6), e um teste novo (`FormatoDosErrosApiTest`) exige todos os campos. A ordem dos campos dos erros passou a
  alfabética; em JSON a ordem não tem significado (RFC 8259), então foi mantida.
- **Renomeações do Spring 7**: 422 `UNPROCESSABLE_ENTITY` → `UNPROCESSABLE_CONTENT` e 413 `PAYLOAD_TOO_LARGE` →
  `CONTENT_TOO_LARGE` — mesmo código HTTP, e o `title` é escrito em português pelo próprio tratador, então o corpo não
  mudou. Eram as 23 falhas de teste da primeira rodada.
- **Spring Security 7**: o `DaoAuthenticationProvider` recebe o serviço de usuários no construtor; `permissionsPolicy`
  ficou obsoleto e virou `permissionsPolicyHeader`, trocado só depois de um teste novo garantir os cabeçalhos de
  segurança (passou nas duas formas e falhou com a política alterada de propósito).
- **springdoc 3.2** já traz o swagger-ui 5.34: a sobreposição manual do webjar saiu. A especificação OpenAPI continua
  com os mesmos 43 caminhos; em produção o Swagger segue desligado.
- **Testes**: `@AutoConfigureTestRestTemplate` nos 15 testes que usam o `TestRestTemplate`; o `PostgreSQLContainer`
  do pacote novo (o antigo está obsoleto no Testcontainers 2).
- O `spring-boot-properties-migrator` não apontou nenhuma propriedade renomeada e foi retirado.

Resultado: 546 testes (4 novos), sem avisos de obsolescência; 37 respostas da API com o mesmo conteúdo do 3.5.4; as
28 capturas da interface (19 telas, 8 diálogos e um select aberto) contra o backend novo iguais às feitas com o 3.5.4,
com 103 chamadas à API atendidas pelo 4.1.2.
