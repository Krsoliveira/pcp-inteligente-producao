package com.krsoliveira.pcp.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @DisplayName("corpo chunked dentro do limite segue e pode ser lido por inteiro")
    void corpoChunkedDentroDoLimiteEhLegivel() throws Exception {
        var request = requisicaoChunked(new byte[(int) LIMITE]);
        var chain   = new MockFilterChain();

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(chain.getRequest().getInputStream().readAllBytes()).hasSize((int) LIMITE);
    }

    @Test
    @DisplayName("corpo chunked acima do limite lança CorpoGrandeDemaisException ao ler")
    void corpoChunkedAcimaDoLimiteLancaAoLer() throws Exception {
        var request = requisicaoChunked(new byte[(int) LIMITE + 1]);
        var chain   = new MockFilterChain();

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        var entrada = chain.getRequest().getInputStream();
        assertThatThrownBy(entrada::readAllBytes).isInstanceOf(CorpoGrandeDemaisException.class);
    }

    @Test
    @DisplayName("corpo chunked acima do limite também é barrado ao ler pelo Reader")
    void corpoChunkedAcimaDoLimiteLancaPeloReader() throws Exception {
        var request = requisicaoChunked("a".repeat((int) LIMITE + 1).getBytes());
        var chain   = new MockFilterChain();

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        var leitor = chain.getRequest().getReader();
        assertThatThrownBy(() -> leitor.read(new char[(int) LIMITE + 1]))
                .isInstanceOf(CorpoGrandeDemaisException.class);
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

    /** Corpo sem Content-Length declarado, como em Transfer-Encoding: chunked. */
    private static MockHttpServletRequest requisicaoChunked(byte[] corpo) {
        var request = new MockHttpServletRequest("POST", "/api/v1/ordens-producao") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        request.setContent(corpo);
        request.addHeader("Transfer-Encoding", "chunked");
        return request;
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
