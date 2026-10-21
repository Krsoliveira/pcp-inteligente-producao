package com.krsoliveira.pcp.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Recusa corpos grandes em POST, PUT e PATCH antes de qualquer leitura ou parse,
 * protegendo a API contra payloads gigantes.
 *
 * <ul>
 *   <li>Content-Length acima do limite → HTTP 413.</li>
 *   <li>Corpo em Transfer-Encoding (sem tamanho declarado) → HTTP 411, pois o tamanho
 *       não pode ser conferido de antemão. O frontend e o curl sempre enviam
 *       Content-Length em JSON.</li>
 * </ul>
 *
 * O limite do Tomcat para formulários ({@code max-http-form-post-size}) não cobre
 * JSON, por isso este filtro existe. A resposta segue o formato Problem Details
 * (RFC 9457).
 */
@Component
@Order(-250)
public class LimiteTamanhoCorpoFilter extends OncePerRequestFilter {

    private static final Set<String> METODOS_COM_CORPO = Set.of("POST", "PUT", "PATCH");

    private final long limiteBytes;

    public LimiteTamanhoCorpoFilter(@Value("${pcp.seguranca.corpo-maximo-bytes:1048576}") long limiteBytes) {
        this.limiteBytes = limiteBytes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!METODOS_COM_CORPO.contains(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        if (request.getContentLengthLong() > limiteBytes) {
            recusar(response, 413, "Conteúdo grande demais",
                    "O corpo da requisição excede o limite de " + limiteBytes + " bytes.");
        } else if (request.getContentLengthLong() < 0 && request.getHeader("Transfer-Encoding") != null) {
            recusar(response, 411, "Tamanho obrigatório",
                    "Informe o cabeçalho Content-Length; corpo sem tamanho declarado não é aceito.");
        } else {
            chain.doFilter(request, response);
        }
    }

    private void recusar(HttpServletResponse response, int status, String titulo, String detalhe) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json;charset=UTF-8");
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + titulo + "\",\"status\":" + status
                + ",\"detail\":\"" + detalhe + "\"}");
    }
}
