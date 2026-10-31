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
 * Pesquisa paginada de materiais e de listas técnicas, de ponta a ponta com PostgreSQL real.
 *
 * Massa: matérias-primas "Aço inox" e "Alumínio", produtos "Viga metálica" e "Chapa dobrada";
 * a Viga tem as versões v1 (ativa) e v2 (em revisão) e a Chapa tem a v1 (em revisão).
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
@AutoConfigureTestRestTemplate
class PesquisaDeMateriaisEListasApiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    private static final String MATERIAIS = "/api/v1/materiais";
    private static final String LISTAS = "/api/v1/listas-tecnicas";

    @Autowired
    private TestRestTemplate rest;

    private String token;
    private String idAco;
    private String idViga;
    private String idChapa;

    @BeforeAll
    @SuppressWarnings("unchecked")
    void prepararMassa() {
        token = (String) rest.postForEntity("/api/v1/auth/login",
                semAuth(Map.of("email", "admin@pcp.com", "senha", "Senha@123")), Map.class).getBody().get("token");

        idAco = criarMaterial("Aço inox", "MATERIA_PRIMA", "kg");
        criarMaterial("Alumínio", "MATERIA_PRIMA", "kg");
        idViga = criarMaterial("Viga metálica", "PRODUTO_ACABADO", "un");
        idChapa = criarMaterial("Chapa dobrada", "PRODUTO_ACABADO", "un");

        String vigaV1 = criarLista(idViga, "v1");
        criarLista(idViga, "v2");
        rest.exchange(LISTAS + "/" + vigaV1 + "/ativar", HttpMethod.PATCH, comAuth(null), Void.class);
        criarLista(idChapa, "v1");
    }

    // ------------------------------------------------------------------ materiais

    @Test
    @DisplayName("materiais: página com o tamanho pedido, total de todos e o cadastrado por último primeiro")
    @SuppressWarnings("unchecked")
    void materiaisPrimeiraPagina() {
        Map<String, Object> corpo = pesquisarMateriais("?tamanho=2").getBody();

        List<Map<String, Object>> itens = (List<Map<String, Object>>) corpo.get("itens");
        assertThat(itens).hasSize(2);
        assertThat(itens.get(0)).containsEntry("descricao", "Chapa dobrada");
        assertThat(corpo).containsEntry("total", 4).containsEntry("totalPaginas", 2);
    }

    @Test
    @DisplayName("materiais: busca pela descrição, sem diferenciar maiúsculas")
    void materiaisBuscaPorDescricao() {
        assertThat(pesquisarMateriais("?busca=VIGA").getBody()).containsEntry("total", 1);
        assertThat(pesquisarMateriais("?busca=met").getBody()).containsEntry("total", 1);
    }

    @Test
    @DisplayName("materiais: busca pelo código formatado (103.000.001) e pelo código só com dígitos")
    @SuppressWarnings("unchecked")
    void materiaisBuscaPorCodigo() {
        List<Map<String, Object>> viga = (List<Map<String, Object>>) pesquisarMateriais("?busca=viga").getBody().get("itens");
        String codigo = (String) viga.get(0).get("codigo");
        String formatado = codigo.substring(0, 3) + "." + codigo.substring(3, 6) + "." + codigo.substring(6);

        assertThat(pesquisarMateriais("?busca=" + formatado).getBody()).containsEntry("total", 1);
        assertThat(pesquisarMateriais("?busca=" + codigo).getBody()).containsEntry("total", 1);
    }

    @Test
    @DisplayName("materiais: filtra por tipo e combina com a busca")
    void materiaisFiltroPorTipo() {
        assertThat(pesquisarMateriais("?tipo=MATERIA_PRIMA").getBody()).containsEntry("total", 2);
        assertThat(pesquisarMateriais("?tipo=PRODUTO_ACABADO&busca=viga").getBody()).containsEntry("total", 1);
        assertThat(pesquisarMateriais("?tipo=MATERIA_PRIMA&busca=viga").getBody()).containsEntry("total", 0);
    }

    @Test
    @DisplayName("materiais: curinga digitado (%) é texto comum")
    void materiaisCuringaEhTextoComum() {
        assertThat(pesquisarMateriais("?busca=%25").getBody()).containsEntry("total", 0);
    }

    @Test
    @DisplayName("materiais: tipo desconhecido devolve 400 e sem token devolve 401")
    void materiaisValidaEntradaEAutenticacao() {
        assertThat(pesquisarMateriais("?tipo=INEXISTENTE").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rest.getForEntity(MATERIAIS + "/pesquisa", Map.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ------------------------------------------------------------------ listas técnicas

    @Test
    @DisplayName("listas: sem filtro traz todas as versões, a criada por último primeiro, com os componentes")
    @SuppressWarnings("unchecked")
    void listasSemFiltro() {
        Map<String, Object> corpo = pesquisarListas("").getBody();

        List<Map<String, Object>> itens = (List<Map<String, Object>>) corpo.get("itens");
        assertThat(corpo).containsEntry("total", 3);
        assertThat(itens.get(0)).containsEntry("materialId", idChapa);
        assertThat((List<?>) itens.get(0).get("itens")).hasSize(1);
    }

    @Test
    @DisplayName("listas: filtra por material e por status")
    void listasFiltros() {
        assertThat(pesquisarListas("?materialId=" + idViga).getBody()).containsEntry("total", 2);
        assertThat(pesquisarListas("?status=ATIVA").getBody()).containsEntry("total", 1);
        assertThat(pesquisarListas("?status=EM_REVISAO").getBody()).containsEntry("total", 2);
        assertThat(pesquisarListas("?materialId=" + idViga + "&status=ATIVA").getBody()).containsEntry("total", 1);
        assertThat(pesquisarListas("?materialId=" + idAco).getBody()).containsEntry("total", 0);
    }

    @Test
    @DisplayName("listas: material com formato inválido devolve 400 e sem token devolve 401")
    void listasValidaEntradaEAutenticacao() {
        assertThat(pesquisarListas("?materialId=nao-e-uuid").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rest.getForEntity(LISTAS + "/pesquisa", Map.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ------------------------------------------------------------------ apoio

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> pesquisarMateriais(String consulta) {
        return rest.exchange(MATERIAIS + "/pesquisa" + consulta, HttpMethod.GET, comAuth(null), Map.class);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> pesquisarListas(String consulta) {
        return rest.exchange(LISTAS + "/pesquisa" + consulta, HttpMethod.GET, comAuth(null), Map.class);
    }

    private String criarMaterial(String descricao, String tipo, String unidade) {
        ResponseEntity<Void> resposta = rest.postForEntity(MATERIAIS, comAuth(Map.of(
                "descricao", descricao, "tipo", tipo, "unidadeDeMedida", unidade)), Void.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return idDe(resposta);
    }

    private String criarLista(String materialId, String versao) {
        ResponseEntity<Void> resposta = rest.postForEntity(LISTAS, comAuth(Map.of(
                "materialId", materialId, "versao", versao,
                "itens", List.of(Map.of("materialComponenteId", idAco,
                        "quantidadePlanejada", 2.0, "unidadeDeMedida", "kg")))), Void.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return idDe(resposta);
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
