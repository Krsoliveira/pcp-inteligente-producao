package com.krsoliveira.pcp.infrastructure.web;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Item adicionado ao consumo de uma ordem em produção (ADR-0025), de ponta a ponta com PostgreSQL real: a linha nasce
 * com planejado 0 (V16), já registrada, com a genealogia dos lotes, e aparece no relatório de consumo.
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
class AdicionarItemAoConsumoApiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String ORDENS = "/api/v1/ordens-producao";

    @Autowired
    private TestRestTemplate rest;

    private String token;
    private String aco;
    private String cola;
    private String lubrificante;
    private String outroAcabado;
    private String produto;
    private String lista;
    private String ordem;
    private String ordemPlanejada;

    @BeforeAll
    void prepararMassa() {
        token = login("admin@pcp.com", "Senha@123");
        aco = material("Aço", "MATERIA_PRIMA", "kg");
        cola = material("Cola", "MATERIA_PRIMA", "kg");
        lubrificante = material("Lubrificante", "MATERIA_PRIMA", "kg");
        outroAcabado = material("Peça pronta", "PRODUTO_ACABADO", "un");
        produto = material("Viga", "PRODUTO_ACABADO", "un");
        lista = idDe(rest.postForEntity("/api/v1/listas-tecnicas", comAuth(Map.of("materialId", produto, "versao", "v1",
                "itens", List.of(Map.of("materialComponenteId", aco, "quantidadePlanejada", 2.5, "unidadeDeMedida", "kg")))), Void.class));
        rest.exchange("/api/v1/listas-tecnicas/" + lista + "/ativar", HttpMethod.PATCH, comAuth(null), Void.class);
        rest.postForEntity("/api/v1/notas-fiscais", comAuth(Map.of(
                "fornecedor", "Fornecedor", "numero", "NF-AD", "dataEmissao", "2026-08-01", "dataRecebimento", "2026-08-02",
                "itens", List.of(
                        Map.of("materialId", aco, "quantidade", 100, "numeroLote", "ACO-1", "dataFabricacao", "2026-07-01", "dataValidade", "2030-01-01"),
                        Map.of("materialId", cola, "quantidade", 10, "numeroLote", "COLA-1", "dataFabricacao", "2026-07-01", "dataValidade", "2030-01-01"),
                        Map.of("materialId", cola, "quantidade", 10, "numeroLote", "COLA-2", "dataFabricacao", "2026-07-02", "dataValidade", "2030-01-02"),
                        Map.of("materialId", lubrificante, "quantidade", 5, "numeroLote", "LUB-1", "dataFabricacao", "2026-07-01", "dataValidade", "2030-01-01")))),
                Void.class);
        ordem = criarOrdem("OP-AD-001");
        avancar(ordem, "LIBERADA");
        avancar(ordem, "EM_PRODUCAO");
        ordemPlanejada = criarOrdem("OP-AD-002");
    }

    @Test
    @DisplayName("adiciona o item com planejado 0, já registrado, com o consumo somando dois lotes; aparece na ordem como não planejado")
    void adicionaItem() {
        List<Map<String, Object>> lotes = lotesDisponiveis(cola);

        ResponseEntity<Map> resposta = adicionar(ordem, Map.of("materialId", cola, "quantidadeConsumida", 12,
                "justificativa", "A lista não previa a cola",
                "alocacoes", List.of(Map.of("loteId", lotes.get(0).get("id"), "quantidade", 10),
                        Map.of("loteId", lotes.get(1).get("id"), "quantidade", 2))));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> corpo = resposta.getBody();
        assertThat(corpo).containsEntry("naoPlanejado", true).containsEntry("registrado", true).containsEntry("justificado", true)
                .containsEntry("unidadeDeMedida", "kg").containsEntry("materialId", cola);
        assertThat(numero(corpo, "quantidadePlanejada")).isEqualByComparingTo("0");
        assertThat(numero(corpo, "quantidadeConsumida")).isEqualByComparingTo("12");
        assertThat(numero(corpo, "desvio")).isEqualByComparingTo("12");
        assertThat(lotesDisponiveis(cola)).hasSize(1); // um dos lotes esgotou; o outro ficou com 8

        List<Map<String, Object>> consumos = rest.exchange(ORDENS + "/" + ordem + "/consumos", HttpMethod.GET, comAuth(null), List.class).getBody();
        assertThat(consumos).filteredOn(c -> Boolean.TRUE.equals(c.get("naoPlanejado"))).hasSize(1);
        assertThat(consumos).filteredOn(c -> Boolean.FALSE.equals(c.get("naoPlanejado"))).hasSize(1); // a linha da lista
    }

    @Test
    @DisplayName("sem justificativa devolve 422 e nada é baixado; material repetido devolve 422")
    void regrasDoItem() {
        List<Map<String, Object>> lotes = lotesDisponiveis(lubrificante);

        ResponseEntity<Map> semJustificativa = adicionar(ordem, Map.of("materialId", lubrificante, "quantidadeConsumida", 1,
                "alocacoes", List.of(Map.of("loteId", lotes.get(0).get("id"), "quantidade", 1))));
        assertThat(semJustificativa.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(lotesDisponiveis(lubrificante).get(0)).satisfies(l -> assertThat(numero(l, "saldo")).isEqualByComparingTo("5"));

        ResponseEntity<Map> repetido = adicionar(ordem, Map.of("materialId", aco, "quantidadeConsumida", 1, "justificativa", "x",
                "alocacoes", List.of(Map.of("loteId", lotesDisponiveis(aco).get(0).get("id"), "quantidade", 1))));
        assertThat(repetido.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat((String) repetido.getBody().get("detail")).contains("já está na ordem");
    }

    @Test
    @DisplayName("produto acabado em ordem comum devolve 422 (só no tipo Conversão); ordem que não está em produção devolve 422")
    void produtoAcabadoEOrdemPlanejada() {
        ResponseEntity<Map> acabado = adicionar(ordem, Map.of("materialId", outroAcabado, "quantidadeConsumida", 1, "justificativa", "x"));
        assertThat(acabado.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat((String) acabado.getBody().get("detail")).contains("Conversão");

        ResponseEntity<Map> planejada = adicionar(ordemPlanejada, Map.of("materialId", lubrificante, "quantidadeConsumida", 1, "justificativa", "x"));
        assertThat(planejada.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat((String) planejada.getBody().get("detail")).contains("em produção");
    }

    @Test
    @DisplayName("quantidade zero ou material ausente devolve 400; material ou ordem inexistentes devolvem 404")
    void validacoes() {
        assertThat(adicionar(ordem, Map.of("materialId", lubrificante, "quantidadeConsumida", 0, "justificativa", "x")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(adicionar(ordem, Map.of("quantidadeConsumida", 1, "justificativa", "x")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(adicionar(ordem, Map.of("materialId", "00000000-0000-0000-0000-0000000000aa", "quantidadeConsumida", 1,
                "justificativa", "x")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(adicionar("00000000-0000-0000-0000-0000000000bb", Map.of("materialId", lubrificante, "quantidadeConsumida", 1,
                "justificativa", "x")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("exige a permissão CONSUMO_ADICIONAR_ITEM: sem token 401; Visualizador, que não a tem, 403")
    void permissao() {
        ResponseEntity<Map> semToken = rest.postForEntity(ORDENS + "/" + ordem + "/consumos",
                new HttpEntity<>(Map.of("materialId", cola, "quantidadeConsumida", 1, "justificativa", "x"), jsonSemAuth()), Map.class);
        assertThat(semToken.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        rest.postForEntity("/api/v1/auth/registrar", new HttpEntity<>(Map.of("nome", "Visual", "email", "visual@pcp.com", "senha", "Senha@123"), jsonSemAuth()), Void.class);
        HttpHeaders headers = jsonSemAuth();
        headers.setBearerAuth(login("visual@pcp.com", "Senha@123"));
        ResponseEntity<Map> proibido = rest.postForEntity(ORDENS + "/" + ordem + "/consumos",
                new HttpEntity<>(Map.of("materialId", cola, "quantidadeConsumida", 1, "justificativa", "x"), headers), Map.class);

        assertThat(proibido.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ------------------------------------------------------------------ apoio

    private ResponseEntity<Map> adicionar(String ordemId, Map<String, Object> corpo) {
        return rest.postForEntity(ORDENS + "/" + ordemId + "/consumos", comAuth(corpo), Map.class);
    }

    private List<Map<String, Object>> lotesDisponiveis(String materialId) {
        return rest.exchange("/api/v1/lotes?materialId=" + materialId + "&disponiveis=true", HttpMethod.GET, comAuth(null), List.class).getBody();
    }

    private static BigDecimal numero(Map<String, Object> mapa, String campo) {
        return new BigDecimal(String.valueOf(mapa.get(campo)));
    }

    private String login(String email, String senha) {
        return (String) rest.postForEntity("/api/v1/auth/login", new HttpEntity<>(Map.of("email", email, "senha", senha), jsonSemAuth()), Map.class)
                .getBody().get("token");
    }

    private String material(String descricao, String tipo, String unidade) {
        return idDe(rest.postForEntity("/api/v1/materiais", comAuth(Map.of("descricao", descricao, "tipo", tipo, "unidadeDeMedida", unidade)), Void.class));
    }

    private String criarOrdem(String codigo) {
        ResponseEntity<Map> resposta = rest.postForEntity(ORDENS, comAuth(Map.of(
                "codigo", codigo, "materialId", produto, "listaTecnicaId", lista, "centroDeTrabalho", "Usinagem", "quantidade", 10,
                "inicioPlanejado", LocalDate.now().toString(), "fimPlanejado", LocalDate.now().plusDays(10).toString())), Map.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) resposta.getBody().get("id");
    }

    private void avancar(String ordemId, String status) {
        assertThat(rest.exchange(ORDENS + "/" + ordemId + "/status", HttpMethod.PATCH, comAuth(Map.of("status", status)), Map.class)
                .getStatusCode()).as("avançar para " + status).isEqualTo(HttpStatus.OK);
    }

    private static String idDe(ResponseEntity<?> resposta) {
        String location = resposta.getHeaders().getLocation().toString();
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private HttpEntity<Object> comAuth(Object corpo) {
        HttpHeaders headers = jsonSemAuth();
        headers.setBearerAuth(token);
        return new HttpEntity<>(corpo, headers);
    }

    private static HttpHeaders jsonSemAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
