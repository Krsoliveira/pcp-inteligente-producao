package com.krsoliveira.pcp.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingFilterTest {

    /** Limites baixos para agilizar o teste. */
    private static final int LIMITE_LOGIN     = 2;
    private static final int LIMITE_REGISTRAR = 1;

    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter(LIMITE_LOGIN, LIMITE_REGISTRAR);
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("login: requisições dentro do limite são permitidas")
    void loginDentroDoLimitePermitido() throws Exception {
        for (int i = 0; i < LIMITE_LOGIN; i++) {
            var response = executar(RateLimitingFilter.ROTA_LOGIN, "192.168.1.1");
            assertThat(response.getStatus())
                    .as("requisição %d deve passar", i + 1)
                    .isNotEqualTo(429);
        }
    }

    @Test
    @DisplayName("login: retorna 429 ao exceder o limite por IP")
    void loginRetorna429AoExcederLimite() throws Exception {
        for (int i = 0; i < LIMITE_LOGIN; i++) {
            executar(RateLimitingFilter.ROTA_LOGIN, "10.0.0.1");
        }

        var response = executar(RateLimitingFilter.ROTA_LOGIN, "10.0.0.1");

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentType()).contains("application/problem+json");
    }

    @Test
    @DisplayName("login: IPs diferentes têm buckets independentes")
    void loginIpsDiferentesNaoCompartilhamBucket() throws Exception {
        for (int i = 0; i < LIMITE_LOGIN; i++) {
            executar(RateLimitingFilter.ROTA_LOGIN, "172.16.0.1");
        }

        // IP diferente ainda deve passar
        var response = executar(RateLimitingFilter.ROTA_LOGIN, "172.16.0.2");
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    // -------------------------------------------------------------------------
    // Registrar
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("registrar: retorna 429 ao exceder o limite por IP")
    void registrarRetorna429AoExcederLimite() throws Exception {
        for (int i = 0; i < LIMITE_REGISTRAR; i++) {
            executar(RateLimitingFilter.ROTA_REGISTRAR, "10.0.0.2");
        }

        var response = executar(RateLimitingFilter.ROTA_REGISTRAR, "10.0.0.2");

        assertThat(response.getStatus()).isEqualTo(429);
    }

    // -------------------------------------------------------------------------
    // Outras rotas
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("rotas fora de /auth não sofrem rate limiting")
    void rotasForaDeAuthNaoSofremRateLimiting() throws Exception {
        for (int i = 0; i < LIMITE_LOGIN + 10; i++) {
            var response = executar("/api/v1/ordens-producao", "10.0.0.3");
            assertThat(response.getStatus()).isNotEqualTo(429);
        }
    }

    @Test
    @DisplayName("X-Forwarded-For forjado não burla o limite: vale o IP da conexão")
    void forwardedForForjadoNaoBurlaLimite() throws Exception {
        for (int i = 0; i < LIMITE_LOGIN; i++) {
            executar(RateLimitingFilter.ROTA_LOGIN, "10.0.0.4", "198.51.100." + i);
        }

        var response = executar(RateLimitingFilter.ROTA_LOGIN, "10.0.0.4", "198.51.100.200");

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("usa sempre o remoteAddr, ignorando X-Forwarded-For")
    void usaRemoteAddrIgnorandoForwardedFor() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
        request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");

        assertThat(RateLimitingFilter.extrairIp(request)).isEqualTo("192.0.2.10");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private MockHttpServletResponse executar(String uri, String ip) throws Exception {
        return executar(uri, ip, null);
    }

    private MockHttpServletResponse executar(String uri, String ip, String forwardedFor) throws Exception {
        var request  = new MockHttpServletRequest("POST", uri);
        var response = new MockHttpServletResponse();
        request.setRemoteAddr(ip);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        filter.doFilterInternal(request, response, new MockFilterChain());
        return response;
    }
}
