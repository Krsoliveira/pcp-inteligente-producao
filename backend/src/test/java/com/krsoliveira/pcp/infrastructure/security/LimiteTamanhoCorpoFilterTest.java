package com.krsoliveira.pcp.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class LimiteTamanhoCorpoFilterTest {

    private static final long LIMITE = 100;

    private final LimiteTamanhoCorpoFilter filter = new LimiteTamanhoCorpoFilter(LIMITE);

    @Test
    @DisplayName("corpo dentro do limite segue para a cadeia")
    void corpoDentroDoLimiteSegue() throws Exception {
        var request = requisicao("POST", LIMITE);
        var chain   = new MockFilterChain();

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @ParameterizedTest(name = "{0} acima do limite retorna 413")
    @ValueSource(strings = {"POST", "PUT", "PATCH"})
    @DisplayName("corpo acima do limite retorna 413 em Problem Details e não segue")
    void corpoAcimaDoLimiteRetorna413(String metodo) throws Exception {
        var request  = requisicao(metodo, LIMITE + 1);
        var response = new MockHttpServletResponse();
        var chain    = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentType()).contains("application/problem+json");
        assertThat(response.getContentAsString()).contains("\"status\":413").contains("\"detail\"");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("corpo em Transfer-Encoding, sem Content-Length, retorna 411")
    void corpoSemTamanhoDeclaradoRetorna411() throws Exception {
        var request  = requisicao("POST", -1);
        request.addHeader("Transfer-Encoding", "chunked");
        var response = new MockHttpServletResponse();
        var chain    = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(411);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("POST sem corpo (sem Content-Length nem Transfer-Encoding) segue")
    void postSemCorpoSegue() throws Exception {
        var chain = new MockFilterChain();

        filter.doFilterInternal(requisicao("POST", -1), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("GET não é verificado, mesmo com Content-Length grande")
    void getNaoEhVerificado() throws Exception {
        var chain = new MockFilterChain();

        filter.doFilterInternal(requisicao("GET", LIMITE * 10), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    private static MockHttpServletRequest requisicao(String metodo, long contentLength) {
        var request = new MockHttpServletRequest(metodo, "/api/v1/ordens-producao");
        if (contentLength >= 0) {
            request.setContent(new byte[(int) Math.min(contentLength, 1_000)]);
            request.addHeader("Content-Length", contentLength);
        }
        return request;
    }
}
