package com.krsoliveira.pcp.infrastructure.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rate limiting de ponta a ponta, com o servidor real.
 *
 * Todas as chamadas saem do mesmo IP de conexão (loopback). Variar o header
 * X-Forwarded-For — que o cliente controla — não pode dar um bucket novo.
 *
 * Usa o HttpClient do JDK de propósito: o Apache HttpClient 5, que o
 * TestRestTemplate escolhe quando está no classpath, repete respostas 429
 * respeitando o Retry-After (60 s) e mascararia o resultado.
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

    @LocalServerPort
    private int porta;

    private final HttpClient cliente = HttpClient.newHttpClient();

    @Test
    @DisplayName("login: X-Forwarded-For forjado não burla o limite e o 429 traz Retry-After")
    void loginComIpForjadoRecebe429() throws Exception {
        for (int i = 0; i < 2; i++) {
            assertThat(login("198.51.100." + i).statusCode()).isEqualTo(401);
        }

        HttpResponse<String> bloqueada = login("198.51.100.250");

        assertThat(bloqueada.statusCode()).isEqualTo(429);
        assertThat(bloqueada.headers().firstValue("Retry-After")).isPresent();
        assertThat(bloqueada.body()).contains("\"status\":429").contains("\"detail\"");
    }

    @Test
    @DisplayName("o 429 também traz os cabeçalhos CORS, senão o navegador o descarta")
    void respostaBloqueadaTemCors() throws Exception {
        for (int i = 0; i < 2; i++) {
            login("198.51.100." + i);
        }

        HttpResponse<String> bloqueada = login("198.51.100.251");

        assertThat(bloqueada.statusCode()).isEqualTo(429);
        assertThat(bloqueada.headers().firstValue("Access-Control-Allow-Origin"))
                .contains("http://localhost:3000");
        assertThat(bloqueada.headers().firstValue("Access-Control-Expose-Headers"))
                .hasValueSatisfying(expostos -> assertThat(expostos).contains("Retry-After"));
    }

    private HttpResponse<String> login(String forwardedFor) throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", forwardedFor)
                .header("Origin", "http://localhost:3000")
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"ninguem@pcp.com\",\"senha\":\"Senha@123\"}"))
                .build();
        return cliente.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }
}
