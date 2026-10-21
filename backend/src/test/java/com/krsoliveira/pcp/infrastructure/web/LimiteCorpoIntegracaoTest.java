package com.krsoliveira.pcp.infrastructure.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Limite de corpo de ponta a ponta. O cliente do teste envia JSON sem
 * Content-Length (chunked) ou com ele, conforme a versão do cliente HTTP;
 * nos dois casos o corpo acima do limite precisa ser recusado com 413.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "pcp.seguranca.corpo-maximo-bytes=300",
                "pcp.seguranca.rate-limit.login=1000",
                "pcp.seguranca.rate-limit.registrar=1000"
        })
@Testcontainers
class LimiteCorpoIntegracaoTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DisplayName("corpo JSON acima do limite retorna 413 em Problem Details")
    void corpoAcimaDoLimiteRetorna413() {
        ResponseEntity<Map> response = login("x".repeat(1_000));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).containsEntry("status", 413).containsKey("detail");
    }

    @Test
    @DisplayName("corpo JSON dentro do limite é processado normalmente")
    void corpoDentroDoLimiteEhProcessado() {
        ResponseEntity<Map> response = login("Senha@123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<Map> login(String senha) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity(
                "/api/v1/auth/login",
                new HttpEntity<>(Map.of("email", "ninguem@pcp.com", "senha", senha), headers),
                Map.class);
    }
}
