package com.krsoliveira.pcp.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Propaga um identificador de correlação (X-Request-ID) em todas as requisições.
 *
 * Se o cliente enviar o header X-Request-ID, ele é reutilizado.
 * Caso contrário, um ID curto é gerado automaticamente.
 *
 * O ID é injetado no MDC (Mapped Diagnostic Context) sob a chave "requestId",
 * tornando-o disponível em todos os logs da requisição quando o padrão de log
 * inclui %X{requestId}.
 *
 * O header X-Request-ID é também devolvido na response para facilitar o
 * rastreamento pelo frontend e ferramentas de observabilidade.
 */
@Component
@Order(-300)
public class CorrelacaoIdFilter extends OncePerRequestFilter {

    static final String HEADER  = "X-Request-ID";
    static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String id = request.getHeader(HEADER);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }

        MDC.put(MDC_KEY, id);
        response.setHeader(HEADER, id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}