package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.BancoDeTesteIsolado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** O health check é público para o provedor de hospedagem, e não pode abrir mais nada do Actuator. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@BancoDeTesteIsolado
@AutoConfigureTestRestTemplate
@SuppressWarnings("rawtypes")
class HealthCheckApiTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DisplayName("sem login, responde que está no ar e não conta detalhes da infraestrutura")
    void healthPublicoSemDetalhes() {
        ResponseEntity<Map> resposta = rest.getForEntity("/actuator/health", Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("status", "UP").doesNotContainKeys("components", "details");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator", "/actuator/env", "/actuator/beans", "/actuator/metrics", "/actuator/health/db", "/actuator/heapdump"})
    @DisplayName("o resto do Actuator continua fechado")
    void restoFechado(String caminho) {
        assertThat(rest.getForEntity(caminho, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
