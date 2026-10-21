package com.krsoliveira.pcp.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garante que os filtros sobem como beans e leem as propriedades pcp.seguranca.*,
 * sem precisar de banco nem de Docker.
 */
class FiltrosDeSegurancaConfiguracaoTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(RateLimitingFilter.class, LimiteTamanhoCorpoFilter.class,
                    CorrelacaoIdFilter.class);

    @Test
    @DisplayName("limite de login vem da propriedade pcp.seguranca.rate-limit.login")
    void limiteDeLoginVemDaPropriedade() {
        contexto.withPropertyValues("pcp.seguranca.rate-limit.login=1").run(ctx -> {
            var filtro = ctx.getBean(RateLimitingFilter.class);

            assertThat(executarLogin(filtro).getStatus()).isNotEqualTo(429);
            assertThat(executarLogin(filtro).getStatus()).isEqualTo(429);
        });
    }

    @Test
    @DisplayName("sem propriedades, valem os padrões: 5 tentativas de login por minuto")
    void padroesSemPropriedades() {
        contexto.run(ctx -> {
            var filtro = ctx.getBean(RateLimitingFilter.class);

            for (int i = 0; i < 5; i++) {
                assertThat(executarLogin(filtro).getStatus()).isNotEqualTo(429);
            }
            assertThat(executarLogin(filtro).getStatus()).isEqualTo(429);
        });
    }

    @Test
    @DisplayName("limite do corpo vem da propriedade pcp.seguranca.corpo-maximo-bytes")
    void limiteDoCorpoVemDaPropriedade() {
        contexto.withPropertyValues("pcp.seguranca.corpo-maximo-bytes=10").run(ctx -> {
            var filtro   = ctx.getBean(LimiteTamanhoCorpoFilter.class);
            var request  = new MockHttpServletRequest("POST", "/api/v1/materiais");
            var response = new MockHttpServletResponse();
            request.setContent(new byte[11]);

            filtro.doFilter(request, response, new MockFilterChain());

            assertThat(response.getStatus()).isEqualTo(413);
        });
    }

    private static MockHttpServletResponse executarLogin(RateLimitingFilter filtro) throws Exception {
        var request  = new MockHttpServletRequest("POST", RateLimitingFilter.ROTA_LOGIN);
        var response = new MockHttpServletResponse();
        request.setRemoteAddr("203.0.113.7");
        filtro.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
