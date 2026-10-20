package com.krsoliveira.pcp.infrastructure.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelacaoIdFilterTest {

    private CorrelacaoIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelacaoIdFilter();
        MDC.clear();
    }

    @Test
    @DisplayName("gera X-Request-ID quando o cliente não envia o header")
    void geraIdQuandoHeaderAusente() throws Exception {
        var request  = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain    = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getHeader(CorrelacaoIdFilter.HEADER))
                .isNotBlank()
                .hasSize(12);
    }

    @Test
    @DisplayName("reutiliza o X-Request-ID enviado pelo cliente")
    void reutilizaIdDoCliente() throws Exception {
        var request  = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain    = new MockFilterChain();
        request.addHeader(CorrelacaoIdFilter.HEADER, "meu-id-externo");

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getHeader(CorrelacaoIdFilter.HEADER)).isEqualTo("meu-id-externo");
    }

    @Test
    @DisplayName("injeta o requestId no MDC durante a execução do filtro")
    void injetaIdNoMdc() throws Exception {
        var request  = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var idCapturado = new String[1];

        FilterChain chainQueCaptura = (req, res) -> idCapturado[0] = MDC.get(CorrelacaoIdFilter.MDC_KEY);

        filter.doFilterInternal(request, response, chainQueCaptura);

        assertThat(idCapturado[0]).isNotBlank();
    }

    @Test
    @DisplayName("remove o requestId do MDC após a execução")
    void removeMdcAposExecucao() throws Exception {
        var request  = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain    = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(MDC.get(CorrelacaoIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("requisições diferentes recebem IDs diferentes")
    void requisicoesDiferentesRecebeIdsDiferentes() throws Exception {
        var response1 = new MockHttpServletResponse();
        var response2 = new MockHttpServletResponse();

        filter.doFilterInternal(new MockHttpServletRequest(), response1, new MockFilterChain());
        filter.doFilterInternal(new MockHttpServletRequest(), response2, new MockFilterChain());

        assertThat(response1.getHeader(CorrelacaoIdFilter.HEADER))
                .isNotEqualTo(response2.getHeader(CorrelacaoIdFilter.HEADER));
    }
}