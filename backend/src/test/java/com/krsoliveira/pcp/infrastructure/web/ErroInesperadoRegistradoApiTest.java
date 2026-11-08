package com.krsoliveira.pcp.infrastructure.web;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.krsoliveira.pcp.BancoDeTesteIsolado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Erro inesperado (500): a causa vai para o log com o {@code requestId} da requisição, e a resposta não repete o
 * detalhe interno. Quem investiga parte do {@code X-Request-ID} que a resposta devolve.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "pcp.seguranca.rate-limit.login=1000",
                "pcp.seguranca.rate-limit.registrar=1000"
        })
@BancoDeTesteIsolado
@AutoConfigureTestRestTemplate
@Import(ErroInesperadoRegistradoApiTest.EndpointQueFalha.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class ErroInesperadoRegistradoApiTest {

    private static final String DETALHE_INTERNO = "detalhe-interno-que-nao-pode-vazar";

    /** Só existe neste teste: importado por @Import, não entra na aplicação nem nos outros testes. */
    @RestController
    static class EndpointQueFalha {
        @GetMapping("/api/v1/teste-erro-inesperado")
        String falhar() {
            throw new IllegalStateException(DETALHE_INTERNO);
        }
    }

    @Autowired
    private TestRestTemplate rest;

    private final ListAppender<ILoggingEvent> registros = new ListAppender<>();
    private Logger raiz;

    @BeforeEach
    void ouvirOLog() {
        raiz = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        registros.start();
        raiz.addAppender(registros);
    }

    @AfterEach
    void pararDeOuvir() {
        raiz.detachAppender(registros);
        registros.stop();
    }

    @Test
    @DisplayName("500: a causa é registrada com o requestId, e a resposta não vaza o detalhe")
    void erroInesperadoFicaNoLog() {
        rest.postForEntity("/api/v1/auth/registrar", json(Map.of("nome", "Erro", "email", "erro@teste.local", "senha", "Senha@123")), Void.class);
        String token = (String) rest.postForEntity("/api/v1/auth/login",
                json(Map.of("email", "erro@teste.local", "senha", "Senha@123")), Map.class).getBody().get("token");
        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.setBearerAuth(token);

        ResponseEntity<String> resposta = rest.exchange("/api/v1/teste-erro-inesperado", HttpMethod.GET,
                new HttpEntity<>(cabecalhos), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody()).doesNotContain(DETALHE_INTERNO);
        String requestId = resposta.getHeaders().getFirst("X-Request-ID");
        assertThat(requestId).isNotBlank();
        assertThat(registros.list).anySatisfy(evento -> {
            assertThat(evento.getLevel().toString()).isEqualTo("ERROR");
            assertThat(evento.getThrowableProxy()).isNotNull();
            assertThat(evento.getThrowableProxy().getMessage()).contains(DETALHE_INTERNO);
            assertThat(evento.getMDCPropertyMap()).containsEntry("requestId", requestId);
        });
    }

    private static HttpEntity<Map<String, Object>> json(Map<String, Object> corpo) {
        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(corpo, cabecalhos);
    }
}
