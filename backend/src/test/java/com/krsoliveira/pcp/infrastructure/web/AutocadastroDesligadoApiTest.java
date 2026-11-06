package com.krsoliveira.pcp.infrastructure.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Com o autocadastro desligado, como em produção (ADR-0031), conta nova só nasce pelas mãos de um gestor. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.admin.nome=Administrador de Teste",
                "app.admin.email=admin@pcp.com",
                "app.admin.senha=Senha@123",
                "pcp.seguranca.rate-limit.login=1000",
                "pcp.seguranca.rate-limit.registrar=1000",
                "pcp.seguranca.autocadastro-habilitado=false"
        })
@Testcontainers
@SuppressWarnings({"unchecked", "rawtypes"})
@AutoConfigureTestRestTemplate
class AutocadastroDesligadoApiTest {

    private static final String MENSAGEM = "O cadastro público está desligado. Peça o seu acesso a um gestor.";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DisplayName("cadastro público responde 403 e a conta não é criada")
    void recusaOCadastro() {
        ResponseEntity<Map> resposta = registrar("nova@teste.local");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("detail", MENSAGEM);
        assertThat(login("nova@teste.local", "Senha@12345").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("a resposta é a mesma para e-mail já cadastrado: não dá para descobrir quem tem conta")
    void naoRevelaQuemTemConta() {
        ResponseEntity<Map> resposta = registrar("admin@pcp.com");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("detail", MENSAGEM);
    }

    @Test
    @DisplayName("a resposta é a mesma para senha fora da política: não valida nada de quem não pode se cadastrar")
    void naoValidaOCorpoDeQuemNaoPodeSeCadastrar() {
        ResponseEntity<Map> resposta = rest.postForEntity("/api/v1/auth/registrar",
                json(Map.of("nome", "Longa", "email", "longa@teste.local", "senha", "é".repeat(60))), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("o login e a criação de usuário por um gestor continuam funcionando")
    void gestorContinuaCriandoUsuarios() {
        String token = (String) login("admin@pcp.com", "Senha@123").getBody().get("token");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        List<Map<String, Object>> perfis = rest.exchange("/api/v1/acessos/perfis", HttpMethod.GET,
                new HttpEntity<>(headers), List.class).getBody();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> criado = rest.exchange("/api/v1/acessos/usuarios", HttpMethod.POST, new HttpEntity<>(Map.of(
                "nome", "Operador", "email", "operador@teste.local", "senhaProvisoria", "Senha@12345",
                "perfilId", perfis.get(0).get("id")), headers), Map.class);

        assertThat(criado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private ResponseEntity<Map> registrar(String email) {
        return rest.postForEntity("/api/v1/auth/registrar",
                json(Map.of("nome", "Nova Pessoa", "email", email, "senha", "Senha@12345")), Map.class);
    }

    private ResponseEntity<Map> login(String email, String senha) {
        return rest.postForEntity("/api/v1/auth/login", json(Map.of("email", email, "senha", senha)), Map.class);
    }

    private static HttpEntity<Object> json(Object corpo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(corpo, headers);
    }
}
