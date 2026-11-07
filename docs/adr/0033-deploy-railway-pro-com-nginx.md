# ADR-0033: Deploy no Railway Pro, com o Nginx do frontend na frente da API

- **Status**: Aceito
- **Data**: 2026-11-07
- **Revisa**: ADR-0013 (Railway como plataforma de deploy)

## Contexto

O ADR-0013 escolheu o Railway antes de existir qualquer artefato de deploy. Desde então, quatro coisas mudaram:

- **Serviços:** o backend não usa Redis nem RabbitMQ (nenhum dos dois está no `pom.xml`). Subi-los só geraria custo.
- **Configuração:** o `railway.json`/`railway.toml` que o ADR-0013 previa está obsoleto no Railway. Serviços novos não podem mais usá-lo, e o suporte acaba em 2026-12-01. O substituto, `.railway/railway.ts` (IaC), é recente, e a referência dele não documenta build por `Dockerfile` em subpasta de monorepo.
- **Plano e domínio:** o dono do projeto assinou o plano **Pro** para hospedar vários projetos e registrou `krsoliveira.com.br`, que será dividido entre eles por subdomínio.
- **IP do cliente:** o limite de tentativas de login usa o IP do cliente. No Railway, a requisição passa pela borda da plataforma e pelo Nginx antes de chegar à API. Confiar no `X-Forwarded-For` errado deixa forjar o IP e furar o limite (ver a regra de segurança do projeto).

## Decisão

**Três serviços num projeto do Railway Pro**, implantados a cada merge na `main`, depois do CI:

| Serviço | Origem | Exposição |
|---|---|---|
| `Postgres` | Banco gerenciado do Railway | Só rede privada |
| `backend` | `backend/Dockerfile`: Maven gera o JAR; JRE 21 Alpine roda sem root | Só rede privada (`backend.railway.internal:8080`) |
| `frontend` | `frontend/Dockerfile`: Vite gera os arquivos; Nginx sem root os serve | Público, em `pcp.krsoliveira.com.br` |

- **Mesmo endereço para tela e API.** O Nginx serve a aplicação e encaminha `/api/` ao backend pela rede privada, então não há CORS entre domínios. Recarregar uma rota da aplicação devolve o `index.html`.
- **O Nginx consulta o DNS de novo a cada 10 s.** O resolvedor vem do próprio contêiner (`NGINX_ENTRYPOINT_LOCAL_RESOLVERS`), porque o backend muda de IP a cada deploy.
- **O Nginx decide quem é o cliente, e o backend confia só no Nginx.**
  - O módulo `realip` lê o IP do cliente de um cabeçalho (`CABECALHO_DO_CLIENTE`) e só o aceita da faixa da borda (`BORDA_CONFIAVEL`). Por padrão, de ninguém.
  - Ele repassa à API um único IP. O backend aceita esse cabeçalho somente do Nginx (`PROXIES_CONFIAVEIS`).
  - Faixa errada nunca abre brecha: no pior caso, todos dividem o mesmo limite.
  - **Medido no primeiro deploy (2026-11-07):**
    - A borda do Railway tem duas camadas: uma pública (`46.151.194.x`), que muda, e uma interna (`100.64.0.0/10`), que é quem conecta no Nginx.
    - A borda descarta o `X-Forwarded-For` do cliente e preenche o `X-Real-IP`, por isso o Nginx usa `X-Real-IP` vindo de `100.64.0.0/10`.
    - O Nginx chega à API por IPv4 (`10.*`) e por IPv6 privado (`fd..`). O padrão de `PROXIES_CONFIAVEIS` não cobria o IPv6, e a API deixava de confiar no Nginx (sem HSTS, cliente visto como o Nginx). Agora cobre `fc00::/7`.
- **Configuração no painel do Railway, registrada em `docs/deploy.md`**, em vez de arquivo da plataforma: o Config as Code está obsoleto e o IaC não cobre o nosso caso.
- **Segurança das imagens.** As duas rodam sem root. O frontend usa a variante `alpine-slim` do Nginx (21 pacotes em vez de 71) e atualiza os pacotes na geração da imagem. Na primeira varredura, o Trivy encontrou 54 vulnerabilidades HIGH/CRITICAL com correção na imagem Nginx completa e zero depois da troca; o backend saiu limpo.
- **Cabeçalhos das páginas no Nginx.** CSP só com scripts da própria origem, `nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy`, `Permissions-Policy` e HSTS. As respostas da API mantêm os cabeçalhos do backend.
- **PostgreSQL 18 em todos os ambientes.** O banco do Railway nasce no 18. O projeto, que usava o 16 (ADR-0008), passa ao 18 no desenvolvimento (`docker-compose.yml`, com volume novo, porque o 18 guarda os dados em outro caminho e não abre a pasta do 16), no ensaio (`docker-compose.prod.yml`) e nos testes com Testcontainers. O que passa nos testes é o que roda em produção.
- **Autocadastro desligado em produção**, como no ADR-0031: contas criadas pelo Administrador.
- **Backup:** backup agendado do volume do Postgres e recuperação a um instante (PITR), recursos do Railway, ligados no painel.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Vercel para o frontend + Railway para o backend | Duas plataformas e duas origens (CORS), sem ganho com o Pro já pago |
| Supabase para o banco | Mais uma plataforma; o projeto não usa autenticação nem API automática dele, e o gratuito pausa sem uso |
| `railway.json` / `railway.toml` | Obsoleto; serviços novos não podem usar |
| `.railway/railway.ts` (IaC) | Recente; a referência não documenta Dockerfile em subpasta. Reavaliar quando documentar |
| Repassar a cadeia do `X-Forwarded-For` e decidir só no backend | Exigiria que a lista de proxies confiáveis do backend cobrisse a borda e o Nginx; um erro nela abre a brecha de forjar o IP |
| Recriar o banco do Railway no PostgreSQL 16 | O volume já tinha sido iniciado pelo 18 e não volta de versão; subir o projeto ao 18, com o banco ainda vazio, deixa os ambientes iguais na versão mais nova |
| Imagem `nginx-unprivileged:alpine` completa | 54 vulnerabilidades HIGH/CRITICAL com correção na data da decisão |

## Consequências

- **Positivas:**
  - deploy automático e reproduzível, ensaiado localmente com `docker-compose.prod.yml` e as mesmas imagens;
  - um único domínio público;
  - IP do cliente decidido num ponto só, com teste de abuso.
- **Negativas e riscos:**
  - **Custo:** o consumo real (Java pesa mais) só se conhece depois do primeiro mês.
  - **Configuração no painel:** mudança feita lá e não registrada em `docs/deploy.md` se perde de vista.
  - **Imagens base:** dependem de atualização; o CI ainda não gera nem varre as imagens.
  - **Google Fonts:** a fonte Inter vem do Google, então o navegador do usuário fala com um terceiro.
- **Pendências:**
  - job no CI que gere e varra as duas imagens;
  - Dependabot para as imagens base (`docker`);
  - fonte servida pela própria aplicação;
  - métricas e alertas quando houver uso real.
