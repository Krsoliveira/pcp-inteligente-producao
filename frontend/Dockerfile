# Imagem do frontend para o deploy (ADR-0033). O Vite gera os arquivos; o Nginx os serve e encaminha /api ao backend,
# no mesmo endereço público (sem CORS entre domínios).
# Gerar localmente: docker build -t pcp-frontend ./frontend

# Mesma versão do Node do CI.
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Nginx sem root (usuário nginx, porta acima de 1024), na variante slim: 21 pacotes do sistema em vez de 71, com o
# que este frontend usa (modelos com envsubst, resolvedor local e realip). Os pacotes são atualizados na geração da
# imagem para não esperar a base ser republicada com as correções de segurança.
FROM nginxinc/nginx-unprivileged:1.29-alpine-slim
USER root
RUN apk upgrade --no-cache
USER 101
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY nginx/cabecalhos-de-seguranca.conf /etc/nginx/snippets/cabecalhos-de-seguranca.conf
COPY --from=build /app/dist /usr/share/nginx/html

# PORT: a plataforma informa a porta; 8080 fora dela.
# BACKEND_ENDERECO: host:porta da API pela rede privada.
# NGINX_ENTRYPOINT_LOCAL_RESOLVERS: a imagem lê o DNS do contêiner (/etc/resolv.conf) e o expõe em
#   NGINX_LOCAL_RESOLVERS; assim o Nginx volta a resolver o endereço da API quando ela é reimplantada e muda de IP.
# BORDA_CONFIAVEL: faixa CIDR da borda da plataforma, a única de quem se aceita o cabeçalho com o IP do cliente. O
#   padrão (só o loopback) não confia em ninguém de fora; a faixa real é medida no deploy (docs/deploy.md).
# CABECALHO_DO_CLIENTE: o cabeçalho em que a borda põe o IP do cliente (no Railway, X-Real-IP).
# NGINX_ENVSUBST_FILTER: só estas variáveis entram no modelo; as demais do ambiente ficam de fora.
ENV PORT=8080 \
    BACKEND_ENDERECO=backend.railway.internal:8080 \
    BORDA_CONFIAVEL=127.0.0.1/32     CABECALHO_DO_CLIENTE=X-Forwarded-For \
    NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1 \
    NGINX_ENVSUBST_FILTER="^(PORT|BACKEND_ENDERECO|BORDA_CONFIAVEL|CABECALHO_DO_CLIENTE|NGINX_LOCAL_RESOLVERS)$"
EXPOSE 8080
