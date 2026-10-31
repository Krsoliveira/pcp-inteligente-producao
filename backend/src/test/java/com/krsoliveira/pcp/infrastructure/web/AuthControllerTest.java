package com.krsoliveira.pcp.infrastructure.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração de ponta a ponta para os endpoints de autenticação.
 *
 * Cada teste é independente — não há estado compartilhado entre eles.
 * E-mails únicos por método evitam colisão no banco.
 *
 * Todas as requisições saem do mesmo IP (loopback), então os limites de rate
 * limiting são elevados aqui; o comportamento do limite tem teste próprio em
 * {@link RateLimitingIntegracaoTest}.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "pcp.seguranca.rate-limit.login=1000",
                "pcp.seguranca.rate-limit.registrar=1000"
        })
@Testcontainers
@AutoConfigureTestRestTemplate
class AuthControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private TestRestTemplate rest;

    // -------------------------------------------------------------------------
    // POST /api/v1/auth/registrar
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("registrar: cadastro válido retorna 201 sem corpo")
    void registrarValido() {
        ResponseEntity<Void> response = rest.postForEntity(
                "/api/v1/auth/registrar",
                json(Map.of("nome", "Alice", "email", "alice@pcp.com", "senha", "Senha@123")),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("toda resposta leva os cabeçalhos de segurança (Permissions-Policy, Referrer-Policy, nosniff)")
    void cabecalhosDeSeguranca() {
        ResponseEntity<Void> response = rest.postForEntity(
                "/api/v1/auth/registrar",
                json(Map.of("nome", "Carla", "email", "carla@pcp.com", "senha", "Senha@123")),
                Void.class);

        HttpHeaders cabecalhos = response.getHeaders();
        assertThat(cabecalhos.getFirst("Permissions-Policy"))
                .isEqualTo("camera=(), microphone=(), geolocation=(), payment=()");
        assertThat(cabecalhos.getFirst("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");
        assertThat(cabecalhos.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
    }

    @Test
    @DisplayName("registrar: e-mail duplicado retorna 409")
    void registrarEmailDuplicado() {
        Map<String, Object> corpo = Map.of("nome", "Bob", "email", "bob@pcp.com", "senha", "Senha@123");
        rest.postForEntity("/api/v1/auth/registrar", json(corpo), Void.class);

        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/registrar", json(corpo), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("registrar: campo obrigatório ausente retorna 400")
    void registrarCampoAusente() {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/registrar",
                json(Map.of("email", "sem-nome@pcp.com", "senha", "Senha@123")),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("registrar: senha com menos de 8 caracteres retorna 400")
    void registrarSenhaCurta() {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/registrar",
                json(Map.of("nome", "Carlos", "email", "carlos@pcp.com", "senha", "123")),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("registrar: e-mail malformado retorna 400")
    void registrarEmailMalformado() {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/registrar",
                json(Map.of("nome", "Diana", "email", "nao-e-email", "senha", "Senha@123")),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/auth/login
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("login: credenciais corretas retornam 200 com token JWT")
    void loginCredenciaisCorretas() {
        rest.postForEntity("/api/v1/auth/registrar",
                json(Map.of("nome", "Eva", "email", "eva@pcp.com", "senha", "Senha@123")),
                Void.class);

        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/login",
                json(Map.of("email", "eva@pcp.com", "senha", "Senha@123")),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("token");
        assertThat((String) response.getBody().get("token")).isNotBlank();
    }

    @Test
    @DisplayName("login: senha incorreta retorna 401")
    void loginSenhaIncorreta() {
        rest.postForEntity("/api/v1/auth/registrar",
                json(Map.of("nome", "Frank", "email", "frank@pcp.com", "senha", "Senha@123")),
                Void.class);

        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/login",
                json(Map.of("email", "frank@pcp.com", "senha", "SenhaErrada@1")),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("login: usuário inexistente retorna 401")
    void loginUsuarioInexistente() {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/login",
                json(Map.of("email", "ninguem@pcp.com", "senha", "Senha@123")),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("login: campo obrigatório ausente retorna 400")
    void loginCampoAusente() {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/login",
                json(Map.of("email", "alguem@pcp.com")),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private HttpEntity<Object> json(Object corpo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(corpo, headers);
    }
}
