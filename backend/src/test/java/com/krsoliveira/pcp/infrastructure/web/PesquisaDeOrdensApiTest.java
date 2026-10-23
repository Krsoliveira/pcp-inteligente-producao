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
 * Pesquisa paginada de ordens e resumo do dashboard, de ponta a ponta com PostgreSQL real.
 *
 * Massa: 5 ordens "OP-A-*" atrasadas (fim em 2020), 2 ordens "OP-B-*" com fim em 2999, e
 * uma das "OP-B" liberada.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "pcp.seguranca.rate-limit.login=1000",
                "pcp.seguranca.rate-limit.registrar=1000"
        })
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PesquisaDeOrdensApiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String ORDENS = "/api/v1/ordens-producao";

    @Autowired
    private TestRestTemplate rest;

    private String token;

    @BeforeAll
    @SuppressWarnings("unchecked")
    void prepararMassa() {
        rest.postForEntity("/api/v1/auth/registrar", semAuth(Map.of(
                "nome", "Planejador", "email", "pesquisa@pcp.com", "senha", "Senha@123")), Map.class);
        token = (String) rest.postForEntity("/api/v1/auth/login",
                semAuth(Map.of("email", "pesquisa@pcp.com", "senha", "Senha@123")), Map.class).getBody().get("token");

        String componente = idDe(rest.postForEntity("/api/v1/materiais", comAuth(Map.of(
                "descricao", "Aço", "tipo", "MATERIA_PRIMA", "unidadeDeMedida", "kg")), Void.class));
        String produto = idDe(rest.postForEntity("/api/v1/materiais", comAuth(Map.of(
                "descricao", "Viga", "tipo", "PRODUTO_ACABADO", "unidadeDeMedida", "un")), Void.class));
        String lista = idDe(rest.postForEntity("/api/v1/listas-tecnicas", comAuth(Map.of(
                "materialId", produto, "versao", "v1",
                "itens", List.of(Map.of("materialComponenteId", componente,
                        "quantidadePlanejada", 5.0, "unidadeDeMedida", "kg")))), Void.class));
        rest.exchange("/api/v1/listas-tecnicas/" + lista + "/ativar", HttpMethod.PATCH, comAuth(null), Void.class);

        for (int i = 1; i <= 5; i++) {
            criarOrdem("OP-A-00" + i, produto, lista, "2020-01-01", "2020-01-10");
        }
        String liberada = null;
        for (int i = 1; i <= 2; i++) {
            ResponseEntity<Map> criada = criarOrdem("OP-B-00" + i, produto, lista, "2999-01-01", "2999-01-10");
            if (i == 1) liberada = (String) criada.getBody().get("id");
        }
        rest.exchange(ORDENS + "/" + liberada + "/status", HttpMethod.PATCH,
                comAuth(Map.of("status", "LIBERADA")), Map.class);
    }

    @Test
    @DisplayName("sem filtro: primeira página com o tamanho pedido e o total de todas as ordens")
    @SuppressWarnings("unchecked")
    void primeiraPagina() {
        Map<String, Object> corpo = pesquisar("?tamanho=3").getBody();

        assertThat((List<?>) corpo.get("itens")).hasSize(3);
        assertThat(corpo).containsEntry("total", 7).containsEntry("pagina", 0)
                .containsEntry("tamanho", 3).containsEntry("totalPaginas", 3);
    }

    @Test
    @DisplayName("última página traz só o que sobrou; as mais recentes vêm primeiro")
    @SuppressWarnings("unchecked")
    void ultimaPaginaEOrdenacao() {
        List<Map<String, Object>> primeira = (List<Map<String, Object>>) pesquisar("?tamanho=3&pagina=0").getBody().get("itens");
        List<Map<String, Object>> ultima = (List<Map<String, Object>>) pesquisar("?tamanho=3&pagina=2").getBody().get("itens");

        assertThat(primeira.get(0)).containsEntry("codigo", "OP-B-002");
        assertThat(ultima).hasSize(1);
        assertThat(ultima.get(0)).containsEntry("codigo", "OP-A-001");
    }

    @Test
    @DisplayName("busca por trecho do código, sem diferenciar maiúsculas")
    void buscaPorCodigo() {
        assertThat(pesquisar("?busca=op-b").getBody()).containsEntry("total", 2);
    }

    @Test
    @DisplayName("filtra por status")
    void filtroPorStatus() {
        assertThat(pesquisar("?status=LIBERADA").getBody()).containsEntry("total", 1);
    }

    @Test
    @DisplayName("filtra pelo período do início planejado (limites inclusivos)")
    void filtroPorPeriodo() {
        assertThat(pesquisar("?de=2999-01-01").getBody()).containsEntry("total", 2);
        assertThat(pesquisar("?ate=2020-12-31").getBody()).containsEntry("total", 5);
        assertThat(pesquisar("?de=2020-01-01&ate=2020-01-01").getBody()).containsEntry("total", 5);
        assertThat(pesquisar("?de=2021-01-01&ate=2021-12-31").getBody()).containsEntry("total", 0);
    }

    @Test
    @DisplayName("período invertido devolve 422 e data inválida devolve 400")
    void periodoInvalido() {
        assertThat(pesquisar("?de=2026-09-30&ate=2026-09-01").getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(pesquisar("?de=ontem").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("curinga digitado (%) é texto comum e não casa com tudo")
    void curingaEhTextoComum() {
        assertThat(pesquisar("?busca=%25").getBody()).containsEntry("total", 0);
        assertThat(pesquisar("?busca=OP_A").getBody()).containsEntry("total", 0);
    }

    @Test
    @DisplayName("tamanho acima do teto é limitado a 100 e página negativa vira 0")
    void limitesDaPagina() {
        Map<String, Object> corpo = pesquisar("?tamanho=100000&pagina=-4").getBody();

        assertThat(corpo).containsEntry("tamanho", 100).containsEntry("pagina", 0);
    }

    @Test
    @DisplayName("status desconhecido devolve 400")
    void statusInvalido() {
        assertThat(pesquisar("?status=INEXISTENTE").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("sem token a pesquisa e o resumo devolvem 401")
    void exigeAutenticacao() {
        assertThat(rest.getForEntity(ORDENS + "/pesquisa", Map.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(rest.getForEntity("/api/v1/dashboard/resumo", Map.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("resumo: total, ordens por status, atrasadas e lotes disponíveis")
    @SuppressWarnings("unchecked")
    void resumo() {
        ResponseEntity<Map> resposta = rest.exchange("/api/v1/dashboard/resumo", HttpMethod.GET, comAuth(null), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> corpo = resposta.getBody();
        assertThat(corpo).containsEntry("totalOrdens", 7).containsEntry("ordensAtrasadas", 5)
                .containsEntry("lotesDisponiveis", 0);
        assertThat((Map<String, Object>) corpo.get("ordensPorStatus"))
                .containsEntry("PLANEJADA", 6).containsEntry("LIBERADA", 1)
                .containsEntry("EM_PRODUCAO", 0).containsEntry("CONCLUIDA", 0).containsEntry("CANCELADA", 0);
    }

    // ------------------------------------------------------------------ apoio

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> pesquisar(String consulta) {
        return rest.exchange(ORDENS + "/pesquisa" + consulta, HttpMethod.GET, comAuth(null), Map.class);
    }

    private ResponseEntity<Map> criarOrdem(String codigo, String materialId, String listaId, String inicio, String fim) {
        ResponseEntity<Map> resposta = rest.postForEntity(ORDENS, comAuth(Map.of(
                "codigo", codigo, "materialId", materialId, "listaTecnicaId", listaId,
                "centroDeTrabalho", "Usinagem", "quantidade", 10,
                "inicioPlanejado", inicio, "fimPlanejado", fim)), Map.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resposta;
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
