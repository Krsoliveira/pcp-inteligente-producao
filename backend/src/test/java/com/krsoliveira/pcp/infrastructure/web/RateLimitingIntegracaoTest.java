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
 * Rate limiting de ponta a ponta, com o servidor real.
 *
 * Todas as chamadas saem do mesmo IP de conexão (loopback). Variar o header
 * X-Forwarded-For — que o cliente controla — não pode dar um bucket novo.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "pcp.seguranca.rate-limit.login=2",
                "pcp.seguranca.rate-limit.registrar=1000"
        })
@Testcontainers
class RateLimitingIntegracaoTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DisplayName("login: X-Forwarded-For forjado não burla o limite e o 429 traz Retry-After")
    void loginComIpForjadoRecebe429() {
        for (int i = 0; i < 2; i++) {
            ResponseEntity<Map> tentativa = login("198.51.100." + i);
            assertThat(tentativa.getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }

        ResponseEntity<Map> bloqueada = login("198.51.100.250");

        assertThat(bloqueada.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(bloqueada.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isNotBlank();
    }

    private ResponseEntity<Map> login(String forwardedFor) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Forwarded-For", forwardedFor);
        return rest.postForEntity(
                "/api/v1/auth/login",
                new HttpEntity<>(Map.of("email", "ninguem@pcp.com", "senha", "Senha@123"), headers),
                Map.class);
    }
}
