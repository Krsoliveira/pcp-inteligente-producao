package com.krsoliveira.pcp.infrastructure.security;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class TempoDeRespostaFilterTest {

    private static final long LIMITE_EM_MILISSEGUNDOS = 50;

    private final ListAppender<ILoggingEvent> registros = new ListAppender<>();
    private Logger log;

    @BeforeEach
    void ouvirOLog() {
        log = (Logger) LoggerFactory.getLogger("pcp.desempenho");
        registros.start();
        log.addAppender(registros);
    }

    @AfterEach
    void pararDeOuvir() {
        log.detachAppender(registros);
        registros.stop();
    }

    @Test
    @DisplayName("requisição lenta é registrada com método, rota, status e duração")
    void lentaEhRegistrada() throws Exception {
        executar("GET", "/api/v1/ordens", 200, LIMITE_EM_MILISSEGUNDOS + 30);

        assertThat(registros.list).singleElement().satisfies(evento -> {
            assertThat(evento.getLevel().toString()).isEqualTo("WARN");
            assertThat(evento.getFormattedMessage())
                    .startsWith("requisicao_lenta requisicao=GET /api/v1/ordens status=200 duracaoMs=");
        });
    }

    @Test
    @DisplayName("requisição rápida não gera linha de log")
    void rapidaNaoEhRegistrada() throws Exception {
        executar("GET", "/api/v1/ordens", 200, 0);

        assertThat(registros.list).isEmpty();
    }

    @Test
    @DisplayName("o health check não é medido")
    void healthCheckNaoEhMedido() throws Exception {
        executar("GET", "/actuator/health", 200, LIMITE_EM_MILISSEGUNDOS + 30);

        assertThat(registros.list).isEmpty();
    }

    @Test
    @DisplayName("parâmetros de consulta não vão para o log")
    void parametrosDeConsultaNaoVaoParaOLog() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/materiais");
        request.setQueryString("busca=dado.pessoal@empresa.com");

        executar(request, 200, LIMITE_EM_MILISSEGUNDOS + 30);

        assertThat(registros.list).singleElement()
                .satisfies(evento -> assertThat(evento.getFormattedMessage()).doesNotContain("dado.pessoal", "busca"));
    }

    @Test
    @DisplayName("registra também quando a requisição termina em erro")
    void lentaComErroEhRegistrada() throws Exception {
        var filtro = new TempoDeRespostaFilter(LIMITE_EM_MILISSEGUNDOS);
        FilterChain falha = (req, res) -> {
            esperar(LIMITE_EM_MILISSEGUNDOS + 30);
            throw new IllegalStateException("falha");
        };

        try {
            filtro.doFilter(new MockHttpServletRequest("POST", "/api/v1/ordens"), new MockHttpServletResponse(), falha);
        } catch (IllegalStateException esperada) {
            // a exceção segue adiante; o que interessa é o log
        }

        assertThat(registros.list).hasSize(1);
    }

    private void executar(String metodo, String rota, int status, long demoraEmMilissegundos) throws Exception {
        executar(new MockHttpServletRequest(metodo, rota), status, demoraEmMilissegundos);
    }

    private void executar(MockHttpServletRequest request, int status, long demoraEmMilissegundos) throws Exception {
        var filtro = new TempoDeRespostaFilter(LIMITE_EM_MILISSEGUNDOS);
        var resposta = new MockHttpServletResponse();
        FilterChain cadeia = (req, res) -> {
            esperar(demoraEmMilissegundos);
            ((MockHttpServletResponse) res).setStatus(status);
        };
        filtro.doFilter(request, resposta, cadeia);
    }

    private static void esperar(long milissegundos) {
        try {
            Thread.sleep(milissegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
