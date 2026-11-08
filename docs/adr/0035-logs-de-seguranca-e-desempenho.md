# ADR-0035: Logs de segurança e de desempenho

- **Status**: Aceito
- **Data**: 2026-11-08

## Contexto

A trilha de auditoria (ADR-0011) responde "quem alterou o quê". Ela não responde "quem tentou entrar e falhou", "quem foi barrado" nem "o que está lento". Antes desta decisão o código só registrava a subida do sistema e um aviso do banco, e o erro 500 saía sem o `requestId` (o Tomcat escreve o log depois de o filtro de correlação limpar o MDC).

## Decisão

Logs de eventos, em dois loggers com nome próprio para filtrar na plataforma:

- **`pcp.seguranca`**: `login_sucesso`, `login_recusado` (com o motivo real: `credenciais_invalidas` ou `usuario_desativado`), `acesso_negado` (usuário, método e rota) e `limite_excedido` (método e rota). A resposta da API não muda: o usuário desativado continua recebendo a mesma mensagem de senha errada.
- **`pcp.desempenho`**: `requisicao_lenta`, em WARN, quando a requisição leva `pcp.observabilidade.requisicao-lenta-ms` (padrão 1000, variável `REQUISICAO_LENTA_MS`) ou mais. Requisição rápida e health check não geram linha.
- **Erro inesperado (500)**: `erro_inesperado` com a causa e o `requestId`, registrado pelo filtro de correlação, que relança a exceção. O Tomcat ainda escreve a própria linha, sem `requestId`.

Em produção o console já sai em JSON (formato ECS), com o `requestId`; em desenvolvimento, texto legível.

### O que nunca vai para o log

Senha, token, corpo da requisição e parâmetros de consulta. O e-mail aparece mascarado (`m***@dominio.com`) e é higienizado contra quebra de linha (injeção de log). O IP do cliente não é registrado aqui: a plataforma já o guarda no log de acesso, e o `requestId` liga os dois.

## Alternativas

- **Eventos de login na trilha de auditoria:** dá consulta pela tela, mas exige migração e muda a regra de que a trilha só registra mutações de dados. Fica como evolução separada.
- **Tratador global para `Exception` devolvendo 500 em RFC 9457:** daria um ponto único de log, mas muda o corpo de resposta do 500 e converte exceções de autenticação hoje tratadas pelo Spring Security. O filtro acrescenta o log sem mexer na resposta.
- **Métricas (Micrometer/Prometheus) e rastreamento:** ficam para uma segunda etapa; este ADR cobre só logs.

## Consequências

- Dá para responder, pelo log, "quem tentou entrar e não conseguiu" e "quais rotas passam do limite", sem custo de volume em operação normal.
- Um 500 gera duas linhas de erro (a nossa, com `requestId`, e a do Tomcat). É ruído aceito para não mudar a resposta.
- Os eventos de login não têm consulta pela tela e se perdem com a rotação de logs da plataforma.
- Reverter é apagar `EventosDeSeguranca`, `TempoDeRespostaFilter` e o `catch` do filtro de correlação; não há dado nem esquema envolvidos.
