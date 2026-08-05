package com.krsoliveira.pcp;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de integração de ponta a ponta: sobe a aplicação inteira e um
 * PostgreSQL REAL via Testcontainers (o Flyway aplica as migrações V1 e V2).
 *
 * {@code @ServiceConnection} conecta o datasource do Spring ao container
 * automaticamente — sem configurar URL/usuário/senha à mão.
 *
 * {@code @TestInstance(PER_CLASS)} permite que {@code @BeforeAll} seja não-estático
 * e use {@code @Autowired}, necessário para registrar e autenticar um usuário
 * antes de executar os testes protegidos por JWT.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrdemProducaoApiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate rest;

    private static final String BASE = "/api/v1/ordens-producao";
    private String jwtToken;
    private String idCriado;

    @BeforeAll
    void autenticar() {
        Map<String, Object> registrar = Map.of(
                "nome", "Planejador Teste",
                "email", "teste@pcp.com",
                "senha", "Senha@123",
                "perfil", "PLANEJADOR");
        rest.postForEntity("/api/v1/auth/registrar", jsonSemAuth(registrar), Map.class);

        ResponseEntity<Map> login = rest.postForEntity(
                "/api/v1/auth/login",
                jsonSemAuth(Map.of("email", "teste@pcp.com", "senha", "Senha@123")),
                Map.class);

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        jwtToken = (String) login.getBody().get("token");
        assertThat(jwtToken).isNotBlank();
    }

    /** Requisição autenticada com JWT Bearer. */
    private HttpEntity<Map<String, Object>> json(Map<String, Object> corpo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken);
        return new HttpEntity<>(corpo, headers);
    }

    /** Requisição sem autenticação (usada nos endpoints públicos de auth). */
    private HttpEntity<Map<String, Object>> jsonSemAuth(Map<String, Object> corpo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(corpo, headers);
    }

    private Map<String, Object> ordemValida(String codigo) {
        return Map.of(
                "codigo", codigo,
                "produto", "Viga metálica 6m",
                "centroDeTrabalho", "Usinagem CNC",
                "quantidade", 100,
                "inicioPlanejado", "2026-08-10",
                "fimPlanejado", "2026-08-20");
    }

    @Test
    @Order(1)
    @DisplayName("POST cria ordem e devolve 201 com Location")
    @SuppressWarnings("unchecked")
    void criaOrdem() {
        ResponseEntity<Map> resposta = rest.postForEntity(BASE, json(ordemValida("OP-0001")), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getHeaders().getLocation()).isNotNull();
        Map<String, Object> corpo = resposta.getBody();
        assertThat(corpo.get("status")).isEqualTo("PLANEJADA");
        assertThat(corpo.get("codigo")).isEqualTo("OP-0001");
        idCriado = (String) corpo.get("id");
    }

    @Test
    @Order(2)
    @DisplayName("GET por id devolve a ordem criada")
    @SuppressWarnings("unchecked")
    void buscaPorId() {
        ResponseEntity<Map> resposta = rest.exchange(
                BASE + "/" + idCriado, HttpMethod.GET, json(null), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody().get("codigo")).isEqualTo("OP-0001");
    }

    @Test
    @Order(3)
    @DisplayName("POST com código duplicado devolve 409")
    void rejeitaCodigoDuplicado() {
        ResponseEntity<Map> resposta = rest.postForEntity(BASE, json(ordemValida("OP-0001")), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @Order(4)
    @DisplayName("PATCH avança o status seguindo a máquina de estados")
    @SuppressWarnings("unchecked")
    void avancaStatus() {
        ResponseEntity<Map> resposta = rest.exchange(BASE + "/" + idCriado + "/status",
                HttpMethod.PATCH, json(Map.of("status", "LIBERADA")), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody().get("status")).isEqualTo("LIBERADA");
    }

    @Test
    @Order(5)
    @DisplayName("PATCH com transição inválida devolve 422")
    void rejeitaTransicaoInvalida() {
        // Ordem está LIBERADA; concluir sem produzir é proibido pelo domínio.
        ResponseEntity<Map> resposta = rest.exchange(BASE + "/" + idCriado + "/status",
                HttpMethod.PATCH, json(Map.of("status", "CONCLUIDA")), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @Order(6)
    @DisplayName("POST com corpo inválido devolve 400 com os campos errados")
    @SuppressWarnings("unchecked")
    void rejeitaCorpoInvalido() {
        Map<String, Object> corpoInvalido = Map.of("produto", "Viga", "quantidade", -5);

        ResponseEntity<Map> resposta = rest.postForEntity(BASE, json(corpoInvalido), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, Object> erros = (Map<String, Object>) resposta.getBody().get("erros");
        assertThat(erros).containsKeys("codigo", "quantidade");
    }

    @Test
    @Order(7)
    @DisplayName("Requisição sem token devolve 401")
    void semTokenDevolve401() {
        ResponseEntity<Map> resposta = rest.getForEntity(BASE, Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(8)
    @DisplayName("Login com credenciais erradas devolve 401")
    void loginComSenhaErradaDevolve401() {
        ResponseEntity<Map> resposta = rest.postForEntity(
                "/api/v1/auth/login",
                jsonSemAuth(Map.of("email", "teste@pcp.com", "senha", "senhaErrada")),
                Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
