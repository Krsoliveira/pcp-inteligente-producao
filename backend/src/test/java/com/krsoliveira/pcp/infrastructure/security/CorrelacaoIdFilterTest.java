package com.krsoliveira.pcp.infrastructure.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest(name = "descarta X-Request-ID inválido: [{0}]")
    @ValueSource(strings = {"id\r\nFORJADO: linha", "com espaço", "<script>", "ação", "   "})
    @DisplayName("descarta X-Request-ID com caracteres inválidos e gera outro")
    void descartaIdInvalido(String invalido) throws Exception {
        var request  = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        request.addHeader(CorrelacaoIdFilter.HEADER, invalido);

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelacaoIdFilter.HEADER))
                .isNotEqualTo(invalido)
                .matches("[a-f0-9]{12}");
    }

    @Test
    @DisplayName("descarta X-Request-ID acima de 64 caracteres")
    void descartaIdLongoDemais() throws Exception {
        var request  = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        request.addHeader(CorrelacaoIdFilter.HEADER, "a".repeat(65));

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelacaoIdFilter.HEADER)).hasSize(12);
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