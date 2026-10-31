package com.krsoliveira.pcp.infrastructure.web;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tipo de ordem Conversão (ADR-0024), de ponta a ponta com PostgreSQL real: o tipo de sistema nasce na migração V15,
 * não se altera, e é o único que permite consumir produto acabado (numa ordem que produz semiacabado).
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
class TipoOrdemConversaoApiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    private static final String TIPOS = "/api/v1/tipos-ordem";

    @Autowired
    private TestRestTemplate rest;

    private String token;
    private String acabado;
    private String semiacabado;
    private String materiaPrima;
    private String idDaConversao;

    @BeforeAll
    void prepararMassa() {
        token = (String) rest.postForEntity("/api/v1/auth/login",
                semAuth(Map.of("email", "admin@pcp.com", "senha", "Senha@123")), Map.class).getBody().get("token");
        materiaPrima = material("Chapa", "MATERIA_PRIMA", "kg");
        acabado = material("Viga pronta", "PRODUTO_ACABADO", "un");
        semiacabado = material("Viga cortada", "SEMIACABADO", "un");
        idDaConversao = (String) tipos().stream().filter(t -> Boolean.TRUE.equals(t.get("conversao"))).findFirst().orElseThrow().get("id");
    }

    @Test
    @DisplayName("a migração cria o tipo Conversão, e só ele é de conversão")
    void tipoDeSistemaExiste() {
        List<Map<String, Object>> tipos = tipos();

        assertThat(tipos).filteredOn(t -> Boolean.TRUE.equals(t.get("conversao"))).singleElement()
                .satisfies(t -> assertThat(t).containsEntry("nome", "Conversão"));
    }

    @Test
    @DisplayName("tipo criado pelo usuário nunca é de conversão, e o tipo Conversão não pode ser alterado (422)")
    void conversaoNaoSeAltera() {
        ResponseEntity<Map> criado = rest.postForEntity(TIPOS, comAuth(Map.of("nome", "Retrabalho API", "cor", "#c62828")), Map.class);
        assertThat(criado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(criado.getBody()).containsEntry("conversao", false);

        ResponseEntity<Map> alteracao = rest.exchange(TIPOS + "/" + idDaConversao, HttpMethod.PUT,
                comAuth(Map.of("nome", "Outro nome", "cor", "#000000")), Map.class);

        assertThat(alteracao.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(tipos()).anySatisfy(t -> assertThat(t).containsEntry("id", idDaConversao).containsEntry("nome", "Conversão"));
    }

    @Test
    @DisplayName("lista de produto acabado não aceita produto acabado como componente (422); a de semiacabado aceita")
    void listaComAcabado() {
        ResponseEntity<Map> recusada = listaComComponente(acabado, acabado, "v1");
        assertThat(recusada.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);

        assertThat(listaComComponente(semiacabado, acabado, "v1").getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("ordem que consome produto acabado só é criada no tipo Conversão; ordem comum recusa (422)")
    void ordemDeConversao() {
        String lista = idDe(listaComComponente(semiacabado, acabado, "v2"));
        rest.exchange("/api/v1/listas-tecnicas/" + lista + "/ativar", HttpMethod.PATCH, comAuth(null), Void.class);

        assertThat(criarOrdem("OP-CV-1", semiacabado, lista, null).getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(criarOrdem("OP-CV-2", semiacabado, lista, idDaConversao).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("o tipo Conversão só vale para ordem que produz semiacabado (422)")
    void conversaoNaoProduzAcabado() {
        String lista = idDe(listaComComponente(acabado, materiaPrima, "v1"));
        rest.exchange("/api/v1/listas-tecnicas/" + lista + "/ativar", HttpMethod.PATCH, comAuth(null), Void.class);

        assertThat(criarOrdem("OP-CV-3", acabado, lista, idDaConversao).getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(criarOrdem("OP-CV-4", acabado, lista, null).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    // ------------------------------------------------------------------ apoio

    private List<Map<String, Object>> tipos() {
        return rest.exchange(TIPOS, HttpMethod.GET, comAuth(null), List.class).getBody();
    }

    private ResponseEntity<Map> listaComComponente(String dono, String componente, String versao) {
        return rest.postForEntity("/api/v1/listas-tecnicas", comAuth(Map.of("materialId", dono, "versao", versao,
                "itens", List.of(Map.of("materialComponenteId", componente, "quantidadePlanejada", 1, "unidadeDeMedida", "un")))), Map.class);
    }

    private ResponseEntity<Map> criarOrdem(String codigo, String materialId, String listaId, String tipoOrdemId) {
        Map<String, Object> corpo = new java.util.HashMap<>(Map.of(
                "codigo", codigo, "materialId", materialId, "listaTecnicaId", listaId,
                "centroDeTrabalho", "Usinagem", "quantidade", 10,
                "inicioPlanejado", "2026-09-01", "fimPlanejado", "2026-09-10"));
        if (tipoOrdemId != null) corpo.put("tipoOrdemId", tipoOrdemId);
        return rest.postForEntity("/api/v1/ordens-producao", comAuth(corpo), Map.class);
    }

    private String material(String descricao, String tipo, String unidade) {
        return idDe(rest.postForEntity("/api/v1/materiais", comAuth(Map.of(
                "descricao", descricao, "tipo", tipo, "unidadeDeMedida", unidade)), Void.class));
    }

    private static String idDe(ResponseEntity<?> resposta) {
        if (resposta.getHeaders().getLocation() != null) {
            String location = resposta.getHeaders().getLocation().toString();
            return location.substring(location.lastIndexOf('/') + 1);
        }
        return (String) ((Map<String, Object>) resposta.getBody()).get("id");
    }

    private HttpEntity<Object> comAuth(Object corpo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return new HttpEntity<>(corpo, headers);
    }

    private static HttpEntity<Object> semAuth(Object corpo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(corpo, headers);
    }
}
