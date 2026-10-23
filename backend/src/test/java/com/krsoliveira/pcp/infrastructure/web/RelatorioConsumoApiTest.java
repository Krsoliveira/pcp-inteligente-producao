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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Relatório de consumo de ordens, de ponta a ponta com PostgreSQL real: nota fiscal gera os lotes, a ordem
 * consome um componente de DOIS lotes (o relatório mostra o total somado), é concluída, e uma segunda ordem
 * fica aberta (não entra no relatório).
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
class RelatorioConsumoApiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String RELATORIO = "/api/v1/relatorios/consumo-de-ordens";
    private static final String FIM = "2026-09-10";

    @Autowired
    private TestRestTemplate rest;

    private String token;
    private String codigoDaOrdem;
    private String codigoDoAco;
    private String codigoDoParafuso;

    @BeforeAll
    void prepararMassa() {
        token = login("admin@pcp.com", "Senha@123");

        String aco = material("Aço carbono", "MATERIA_PRIMA", "kg");
        String parafuso = material("Parafuso M8", "MATERIA_PRIMA", "un");
        String produto = material("Viga", "PRODUTO_ACABADO", "un");
        codigoDoAco = codigoDe(aco);
        codigoDoParafuso = codigoDe(parafuso);
        String lista = idDe(rest.postForEntity("/api/v1/listas-tecnicas", comAuth(Map.of(
                "materialId", produto, "versao", "v1",
                "itens", List.of(
                        Map.of("materialComponenteId", aco, "quantidadePlanejada", 2.5, "unidadeDeMedida", "kg"),
                        Map.of("materialComponenteId", parafuso, "quantidadePlanejada", 4, "unidadeDeMedida", "un")))), Void.class));
        rest.exchange("/api/v1/listas-tecnicas/" + lista + "/ativar", HttpMethod.PATCH, comAuth(null), Void.class);

        // O aço vem em DOIS lotes (15 kg + 15 kg); o parafuso em um.
        rest.postForEntity("/api/v1/notas-fiscais", comAuth(Map.of(
                "fornecedor", "Aços do Sul", "numero", "NF-1", "dataEmissao", "2026-08-01", "dataRecebimento", "2026-08-02",
                "itens", List.of(
                        Map.of("materialId", aco, "quantidade", 15, "numeroLote", "ACO-A", "dataFabricacao", "2026-07-01", "dataValidade", "2030-01-01"),
                        Map.of("materialId", aco, "quantidade", 15, "numeroLote", "ACO-B", "dataFabricacao", "2026-07-02", "dataValidade", "2030-01-02"),
                        Map.of("materialId", parafuso, "quantidade", 100, "numeroLote", "PAR-A", "dataFabricacao", "2026-07-01", "dataValidade", "2030-01-01")))),
                Void.class);

        // Ordem 1: 10 un → planeja 25 kg de aço e 40 un de parafuso.
        String ordem = criarOrdem("OP-REL-001", produto, lista, "2026-09-01", FIM);
        codigoDaOrdem = "OP-REL-001";
        avancar(ordem, "LIBERADA");
        avancar(ordem, "EM_PRODUCAO");
        List<Map<String, Object>> consumos = rest.exchange("/api/v1/ordens-producao/" + ordem + "/consumos", HttpMethod.GET,
                comAuth(null), List.class).getBody();
        for (Map<String, Object> consumo : consumos) {
            String componente = (String) consumo.get("materialId");
            List<Map<String, Object>> lotes = rest.exchange("/api/v1/lotes?materialId=" + componente + "&disponiveis=true",
                    HttpMethod.GET, comAuth(null), List.class).getBody();
            Map<String, Object> corpo = componente.equals(aco)
                    // 26 kg (1 kg acima do planejado, com justificativa), de dois lotes: 15 + 11
                    ? Map.of("quantidadeConsumida", 26, "justificativa", "Perda no corte",
                            "alocacoes", List.of(
                                    Map.of("loteId", lotes.get(0).get("id"), "quantidade", 15),
                                    Map.of("loteId", lotes.get(1).get("id"), "quantidade", 11)))
                    : Map.of("quantidadeConsumida", 40,
                            "alocacoes", List.of(Map.of("loteId", lotes.get(0).get("id"), "quantidade", 40)));
            ResponseEntity<Map> registrado = rest.exchange("/api/v1/ordens-producao/" + ordem + "/consumos/" + consumo.get("id"),
                    HttpMethod.PATCH, comAuth(corpo), Map.class);
            assertThat(registrado.getStatusCode()).as("registrar consumo").isEqualTo(HttpStatus.OK);
        }
        ResponseEntity<Map> concluida = rest.exchange("/api/v1/ordens-producao/" + ordem + "/concluir", HttpMethod.POST,
                comAuth(Map.of("quantidadeProduzida", 10, "dataFabricacao", LocalDate.now().toString(),
                        "dataValidade", "2031-01-01")), Map.class);
        assertThat(concluida.getStatusCode()).as("concluir").isEqualTo(HttpStatus.OK);

        // Ordem 2, no mesmo período, mas ainda planejada: não entra.
        criarOrdem("OP-REL-002", produto, lista, "2026-09-02", "2026-09-12");
    }

    @Test
    @DisplayName("uma linha por componente da ordem concluída, com o consumo total somando os lotes e o desvio")
    void consumoTotalPorComponente() {
        ResponseEntity<Map> resposta = relatorio("?de=2026-09-01&ate=2026-09-30");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> corpo = resposta.getBody();
        List<Map<String, Object>> linhas = (List<Map<String, Object>>) corpo.get("linhas");
        assertThat(corpo).containsEntry("totalDeOrdens", 1).containsEntry("de", "2026-09-01").containsEntry("ate", "2026-09-30");
        assertThat(linhas).hasSize(2);
        assertThat(linhas).allSatisfy(l -> assertThat(l).containsEntry("ordemCodigo", codigoDaOrdem).containsEntry("fimPlanejado", FIM)
                .containsEntry("produtoDescricao", "Viga"));

        Map<String, Object> aco = linhaDo(linhas, codigoDoAco);
        assertThat(aco).containsEntry("componenteDescricao", "Aço carbono").containsEntry("unidadeDeMedida", "kg");
        assertThat(numero(aco, "quantidadePlanejada")).isEqualByComparingTo("25");
        assertThat(numero(aco, "quantidadeConsumida")).isEqualByComparingTo("26"); // 15 kg de um lote + 11 kg do outro
        assertThat(numero(aco, "desvio")).isEqualByComparingTo("1");

        Map<String, Object> parafuso = linhaDo(linhas, codigoDoParafuso);
        assertThat(numero(parafuso, "quantidadePlanejada")).isEqualByComparingTo("40");
        assertThat(numero(parafuso, "quantidadeConsumida")).isEqualByComparingTo("40");
        assertThat(numero(parafuso, "desvio")).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("período sem ordens concluídas devolve relatório vazio, e os limites do fim planejado são inclusivos")
    void periodoEIntervalos() {
        assertThat(relatorio("?de=2025-01-01&ate=2025-12-31").getBody()).containsEntry("totalDeOrdens", 0);
        assertThat((List<?>) relatorio("?de=2025-01-01&ate=2025-12-31").getBody().get("linhas")).isEmpty();
        assertThat((List<?>) relatorio("?de=" + FIM + "&ate=" + FIM).getBody().get("linhas")).hasSize(2);
        assertThat((List<?>) relatorio("?de=2026-09-11&ate=2026-09-30").getBody().get("linhas")).isEmpty();
    }

    @Test
    @DisplayName("período ausente ou com data inválida devolve 400; invertido ou maior que 366 dias devolve 422")
    void validaOPeriodo() {
        assertThat(relatorio("").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(relatorio("?de=2026-09-01").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(relatorio("?de=ontem&ate=hoje").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(relatorio("?de=2026-09-30&ate=2026-09-01").getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(relatorio("?de=2025-01-01&ate=2026-09-01").getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("sem token devolve 401; Visualizador, que não tem RELATORIO_CONSUMO_VISUALIZAR, recebe 403")
    void exigeAutenticacaoEPermissao() {
        assertThat(rest.getForEntity(RELATORIO + "?de=2026-09-01&ate=2026-09-30", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        rest.postForEntity("/api/v1/auth/registrar", semAuth(Map.of("nome", "Visual", "email", "visual@pcp.com", "senha", "Senha@123")), Void.class);
        String visualizador = login("visual@pcp.com", "Senha@123");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(visualizador);
        ResponseEntity<Map> proibido = rest.exchange(RELATORIO + "?de=2026-09-01&ate=2026-09-30", HttpMethod.GET,
                new HttpEntity<>(headers), Map.class);

        assertThat(proibido.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(proibido.getBody()).containsEntry("title", "Acesso negado");
    }

    // ------------------------------------------------------------------ apoio

    private ResponseEntity<Map> relatorio(String consulta) {
        return rest.exchange(RELATORIO + consulta, HttpMethod.GET, comAuth(null), Map.class);
    }

    private static Map<String, Object> linhaDo(List<Map<String, Object>> linhas, String codigoDoComponente) {
        return linhas.stream().filter(l -> codigoDoComponente.equals(l.get("componenteCodigo"))).findFirst().orElseThrow();
    }

    private static java.math.BigDecimal numero(Map<String, Object> linha, String campo) {
        return new java.math.BigDecimal(String.valueOf(linha.get(campo)));
    }

    private String login(String email, String senha) {
        return (String) rest.postForEntity("/api/v1/auth/login", semAuth(Map.of("email", email, "senha", senha)), Map.class)
                .getBody().get("token");
    }

    private String material(String descricao, String tipo, String unidade) {
        return idDe(rest.postForEntity("/api/v1/materiais", comAuth(Map.of(
                "descricao", descricao, "tipo", tipo, "unidadeDeMedida", unidade)), Void.class));
    }

    private String codigoDe(String materialId) {
        return (String) rest.exchange("/api/v1/materiais/" + materialId, HttpMethod.GET, comAuth(null), Map.class).getBody().get("codigo");
    }

    private String criarOrdem(String codigo, String materialId, String listaId, String inicio, String fim) {
        ResponseEntity<Map> resposta = rest.postForEntity("/api/v1/ordens-producao", comAuth(Map.of(
                "codigo", codigo, "materialId", materialId, "listaTecnicaId", listaId,
                "centroDeTrabalho", "Usinagem", "quantidade", 10,
                "inicioPlanejado", inicio, "fimPlanejado", fim)), Map.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) resposta.getBody().get("id");
    }

    private void avancar(String ordemId, String status) {
        ResponseEntity<Map> resposta = rest.exchange("/api/v1/ordens-producao/" + ordemId + "/status", HttpMethod.PATCH,
                comAuth(Map.of("status", status)), Map.class);
        assertThat(resposta.getStatusCode()).as("avançar para " + status).isEqualTo(HttpStatus.OK);
    }

    private static String idDe(ResponseEntity<?> resposta) {
        String location = resposta.getHeaders().getLocation().toString();
        return location.substring(location.lastIndexOf('/') + 1);
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
