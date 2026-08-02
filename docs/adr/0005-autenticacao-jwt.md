# ADR-0005 — Autenticação com JWT (JSON Web Token)

**Status:** Aceito  
**Data:** 2026-08-02

## Contexto

A plataforma PCP expõe uma API REST consumida por um frontend React e,
futuramente, por integrações (SAP mock, Power BI). Precisamos de um mecanismo
de autenticação que seja:

- **Stateless** — sem sessão no servidor, compatível com deploys horizontais
  (múltiplas instâncias no Render).
- **Simples de integrar** — o frontend armazena o token e o envia no cabeçalho
  `Authorization: Bearer <token>`.
- **Sem dependência de terceiros** — não queremos um servidor OAuth externo
  obrigatório para rodar localmente.

## Decisão

Usar **JWT (HS256)** gerado pela própria aplicação com a biblioteca
`io.jsonwebtoken:jjwt` (0.12.x).

Fluxo:
1. `POST /api/v1/auth/registrar` cria o usuário (senha com BCrypt).
2. `POST /api/v1/auth/login` valida as credenciais e devolve um JWT com
   validade de 8 horas (configurável via `JWT_EXPIRACAO_MS`).
3. O cliente envia o token em todas as requisições protegidas.
4. O `JwtAuthenticationFilter` intercepta, valida e popula o `SecurityContext`.

O segredo de assinatura (`JWT_SECRET`) é uma variável de ambiente com mínimo
de 32 caracteres. Um valor padrão inseguro é fornecido apenas para
desenvolvimento local.

## Alternativas consideradas

| Alternativa | Motivo da rejeição |
|---|---|
| **OAuth2 com Google/GitHub** | Dependência de rede externa; complexidade desnecessária para portfólio |
| **Sessões HTTP (stateful)** | Incompatível com múltiplas instâncias; contraria o modelo REST |
| **Basic Auth** | Credenciais trafegam em toda requisição; não é padrão moderno |

## Consequências

**Positivas:**
- Sem estado no servidor — qualquer instância valida qualquer token.
- Fácil de testar: `POST /auth/login` → copiar token → usar no Swagger UI.
- Revogação de token não implementada (aceitável para portfólio; em produção
  real usaríamos uma lista negra em Redis).

**Negativas / trade-offs:**
- Tokens expirados demandam novo login (sem refresh token na Fase 2).
- Se o segredo vazar, todos os tokens emitidos ficam comprometidos.
- Revogação imediata de tokens individuais requer estado (não implementado).
