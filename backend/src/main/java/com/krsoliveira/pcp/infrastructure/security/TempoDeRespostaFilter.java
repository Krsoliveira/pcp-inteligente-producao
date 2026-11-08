package com.krsoliveira.pcp.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Mede quanto cada requisição levou e registra, em WARN, as que passam do limite ({@code pcp.observabilidade.
 * requisicao-lenta-ms}). As rápidas não geram linha: o volume de log fica proporcional ao problema.
 *
 * <p>Registra método, rota (sem parâmetros de consulta, que podem trazer dado do usuário), status e duração. Roda logo
 * depois do filtro de correlação, então a linha leva o {@code requestId}.
 */
@Component
@Order(-290)
public class TempoDeRespostaFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger("pcp.desempenho");

    private static final Pattern CONTROLE = Pattern.compile("\\p{Cntrl}");

    private final long limiteEmMilissegundos;

    public TempoDeRespostaFilter(@Value("${pcp.observabilidade.requisicao-lenta-ms:1000}") long limiteEmMilissegundos) {
        this.limiteEmMilissegundos = limiteEmMilissegundos;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // O health check do Railway bate o tempo todo e não interessa aqui.
        return request.getRequestURI().startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long inicio = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long duracao = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
            if (duracao >= limiteEmMilissegundos) {
                LOG.warn("requisicao_lenta requisicao={} {} status={} duracaoMs={}", request.getMethod(),
                        CONTROLE.matcher(request.getRequestURI()).replaceAll("?"), response.getStatus(), duracao);
            }
        }
    }
}
