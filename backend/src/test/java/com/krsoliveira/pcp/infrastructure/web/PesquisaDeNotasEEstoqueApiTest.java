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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pesquisa paginada de notas fiscais e do estoque, e resumo de alertas, de ponta a ponta com PostgreSQL real.
 *
 * Massa: matérias-primas "Aço inox" (com lotes) e "Alumínio" (sem lotes) e o produto "Viga"; três notas:
 * NF-001 (Aços Brasil, 01/09, lote LOTE-A1), NF-002 (Metais Sul, 10/09, lote ZINCO-9) e
 * NF-003 (Aços Brasil, 05/10, lote LOTE-B7).
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "pcp.seguranca.rate-limit.login=1000",
                "pcp.seguranca.rate-limit.registrar=1000"
        })
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PesquisaDeNotasEEstoqueApiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String NOTAS = "/api/v1/notas-fiscais";
    private static final String ESTOQUE = "/api/v1/estoque";

    @Autowired
    private TestRestTemplate rest;

    private String token;
    private String idAco;

    @BeforeAll
    @SuppressWarnings("unchecked")
    void prepararMassa() {
        rest.postForEntity("/api/v1/auth/registrar", semAuth(Map.of(
                "nome", "Almoxarife", "email", "notas@pcp.com", "senha", "Senha@123")), Map.class);
        token = (String) rest.postForEntity("/api/v1/auth/login",
                semAuth(Map.of("email", "notas@pcp.com", "senha", "Senha@123")), Map.class).getBody().get("token");

        idAco = criarMaterial("Aço inox", "MATERIA_PRIMA", "kg");
        criarMaterial("Alumínio", "MATERIA_PRIMA", "kg");
        criarMaterial("Viga metálica", "PRODUTO_ACABADO", "un");

        registrarNota("Aços Brasil", "NF-001", "2026-09-01", "LOTE-A1");
        registrarNota("Metais Sul", "NF-002", "2026-09-10", "ZINCO-9");
        registrarNota("Aços Brasil", "NF-003", "2026-10-05", "LOTE-B7");
    }

    // ------------------------------------------------------------------ notas fiscais

    @Test
    @DisplayName("notas: página com o tamanho pedido, total de todas e a recebida por último primeiro, com os itens")
    @SuppressWarnings("unchecked")
    void notasPrimeiraPagina() {
        Map<String, Object> corpo = pesquisarNotas("?tamanho=2").getBody();

        List<Map<String, Object>> itens = (List<Map<String, Object>>) corpo.get("itens");
        assertThat(itens).hasSize(2);
        assertThat(itens.get(0)).containsEntry("numero", "NF-003").containsEntry("quantidadeItens", 1);
        assertThat((List<?>) itens.get(0).get("itens")).hasSize(1);
        assertThat(corpo).containsEntry("total", 3).containsEntry("totalPaginas", 2);
    }

    @Test
    @DisplayName("notas: busca por número, por fornecedor e pelo número do lote de um item")
    void notasBusca() {
        assertThat(pesquisarNotas("?busca=nf-00").getBody()).containsEntry("total", 3);
        assertThat(pesquisarNotas("?busca=METAIS").getBody()).containsEntry("total", 1);
        assertThat(pesquisarNotas("?busca=zinco").getBody()).containsEntry("total", 1);
        assertThat(pesquisarNotas("?busca=lote-b7").getBody()).containsEntry("total", 1);
        assertThat(pesquisarNotas("?busca=inexistente").getBody()).containsEntry("total", 0);
    }

    @Test
    @DisplayName("notas: filtra pelo período de recebimento, com as datas inclusivas, e combina com a busca")
    void notasPeriodo() {
        assertThat(pesquisarNotas("?de=2026-09-01&ate=2026-09-30").getBody()).containsEntry("total", 2);
        assertThat(pesquisarNotas("?de=2026-09-10&ate=2026-09-10").getBody()).containsEntry("total", 1);
        assertThat(pesquisarNotas("?de=2026-10-01").getBody()).containsEntry("total", 1);
        assertThat(pesquisarNotas("?ate=2026-09-01").getBody()).containsEntry("total", 1);
        assertThat(pesquisarNotas("?busca=Aços&de=2026-09-15").getBody()).containsEntry("total", 1);
    }

    @Test
    @DisplayName("notas: período invertido devolve 422; curinga digitado (%) é texto comum")
    void notasValidacoes() {
        assertThat(pesquisarNotas("?de=2026-09-30&ate=2026-09-01").getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(pesquisarNotas("?busca=%25").getBody()).containsEntry("total", 0);
        assertThat(pesquisarNotas("?busca=NF_00").getBody()).containsEntry("total", 0);
    }

    @Test
    @DisplayName("notas e estoque: sem token devolvem 401")
    void exigemAutenticacao() {
        assertThat(rest.getForEntity(NOTAS + "/pesquisa", Map.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(rest.getForEntity(ESTOQUE + "/pesquisa", Map.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(rest.getForEntity(ESTOQUE + "/resumo", Map.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ------------------------------------------------------------------ estoque

    @Test
    @DisplayName("estoque: o material com entrada mais recente vem primeiro e todos aparecem, inclusive sem lote")
    @SuppressWarnings("unchecked")
    void estoquePrimeiraPagina() {
        Map<String, Object> corpo = pesquisarEstoque("").getBody();

        List<Map<String, Object>> itens = (List<Map<String, Object>>) corpo.get("itens");
        assertThat(corpo).containsEntry("total", 3);
        assertThat(itens.get(0)).containsEntry("descricao", "Aço inox");
        assertThat(((Number) itens.get(0).get("saldoDisponivel")).doubleValue()).isEqualTo(750.0);
    }

    @Test
    @DisplayName("estoque: busca pela descrição ou pelo código formatado, tipo e só com saldo")
    @SuppressWarnings("unchecked")
    void estoqueFiltros() {
        assertThat(pesquisarEstoque("?busca=alum").getBody()).containsEntry("total", 1);
        assertThat(pesquisarEstoque("?tipo=MATERIA_PRIMA").getBody()).containsEntry("total", 2);
        assertThat(pesquisarEstoque("?tipo=PRODUTO_ACABADO").getBody()).containsEntry("total", 1);
        assertThat(pesquisarEstoque("?somenteComSaldo=true").getBody()).containsEntry("total", 1);
        assertThat(pesquisarEstoque("?somenteComSaldo=false").getBody()).containsEntry("total", 3);

        List<Map<String, Object>> aco = (List<Map<String, Object>>) pesquisarEstoque("?busca=aço").getBody().get("itens");
        String codigo = (String) aco.get(0).get("codigo");
        String formatado = codigo.substring(0, 3) + "." + codigo.substring(3, 6) + "." + codigo.substring(6);
        assertThat(pesquisarEstoque("?busca=" + formatado).getBody()).containsEntry("total", 1);
    }

    @Test
    @DisplayName("estoque: tipo desconhecido devolve 400")
    void estoqueTipoInvalido() {
        assertThat(pesquisarEstoque("?tipo=INEXISTENTE").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("estoque: resumo dos alertas (nenhum lote vence em 30 dias; Alumínio está sem saldo)")
    void estoqueResumo() {
        ResponseEntity<Map> resposta = rest.exchange(ESTOQUE + "/resumo", HttpMethod.GET, comAuth(null), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("totalMateriais", 3)
                .containsEntry("materiaisVencendo", 0)
                .containsEntry("materiasPrimasSemSaldo", 1);
    }

    // ------------------------------------------------------------------ apoio

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> pesquisarNotas(String consulta) {
        return rest.exchange(NOTAS + "/pesquisa" + consulta, HttpMethod.GET, comAuth(null), Map.class);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> pesquisarEstoque(String consulta) {
        return rest.exchange(ESTOQUE + "/pesquisa" + consulta, HttpMethod.GET, comAuth(null), Map.class);
    }

    private String criarMaterial(String descricao, String tipo, String unidade) {
        ResponseEntity<Void> resposta = rest.postForEntity("/api/v1/materiais", comAuth(Map.of(
                "descricao", descricao, "tipo", tipo, "unidadeDeMedida", unidade)), Void.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String location = resposta.getHeaders().getLocation().toString();
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private void registrarNota(String fornecedor, String numero, String recebimento, String lote) {
        ResponseEntity<Map> resposta = rest.postForEntity(NOTAS, comAuth(Map.of(
                "fornecedor", fornecedor, "numero", numero,
                "dataEmissao", recebimento, "dataRecebimento", recebimento,
                "itens", List.of(Map.of(
                        "materialId", idAco, "quantidade", 250, "numeroLote", lote,
                        "dataFabricacao", "2026-08-01", "dataValidade", "2031-08-01")))), Map.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
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
