package com.krsoliveira.pcp.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Registra uma linha por requisição que altera dados (POST, PUT, PATCH e DELETE): quem, o quê, o resultado e quanto
 * levou. Resposta 4xx sai em WARN e 5xx em ERROR, para a recusa de uma regra de negócio aparecer sem abrir a auditoria.
 *
 * <p>Fica dentro da cadeia do Spring Security, depois do filtro do JWT: só ali o usuário já está identificado, e o
 * status é o final (401 e 403 já traduzidos). O corpo da requisição e os parâmetros de consulta nunca são registrados;
 * o que mudou no registro continua só na trilha de auditoria (ADR-0011). Leituras ficam de fora para o volume de log
 * acompanhar o uso de escrita. O login tem os próprios eventos em {@link EventosDeSeguranca}.
 */
@Component
public class AtividadeFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger("pcp.atividade");

    private static final Set<String> METODOS_QUE_ALTERAM = Set.of(
            HttpMethod.POST.name(), HttpMethod.PUT.name(), HttpMethod.PATCH.name(), HttpMethod.DELETE.name());
    private static final String PREFIXO_DA_AUTENTICACAO = "/api/v1/auth/";
    private static final int PRIMEIRO_STATUS_DE_ERRO_DO_CLIENTE = 400;
    private static final int PRIMEIRO_STATUS_DE_ERRO_DO_SERVIDOR = 500;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !METODOS_QUE_ALTERAM.contains(request.getMethod())
                || request.getRequestURI().startsWith(PREFIXO_DA_AUTENTICACAO);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long inicio = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            registrar(request, response.getStatus(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio));
        }
    }

    private void registrar(HttpServletRequest request, int status, long duracaoEmMilissegundos) {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        String usuario = EventosDeSeguranca.mascarar(autenticacao == null ? null : autenticacao.getName());
        String metodo = request.getMethod();
        String rota = EventosDeSeguranca.apenasImprimiveis(request.getRequestURI());
        String mensagem = "requisicao usuario={} {} {} status={} duracaoMs={}";
        if (status >= PRIMEIRO_STATUS_DE_ERRO_DO_SERVIDOR) {
            LOG.error(mensagem, usuario, metodo, rota, status, duracaoEmMilissegundos);
        } else if (status >= PRIMEIRO_STATUS_DE_ERRO_DO_CLIENTE) {
            LOG.warn(mensagem, usuario, metodo, rota, status, duracaoEmMilissegundos);
        } else {
            LOG.info(mensagem, usuario, metodo, rota, status, duracaoEmMilissegundos);
        }
    }
}
