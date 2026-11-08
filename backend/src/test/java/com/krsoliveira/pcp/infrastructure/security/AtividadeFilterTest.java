package com.krsoliveira.pcp.infrastructure.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AtividadeFilterTest {

    private final AtividadeFilter filtro = new AtividadeFilter();
    private final ListAppender<ILoggingEvent> registros = new ListAppender<>();
    private Logger log;

    @BeforeEach
    void ouvirOLog() {
        log = (Logger) LoggerFactory.getLogger("pcp.atividade");
        registros.start();
        log.addAppender(registros);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("maria@empresa.com", null, List.of()));
    }

    @AfterEach
    void pararDeOuvir() {
        log.detachAppender(registros);
        registros.stop();
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "{0} é registrado")
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    @DisplayName("requisição que altera dados gera uma linha INFO com usuário mascarado, rota, status e duração")
    void alteracaoEhRegistrada(String metodo) throws Exception {
        executar(metodo, "/api/v1/materiais", 201);

        assertThat(registros.list).singleElement().satisfies(evento -> {
            assertThat(evento.getLevel()).isEqualTo(Level.INFO);
            assertThat(evento.getFormattedMessage())
                    .startsWith("requisicao usuario=m***@empresa.com " + metodo + " /api/v1/materiais status=201 duracaoMs=");
        });
    }

    @Test
    @DisplayName("leitura não gera linha")
    void leituraNaoEhRegistrada() throws Exception {
        executar("GET", "/api/v1/materiais", 200);

        assertThat(registros.list).isEmpty();
    }

    @Test
    @DisplayName("o login tem eventos próprios e não é registrado aqui")
    void loginNaoEhRegistrado() throws Exception {
        executar("POST", "/api/v1/auth/login", 200);

        assertThat(registros.list).isEmpty();
    }

    @Test
    @DisplayName("recusa do cliente (4xx) sai em WARN e erro do servidor (5xx) em ERROR")
    void nivelPeloStatus() throws Exception {
        executar("POST", "/api/v1/consumos", 422);
        executar("POST", "/api/v1/consumos", 500);

        assertThat(registros.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.WARN, Level.ERROR);
    }

    @Test
    @DisplayName("sem usuário identificado, aparece como anônimo")
    void semUsuario() throws Exception {
        SecurityContextHolder.clearContext();

        executar("POST", "/api/v1/materiais", 401);

        assertThat(registros.list).singleElement()
                .satisfies(evento -> assertThat(evento.getFormattedMessage()).contains("usuario=anonimo"));
    }

    @Test
    @DisplayName("parâmetros de consulta e corpo não vão para o log")
    void dadosDoUsuarioNaoVaoParaOLog() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/materiais");
        request.setQueryString("busca=dado.pessoal@empresa.com");
        request.setContent("{\"senha\":\"Segredo@123\"}".getBytes(StandardCharsets.UTF_8));

        filtro.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertThat(registros.list).singleElement().satisfies(evento ->
                assertThat(evento.getFormattedMessage()).doesNotContain("dado.pessoal", "busca", "Segredo"));
    }

    @Test
    @DisplayName("registra também quando a requisição termina em exceção")
    void excecaoEhRegistrada() {
        FilterChain falha = (req, res) -> {
            throw new IllegalStateException("falha");
        };

        try {
            filtro.doFilter(new MockHttpServletRequest("POST", "/api/v1/ordens"), new MockHttpServletResponse(), falha);
        } catch (Exception esperada) {
            // a exceção segue adiante; o que interessa é o log
        }

        assertThat(registros.list).hasSize(1);
    }

    private void executar(String metodo, String rota, int status) throws Exception {
        FilterChain cadeia = (req, res) -> ((MockHttpServletResponse) res).setStatus(status);
        filtro.doFilter(new MockHttpServletRequest(metodo, rota), new MockHttpServletResponse(), cadeia);
    }
}
