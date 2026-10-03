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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.security.core.userdetails.User;
import com.krsoliveira.pcp.infrastructure.security.JwtService;

/**
 * Teste de integração de ponta a ponta: sobe a aplicação inteira e um
 * PostgreSQL REAL via Testcontainers (o Flyway aplica todas as migrações).
 *
 * {@code @ServiceConnection} conecta o datasource do Spring ao container
 * automaticamente — sem configurar URL/usuário/senha à mão.
 *
 * {@code @TestInstance(PER_CLASS)} permite que {@code @BeforeAll} seja não-estático
 * e use {@code @Autowired}.
 *
 * Fase 5a: o setup cria Material + Lista Técnica + ativa a lista antes de
 * criar ordens — refletindo o novo modelo de domínio (ADR-0007).
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

    @Autowired
    private JwtService jwtService;

    private static final String BASE_ORDENS     = "/api/v1/ordens-producao";
    private static final String BASE_MATERIAIS   = "/api/v1/materiais";
    private static final String BASE_LISTAS      = "/api/v1/listas-tecnicas";
    private static final String BASE_NOTAS       = "/api/v1/notas-fiscais";

    private String jwtToken;
    private String idCriado;
    private String materialId;
    private String listaTecnicaId;
    private String materiaPrimaId;

    @BeforeAll
    @SuppressWarnings("unchecked")
    void configurarAmbiente() {
        // 1. Registrar e autenticar usuário
        rest.postForEntity("/api/v1/auth/registrar", jsonSemAuth(Map.of(
                "nome", "Planejador Teste",
                "email", "teste@pcp.com",
                "senha", "Senha@123",
                "perfil", "PLANEJADOR")), Map.class);

        ResponseEntity<Map> login = rest.postForEntity(
                "/api/v1/auth/login",
                jsonSemAuth(Map.of("email", "teste@pcp.com", "senha", "Senha@123")),
                Map.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        jwtToken = (String) login.getBody().get("token");
        assertThat(jwtToken).isNotBlank();

        // 2. Criar matéria-prima (componente da BOM)
        ResponseEntity<Void> respMp = rest.postForEntity(BASE_MATERIAIS, json(Map.of(
                "descricao", "Aço estrutural",
                "tipo", "MATERIA_PRIMA",
                "unidadeDeMedida", "kg")), Void.class);
        assertThat(respMp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String mpId = extrairIdDaLocation(respMp);
        materiaPrimaId = mpId;

        // 3. Criar produto acabado (o que será produzido)
        ResponseEntity<Void> respPa = rest.postForEntity(BASE_MATERIAIS, json(Map.of(
                "descricao", "Viga metálica 6m",
                "tipo", "PRODUTO_ACABADO",
                "unidadeDeMedida", "un")), Void.class);
        assertThat(respPa.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        materialId = extrairIdDaLocation(respPa);

        // 4. Criar lista técnica para o produto acabado
        ResponseEntity<Void> respLista = rest.postForEntity(BASE_LISTAS, json(Map.of(
                "materialId", materialId,
                "versao", "v1",
                "itens", List.of(Map.of(
                        "materialComponenteId", mpId,
                        "quantidadePlanejada", 50.0,
                        "unidadeDeMedida", "kg")))), Void.class);
        assertThat(respLista.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        listaTecnicaId = extrairIdDaLocation(respLista);

        // 5. Ativar a lista técnica
        ResponseEntity<Void> respAtivar = rest.exchange(
                BASE_LISTAS + "/" + listaTecnicaId + "/ativar",
                HttpMethod.PATCH, json(null), Void.class);
        assertThat(respAtivar.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private String extrairIdDaLocation(ResponseEntity<?> resposta) {
        String location = resposta.getHeaders().getLocation().toString();
        return location.substring(location.lastIndexOf('/') + 1);
    }

    /** Requisição autenticada com JWT Bearer. */
    private HttpEntity<Object> json(Object corpo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken);
        return new HttpEntity<>(corpo, headers);
    }

    /** Requisição sem autenticação (usada nos endpoints públicos de auth). */
    private HttpEntity<Object> jsonSemAuth(Object corpo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(corpo, headers);
    }

    private Map<String, Object> ordemValida(String codigo) {
        return Map.of(
                "codigo", codigo,
                "materialId", materialId,
                "listaTecnicaId", listaTecnicaId,
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
        ResponseEntity<Map> resposta = rest.postForEntity(
                BASE_ORDENS, json(ordemValida("OP-0001")), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getHeaders().getLocation()).isNotNull();
        Map<String, Object> corpo = resposta.getBody();
        assertThat(corpo.get("status")).isEqualTo("PLANEJADA");
        assertThat(corpo.get("codigo")).isEqualTo("OP-0001");
        assertThat(corpo.get("materialId")).isEqualTo(materialId);
        idCriado = (String) corpo.get("id");
    }

    @Test
    @Order(2)
    @DisplayName("GET por id devolve a ordem criada")
    @SuppressWarnings("unchecked")
    void buscaPorId() {
        ResponseEntity<Map> resposta = rest.exchange(
                BASE_ORDENS + "/" + idCriado, HttpMethod.GET, json(null), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody().get("codigo")).isEqualTo("OP-0001");
    }

    @Test
    @Order(3)
    @DisplayName("POST com código duplicado devolve 409")
    void rejeitaCodigoDuplicado() {
        ResponseEntity<Map> resposta = rest.postForEntity(
                BASE_ORDENS, json(ordemValida("OP-0001")), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @Order(4)
    @DisplayName("PATCH avança o status seguindo a máquina de estados")
    @SuppressWarnings("unchecked")
    void avancaStatus() {
        ResponseEntity<Map> resposta = rest.exchange(
                BASE_ORDENS + "/" + idCriado + "/status",
                HttpMethod.PATCH, json(Map.of("status", "LIBERADA")), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody().get("status")).isEqualTo("LIBERADA");
    }

    @Test
    @Order(5)
    @DisplayName("PATCH com transição inválida devolve 422")
    void rejeitaTransicaoInvalida() {
        // Ordem está LIBERADA; concluir sem passar por EM_PRODUCAO é proibido.
        ResponseEntity<Map> resposta = rest.exchange(
                BASE_ORDENS + "/" + idCriado + "/status",
                HttpMethod.PATCH, json(Map.of("status", "CONCLUIDA")), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @Order(6)
    @DisplayName("POST com corpo inválido devolve 400 com os campos errados")
    @SuppressWarnings("unchecked")
    void rejeitaCorpoInvalido() {
        Map<String, Object> corpoInvalido = Map.of("centroDeTrabalho", "CNC", "quantidade", -5);

        ResponseEntity<Map> resposta = rest.postForEntity(
                BASE_ORDENS, json(corpoInvalido), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, Object> erros = (Map<String, Object>) resposta.getBody().get("erros");
        assertThat(erros).containsKeys("codigo", "quantidade");
    }

    @Test
    @Order(7)
    @DisplayName("Requisição sem token devolve 401")
    void semTokenDevolve401() {
        ResponseEntity<Map> resposta = rest.getForEntity(BASE_ORDENS, Map.class);

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

    @Test
    @Order(20)
    @DisplayName("código do material é gerado na faixa do tipo (110 matéria-prima, 103 produto acabado)")
    @SuppressWarnings("unchecked")
    void codigoDoMaterialGeradoPorFaixa() {
        ResponseEntity<List> materiais = rest.exchange(BASE_MATERIAIS, HttpMethod.GET, json(null), List.class);

        Map<String, String> codigoPorId = new java.util.HashMap<>();
        ((List<Map<String, Object>>) materiais.getBody())
                .forEach(m -> codigoPorId.put((String) m.get("id"), (String) m.get("codigo")));
        assertThat(codigoPorId).containsEntry(materiaPrimaId, "110000001").containsEntry(materialId, "103000001");
    }

    @Test
    @Order(21)
    @DisplayName("entrada de nota fiscal cria um lote por item e alimenta o estoque")
    @SuppressWarnings("unchecked")
    void entradaDeNotaFiscalAlimentaEstoque() {
        ResponseEntity<Map> nota = rest.postForEntity(BASE_NOTAS, json(notaFiscal("NF-900", "lote-a1")), Map.class);

        assertThat(nota.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(nota.getHeaders().getLocation()).isNotNull();
        assertThat(nota.getBody().get("quantidadeItens")).isEqualTo(1);
        List<Map<String, Object>> itens = (List<Map<String, Object>>) nota.getBody().get("itens");
        assertThat(itens).singleElement().satisfies(item -> {
            assertThat(item.get("numeroLote")).isEqualTo("LOTE-A1");
            assertThat(item.get("notaFiscalId")).isEqualTo(nota.getBody().get("id"));
        });

        ResponseEntity<List> estoque = rest.exchange("/api/v1/estoque", HttpMethod.GET, json(null), List.class);
        assertThat(estoque.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> posicao = ((List<Map<String, Object>>) estoque.getBody()).stream()
                .filter(p -> materiaPrimaId.equals(p.get("materialId"))).findFirst().orElseThrow();
        assertThat(((Number) posicao.get("saldoDisponivel")).doubleValue()).isEqualTo(250.0);
        assertThat(posicao.get("lotesDisponiveis")).isEqualTo(1);

        ResponseEntity<List> lotes = rest.exchange("/api/v1/lotes?materialId=" + materiaPrimaId, HttpMethod.GET,
                json(null), List.class);
        assertThat(lotes.getBody()).hasSize(1);

        ResponseEntity<List> notas = rest.exchange(BASE_NOTAS, HttpMethod.GET, json(null), List.class);
        assertThat(notas.getBody()).hasSize(1);
    }

    @Test
    @Order(25)
    @DisplayName("a mesma nota pode trazer o mesmo material em lotes diferentes")
    @SuppressWarnings("unchecked")
    void mesmoMaterialEmLotesDiferentesNaNota() {
        Map<String, Object> item = Map.of("materialId", materiaPrimaId, "quantidade", 10,
                "dataFabricacao", "2026-08-15", "dataValidade", "2031-08-15");
        Map<String, Object> corpo = new java.util.HashMap<>(notaFiscal("NF-910", "X"));
        Map<String, Object> lote1 = new java.util.HashMap<>(item);
        lote1.put("numeroLote", "C-1");
        Map<String, Object> lote2 = new java.util.HashMap<>(item);
        lote2.put("numeroLote", "C-2");
        corpo.put("itens", List.of(lote1, lote2));

        ResponseEntity<Map> resposta = rest.postForEntity(BASE_NOTAS, json(corpo), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody().get("quantidadeItens")).isEqualTo(2);
    }

    @Test
    @Order(22)
    @DisplayName("nota fiscal repetida do mesmo fornecedor devolve 409")
    void notaFiscalRepetidaDevolve409() {
        ResponseEntity<Map> resposta = rest.postForEntity(BASE_NOTAS, json(notaFiscal("NF-900", "LOTE-B1")),
                Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @Order(23)
    @DisplayName("lote do fornecedor já recebido devolve 409")
    void loteRepetidoDevolve409() {
        ResponseEntity<Map> resposta = rest.postForEntity(BASE_NOTAS, json(notaFiscal("NF-901", "LOTE-A1")),
                Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @Order(24)
    @DisplayName("nota fiscal sem itens devolve 400")
    void notaFiscalSemItensDevolve400() {
        Map<String, Object> corpo = new java.util.HashMap<>(notaFiscal("NF-902", "X"));
        corpo.put("itens", List.of());

        ResponseEntity<Map> resposta = rest.postForEntity(BASE_NOTAS, json(corpo), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private Map<String, Object> notaFiscal(String numero, String numeroLote) {
        return Map.of(
                "fornecedor", "Aços Brasil Ltda",
                "numero", numero,
                "dataEmissao", "2026-09-01",
                "dataRecebimento", "2026-09-02",
                "itens", List.of(Map.of(
                        "materialId", materiaPrimaId,
                        "quantidade", 250,
                        "numeroLote", numeroLote,
                        "dataFabricacao", "2026-08-15",
                        "dataValidade", "2031-08-15")));
    }

    @Test
    @Order(100)
    @DisplayName("token válido de usuário que não existe mais retorna 401, não 500")
    void tokenDeUsuarioInexistenteRetorna401() {
        // Situação real: o banco foi recriado e o navegador ainda guarda o token antigo.
        String tokenOrfao = jwtService.gerarToken(
                User.withUsername("removido@pcp.com").password("x").build());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenOrfao);

        ResponseEntity<String> resposta = rest.exchange(BASE_ORDENS, HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
