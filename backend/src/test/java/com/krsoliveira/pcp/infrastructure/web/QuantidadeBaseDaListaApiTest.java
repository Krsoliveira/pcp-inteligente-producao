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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Quantidade base da lista técnica (ADR-0027), de ponta a ponta com PostgreSQL real: a V17 grava a base, o consumo da
 * ordem usa quantidade do item ÷ base × quantidade da ordem, e a comparação de versões é por unidade produzida.
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
class QuantidadeBaseDaListaApiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String LISTAS = "/api/v1/listas-tecnicas";
    private static final String ORDENS = "/api/v1/ordens-producao";

    @Autowired
    private TestRestTemplate rest;

    private String token;
    private String aco;
    private String produto;
    private int sequencia = 0;

    @BeforeAll
    void prepararMassa() {
        token = (String) rest.postForEntity("/api/v1/auth/login", semAuth(Map.of("email", "admin@pcp.com", "senha", "Senha@123")), Map.class)
                .getBody().get("token");
        aco = material("Aço", "MATERIA_PRIMA", "kg");
        produto = material("Viga", "PRODUTO_ACABADO", "un");
    }

    @Test
    @DisplayName("a lista grava e devolve a quantidade base; sem informar, é 1")
    void gravaEDevolveABase() {
        String comBase = cadastrar(novoProduto(), "v1", "100", "200");
        String semBase = cadastrar(novoProduto(), "v1", null, "2");

        assertThat(numero(lista(comBase), "quantidadeBase")).isEqualByComparingTo("100");
        assertThat(numero(lista(semBase), "quantidadeBase")).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("base zero, negativa ou com mais de 4 casas devolve 400 e nada é cadastrado")
    void baseInvalida() {
        for (String invalida : List.of("0", "-5", "1.00001", "123456789")) {
            ResponseEntity<Map> resposta = tentarCadastrar(produto, "vx" + invalida, invalida, "1");
            assertThat(resposta.getStatusCode()).as("base " + invalida).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Test
    @DisplayName("o consumo da ordem usa a base: 200 kg para 100 un, ordem de 50 un, planeja 100 kg")
    void consumoUsaABase() {
        String material = novoProduto();
        String lista = cadastrar(material, "v1", "100", "200");
        rest.exchange(LISTAS + "/" + lista + "/ativar", HttpMethod.PATCH, comAuth(null), Void.class);

        String ordem = criarOrdem(material, lista, 50).getBody().get("id").toString();

        List<Map<String, Object>> consumos = rest.exchange(ORDENS + "/" + ordem + "/consumos", HttpMethod.GET, comAuth(null), List.class).getBody();
        assertThat(consumos).singleElement().satisfies(c -> assertThat(numero(c, "quantidadePlanejada")).isEqualByComparingTo("100"));
    }

    @Test
    @DisplayName("sem base (padrão 1) a conta é a de sempre: 2 kg por unidade, ordem de 50, planeja 100 kg")
    void semBaseContinuaPorUnidade() {
        String material = novoProduto();
        String lista = cadastrar(material, "v1", null, "2");
        rest.exchange(LISTAS + "/" + lista + "/ativar", HttpMethod.PATCH, comAuth(null), Void.class);

        String ordem = criarOrdem(material, lista, 50).getBody().get("id").toString();

        assertThat(rest.exchange(ORDENS + "/" + ordem + "/consumos", HttpMethod.GET, comAuth(null), List.class).getBody())
                .singleElement().satisfies(c -> assertThat(numero((Map<String, Object>) c, "quantidadePlanejada")).isEqualByComparingTo("100"));
    }

    @Test
    @DisplayName("componente que arredonda para zero numa ordem pequena devolve 422 e a ordem não é criada")
    void arredondaParaZero() {
        String material = novoProduto();
        String lista = cadastrar(material, "v1", "1000000", "0.001");
        rest.exchange(LISTAS + "/" + lista + "/ativar", HttpMethod.PATCH, comAuth(null), Void.class);

        ResponseEntity<Map> resposta = criarOrdem(material, lista, 1);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat((String) resposta.getBody().get("detail")).contains("arredonda para zero");
        Map<String, Object> pesquisa = rest.exchange(ORDENS + "/pesquisa?busca=OP-QB-" + sequencia, HttpMethod.GET, comAuth(null), Map.class).getBody();
        assertThat(pesquisa).containsEntry("total", 0); // a transação desfez a ordem
    }

    @Test
    @DisplayName("comparação por unidade produzida: 200 kg para 100 un é igual a 2 kg para 1 un; 250 é alteração")
    void comparaPorUnidade() {
        String material = novoProduto();
        String v1 = cadastrar(material, "v1", "100", "200");
        String v2 = cadastrar(material, "v2", null, "2");
        String v3 = cadastrar(material, "v3", "100", "250");

        Map<String, Object> iguais = comparar(v1, v2);
        Map<String, Object> alterados = comparar(v1, v3);

        assertThat(iguais).containsEntry("temDiferencas", false);
        assertThat((List<?>) iguais.get("iguais")).hasSize(1);
        assertThat(numero((Map<String, Object>) iguais.get("versaoA"), "quantidadeBase")).isEqualByComparingTo("100");
        assertThat(numero((Map<String, Object>) iguais.get("versaoB"), "quantidadeBase")).isEqualByComparingTo("1");
        assertThat(alterados).containsEntry("temDiferencas", true);
        assertThat((List<?>) alterados.get("alterados")).hasSize(1);
    }

    // ------------------------------------------------------------------ apoio

    private String novoProduto() {
        return material("Produto " + (++sequencia), "PRODUTO_ACABADO", "un");
    }

    private ResponseEntity<Map> tentarCadastrar(String materialId, String versao, String base, String quantidadeDoItem) {
        Map<String, Object> corpo = new HashMap<>(Map.of("materialId", materialId, "versao", versao,
                "itens", List.of(Map.of("materialComponenteId", aco, "quantidadePlanejada", new BigDecimal(quantidadeDoItem), "unidadeDeMedida", "kg"))));
        if (base != null) corpo.put("quantidadeBase", new BigDecimal(base));
        return rest.postForEntity(LISTAS, comAuth(corpo), Map.class);
    }

    private String cadastrar(String materialId, String versao, String base, String quantidadeDoItem) {
        ResponseEntity<Map> resposta = tentarCadastrar(materialId, versao, base, quantidadeDoItem);
        assertThat(resposta.getStatusCode()).as("cadastrar lista: " + resposta.getBody()).isEqualTo(HttpStatus.CREATED);
        String location = resposta.getHeaders().getLocation().toString();
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private Map<String, Object> lista(String id) {
        return rest.exchange(LISTAS + "/" + id, HttpMethod.GET, comAuth(null), Map.class).getBody();
    }

    private Map<String, Object> comparar(String a, String b) {
        return rest.exchange(LISTAS + "/comparar?a=" + a + "&b=" + b, HttpMethod.GET, comAuth(null), Map.class).getBody();
    }

    private ResponseEntity<Map> criarOrdem(String materialId, String listaId, int quantidade) {
        return rest.postForEntity(ORDENS, comAuth(Map.of(
                "codigo", "OP-QB-" + sequencia + "-" + (++sequencia), "materialId", materialId, "listaTecnicaId", listaId,
                "centroDeTrabalho", "Usinagem", "quantidade", quantidade,
                "inicioPlanejado", LocalDate.now().toString(), "fimPlanejado", LocalDate.now().plusDays(10).toString())), Map.class);
    }

    private static BigDecimal numero(Map<String, Object> mapa, String campo) {
        return new BigDecimal(String.valueOf(mapa.get(campo)));
    }

    private String material(String descricao, String tipo, String unidade) {
        ResponseEntity<Void> resposta = rest.postForEntity("/api/v1/materiais", comAuth(Map.of(
                "descricao", descricao, "tipo", tipo, "unidadeDeMedida", unidade)), Void.class);
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
