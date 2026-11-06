package com.krsoliveira.pcp.infrastructure.web;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contrato das respostas de erro (RFC 9457) de {@code /api/v1}: todo erro traz {@code type}, {@code title},
 * {@code status}, {@code detail} e {@code instance}. O contrato só cresce — no Spring Framework 7 o {@code type}
 * passou a ser omitido quando vale {@code about:blank}, e este teste impede que um campo suma de novo (ADR-0030).
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.admin.nome=Administrador de Teste",
                "app.admin.email=admin@pcp.com",
                "app.admin.senha=Senha@123",
                "pcp.seguranca.rate-limit.login=1000",
                "pcp.seguranca.rate-limit.registrar=1000"
        })
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings({"unchecked", "rawtypes"})
@AutoConfigureTestRestTemplate
class FormatoDosErrosApiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private TestRestTemplate rest;

    private String token;

    @BeforeAll
    void entrar() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        token = (String) rest.postForEntity("/api/v1/auth/login",
                new HttpEntity<>(Map.of("email", "admin@pcp.com", "senha", "Senha@123"), headers), Map.class)
                .getBody().get("token");
    }

    @Test
    @DisplayName("404: recurso não encontrado traz todos os campos do RFC 9457, com type about:blank")
    void naoEncontrado() {
        Map<String, Object> erro = chamar(HttpMethod.GET, "/api/v1/ordens-producao/00000000-0000-0000-0000-000000000000", null);

        assertThat(erro).containsEntry("type", "about:blank").containsEntry("status", 404)
                .containsEntry("title", "Recurso não encontrado")
                .containsEntry("instance", "/api/v1/ordens-producao/00000000-0000-0000-0000-000000000000")
                .containsKey("detail");
    }

    @Test
    @DisplayName("422: regra de negócio violada traz todos os campos do RFC 9457, com type about:blank")
    void regraDeNegocio() {
        Map<String, Object> erro = chamar(HttpMethod.GET, "/api/v1/relatorios/consumo-de-ordens?de=2026-09-30&ate=2026-07-01", null);

        assertThat(erro).containsEntry("type", "about:blank").containsEntry("status", 422)
                .containsEntry("title", "Regra de negócio violada")
                .containsEntry("detail", "O fim do período não pode ser anterior ao início.")
                .containsEntry("instance", "/api/v1/relatorios/consumo-de-ordens");
    }

    @Test
    @DisplayName("400: corpo inválido traz os campos do RFC 9457 e a lista de erros por campo")
    void validacao() {
        Map<String, Object> erro = chamar(HttpMethod.POST, "/api/v1/materiais", Map.of());

        assertThat(erro).containsEntry("type", "about:blank").containsEntry("status", 400)
                .containsKeys("title", "detail", "instance", "erros");
    }

    @Test
    @DisplayName("400: identificador que não é UUID traz o RFC 9457, sem repetir o valor enviado, e mantém os campos antigos")
    void identificadorInvalido() {
        ResponseEntity<Map> resposta = chamarComResposta(HttpMethod.GET, "/api/v1/ordens-producao/nao-e-um-uuid", null);

        assertErroDoFramework(resposta, 400, "Requisição inválida", "/api/v1/ordens-producao/nao-e-um-uuid");
        assertThat(resposta.getBody()).containsEntry("detail", "O valor informado para 'id' é inválido.");
        assertThat(resposta.getBody().get("detail").toString()).doesNotContain("nao-e-um-uuid");
    }

    @Test
    @DisplayName("400: valor fora da lista num parâmetro traz o RFC 9457 e mantém os campos antigos")
    void parametroComValorInvalido() {
        ResponseEntity<Map> resposta = chamarComResposta(HttpMethod.GET, "/api/v1/ordens-producao/pesquisa?status=INEXISTENTE", null);

        assertErroDoFramework(resposta, 400, "Requisição inválida", "/api/v1/ordens-producao/pesquisa");
        assertThat(resposta.getBody()).containsEntry("detail", "O valor informado para 'status' é inválido.");
    }

    @Test
    @DisplayName("400: parâmetro obrigatório ausente traz o RFC 9457 e mantém os campos antigos")
    void parametroAusente() {
        ResponseEntity<Map> resposta = chamarComResposta(HttpMethod.GET, "/api/v1/relatorios/consumo-de-ordens", null);

        assertErroDoFramework(resposta, 400, "Requisição inválida", "/api/v1/relatorios/consumo-de-ordens");
        assertThat(resposta.getBody()).containsEntry("detail", "O parâmetro 'de' é obrigatório.");
    }

    @Test
    @DisplayName("400: JSON malformado traz o RFC 9457 e mantém os campos antigos")
    void jsonMalformado() {
        ResponseEntity<Map> resposta = chamarComResposta(HttpMethod.POST, "/api/v1/materiais", "{isto nao e json");

        assertErroDoFramework(resposta, 400, "Requisição inválida", "/api/v1/materiais");
        assertThat(resposta.getBody()).containsEntry("detail", "O corpo da requisição não pôde ser lido. Confira o formato dos dados.");
    }

    @Test
    @DisplayName("405: método não permitido traz o RFC 9457 e mantém os campos antigos")
    void metodoNaoPermitido() {
        ResponseEntity<Map> resposta = chamarComResposta(HttpMethod.DELETE, "/api/v1/materiais", null);

        assertErroDoFramework(resposta, 405, "Método não permitido", "/api/v1/materiais");
        assertThat(resposta.getBody()).containsEntry("detail", "Este método não é permitido neste endereço.");
    }

    @Test
    @DisplayName("404: endereço que não existe traz o RFC 9457 e mantém os campos antigos")
    void enderecoInexistente() {
        ResponseEntity<Map> resposta = chamarComResposta(HttpMethod.GET, "/api/v1/nao-existe", null);

        assertErroDoFramework(resposta, 404, "Recurso não encontrado", "/api/v1/nao-existe");
        assertThat(resposta.getBody()).containsEntry("detail", "O endereço pedido não existe.");
    }

    /** Campos do RFC 9457 e, como o contrato só cresce, os que o erro padrão do Spring já devolvia. */
    private static void assertErroDoFramework(ResponseEntity<Map> resposta, int status, String titulo, String caminho) {
        assertThat(resposta.getStatusCode().value()).isEqualTo(status);
        assertThat(resposta.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(resposta.getBody()).containsEntry("type", "about:blank").containsEntry("status", status)
                .containsEntry("title", titulo).containsEntry("instance", caminho).containsKey("detail")
                .containsEntry("path", caminho).containsKeys("timestamp", "error");
    }

    private Map<String, Object> chamar(HttpMethod metodo, String caminho, Object corpo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        ResponseEntity<Map> resposta = rest.exchange(caminho, metodo, new HttpEntity<>(corpo, headers), Map.class);
        return resposta.getBody();
    }

    private ResponseEntity<Map> chamarComResposta(HttpMethod metodo, String caminho, Object corpo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return rest.exchange(caminho, metodo, new HttpEntity<>(corpo, headers), Map.class);
    }
}
