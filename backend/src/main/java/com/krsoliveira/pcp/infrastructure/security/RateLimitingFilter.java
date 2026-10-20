package com.krsoliveira.pcp.infrastructure.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
 *   POST /api/v1/auth/login     → {@code limiteLogin} tentativas (padrão: 5)
 *   POST /api/v1/auth/registrar → {@code limiteRegistrar} tentativas (padrão: 3)
 *
 * Ao exceder o limite, retorna HTTP 429 com corpo no formato Problem Details
 * (RFC 9457). O bucket é reiniciado automaticamente após 1 minuto.
 *
 * Os limites são configuráveis via construtor para facilitar testes unitários.
 */
@Component
@Order(-200)
public class RateLimitingFilter extends OncePerRequestFilter {

    static final String ROTA_LOGIN     = "/api/v1/auth/login";
    static final String ROTA_REGISTRAR = "/api/v1/auth/registrar";

    private final int limiteLogin;
    private final int limiteRegistrar;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** Construtor padrão usado pelo Spring — limites de produção. */
    public RateLimitingFilter() {
        this(5, 3);
    }

    /** Construtor para testes — permite definir limites menores. */
    RateLimitingFilter(int limiteLogin, int limiteRegistrar) {
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
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
