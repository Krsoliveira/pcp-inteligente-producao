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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração de ponta a ponta para os endpoints de autenticação.
 *
 * Cada teste é independente — não há estado compartilhado entre eles.
 * E-mails únicos por método evitam colisão no banco.
 *
 * O header X-Forwarded-For usa um IP sintético único por teste para não
 * esgotar o bucket de rate limiting compartilhado por todos os métodos.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate rest;

    /** Contador por instância: gera IPs únicos como 10.0.TEST_INDEX.COUNTER. */
    private static final AtomicInteger IP_COUNTER = new AtomicInteger(1);

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
    @DisplayName("registrar: e-mail duplicado retorna 409")
    void registrarEmailDuplicado() {
        String ip = proximoIp();
        Map<String, Object> corpo = Map.of("nome", "Bob", "email", "bob@pcp.com", "senha", "Senha@123");
        rest.postForEntity("/api/v1/auth/registrar", jsonComIp(corpo, ip), Void.class);

        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/registrar", jsonComIp(corpo, proximoIp()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("registrar: campo obrigatório ausente retorna 400")
    void registrarCampoAusente() {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/registrar",
                jsonComIp(Map.of("email", "sem-nome@pcp.com", "senha", "Senha@123"), proximoIp()),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("registrar: senha com menos de 8 caracteres retorna 400")
    void registrarSenhaCurta() {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/registrar",
                jsonComIp(Map.of("nome", "Carlos", "email", "carlos@pcp.com", "senha", "123"), proximoIp()),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("registrar: e-mail malformado retorna 400")
    void registrarEmailMalformado() {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/registrar",
                jsonComIp(Map.of("nome", "Diana", "email", "nao-e-email", "senha", "Senha@123"), proximoIp()),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/auth/login
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("login: credenciais corretas retornam 200 com token JWT")
    void loginCredenciaisCorretas() {
        String ip = proximoIp();
        rest.postForEntity("/api/v1/auth/registrar",
                jsonComIp(Map.of("nome", "Eva", "email", "eva@pcp.com", "senha", "Senha@123"), ip),
                Void.class);

        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/login",
                jsonComIp(Map.of("email", "eva@pcp.com", "senha", "Senha@123"), ip),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("token");
        assertThat((String) response.getBody().get("token")).isNotBlank();
    }

    @Test
    @DisplayName("login: senha incorreta retorna 401")
    void loginSenhaIncorreta() {
        String ip = proximoIp();
        rest.postForEntity("/api/v1/auth/registrar",
                jsonComIp(Map.of("nome", "Frank", "email", "frank@pcp.com", "senha", "Senha@123"), ip),
                Void.class);

        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/login",
                jsonComIp(Map.of("email", "frank@pcp.com", "senha", "SenhaErrada@1"), ip),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("login: usuário inexistente retorna 401")
    void loginUsuarioInexistente() {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/login",
                jsonComIp(Map.of("email", "ninguem@pcp.com", "senha", "Senha@123"), proximoIp()),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("login: campo obrigatório ausente retorna 400")
    void loginCampoAusente() {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/login",
                jsonComIp(Map.of("email", "alguem@pcp.com"), proximoIp()),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** IP único por chamada — evita esgotamento de bucket de rate limiting entre testes. */
    private static String proximoIp() {
        int n = IP_COUNTER.getAndIncrement();
        return "10.99." + (n / 255) + "." + (n % 255 + 1);
    }

    private HttpEntity<Object> json(Object corpo) {
        return jsonComIp(corpo, proximoIp());
    }

    private HttpEntity<Object> jsonComIp(Object corpo, String ip) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Forwarded-For", ip);
        return new HttpEntity<>(corpo, headers);
    }
}