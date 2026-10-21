package com.krsoliveira.pcp.infrastructure.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita o número de requisições por IP nas rotas públicas de autenticação,
 * protegendo contra ataques de força bruta e enumeração de contas.
 *
 * Limites (por IP, por minuto):
 *   POST /api/v1/auth/login     → {@code pcp.seguranca.rate-limit.login} (padrão: 5)
 *   POST /api/v1/auth/registrar → {@code pcp.seguranca.rate-limit.registrar} (padrão: 3)
 *
 * O IP é sempre o da conexão ({@code getRemoteAddr}). O header X-Forwarded-For
 * é controlado pelo cliente e nunca é lido aqui: atrás de proxy, é o Tomcat
 * ({@code server.forward-headers-strategy=native}, perfil prod) que o aplica,
 * e só quando a requisição vem de um proxy confiável.
 *
 * Ao exceder o limite, retorna HTTP 429 com corpo no formato Problem Details
 * (RFC 9457). O bucket é reiniciado automaticamente após 1 minuto.
 *
 * Os limites são configuráveis por propriedade e por construtor (testes).
 */
@Component
@Order(-200)
public class RateLimitingFilter extends OncePerRequestFilter {

    static final String ROTA_LOGIN     = "/api/v1/auth/login";
    static final String ROTA_REGISTRAR = "/api/v1/auth/registrar";

    private final int limiteLogin;
    private final int limiteRegistrar;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(@Value("${pcp.seguranca.rate-limit.login:5}") int limiteLogin,
                              @Value("${pcp.seguranca.rate-limit.registrar:3}") int limiteRegistrar) {
        this.limiteLogin     = limiteLogin;
        this.limiteRegistrar = limiteRegistrar;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        if (!ROTA_LOGIN.equals(uri) && !ROTA_REGISTRAR.equals(uri)) {
            chain.doFilter(request, response);
            return;
        }

        int limite = ROTA_LOGIN.equals(uri) ? limiteLogin : limiteRegistrar;
        String ip    = extrairIp(request);
        String chave = ip + ":" + uri;

        Bucket bucket = buckets.computeIfAbsent(chave, k -> criarBucket(limite));

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/problem+json;charset=UTF-8");
            response.getWriter().write(
                    "{\"status\":429,\"title\":\"Muitas tentativas. Aguarde um momento e tente novamente.\"}"
            );
        }
    }

    private Bucket criarBucket(int capacidade) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacidade)
                        .refillIntervally(capacidade, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    static String extrairIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
