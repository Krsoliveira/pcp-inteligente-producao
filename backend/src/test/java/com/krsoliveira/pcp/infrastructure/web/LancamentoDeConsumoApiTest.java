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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lançamento do consumo de uma ordem de uma vez, tudo ou nada (ADR-0026), de ponta a ponta com PostgreSQL real: o
 * "tudo ou nada" é provado pela volta atrás da transação (nenhuma linha lançada e nenhum saldo de lote baixado quando
 * uma linha é recusada).
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.admin.nome=Administrador de Teste",
                "app.admin.email=admin@pcp.com",
                "app.admin.senha=Senha@123",
                "pcp.seguranca.rate-limit.login=1000",
                "pcp.seguranca.rate-limit.registrar=1000",
                "pcp.seguranca.rate-limit.trocar-senha=1000"
        })
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings({"unchecked", "rawtypes"})
@AutoConfigureTestRestTemplate
class LancamentoDeConsumoApiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    private static final String ORDENS = "/api/v1/ordens-producao";
    private static final String SENHA = "Senha@123";

    @Autowired
    private TestRestTemplate rest;

    private String token;
    private String aco;
    private String cola;
    private String produto;
    private String lista;
    private int sequencia = 0;

    @BeforeAll
    void prepararMassa() {
        token = login("admin@pcp.com", SENHA);
        aco = material("Aço", "MATERIA_PRIMA", "kg");
        cola = material("Cola", "MATERIA_PRIMA", "kg");
        produto = material("Viga", "PRODUTO_ACABADO", "un");
        lista = idDe(rest.postForEntity("/api/v1/listas-tecnicas", comAuth(Map.of("materialId", produto, "versao", "v1",
                "itens", List.of(Map.of("materialComponenteId", aco, "quantidadePlanejada", 2.5, "unidadeDeMedida", "kg")))), Void.class));
        rest.exchange("/api/v1/listas-tecnicas/" + lista + "/ativar", HttpMethod.PATCH, comAuth(null), Void.class);
        rest.postForEntity("/api/v1/notas-fiscais", comAuth(Map.of(
                "fornecedor", "Fornecedor", "numero", "NF-LC", "dataEmissao", "2026-08-01", "dataRecebimento", "2026-08-02",
                "itens", List.of(
                        Map.of("materialId", aco, "quantidade", 500, "numeroLote", "ACO-1", "dataFabricacao", "2026-07-01", "dataValidade", "2030-01-01"),
                        Map.of("materialId", cola, "quantidade", 100, "numeroLote", "COLA-1", "dataFabricacao", "2026-07-01", "dataValidade", "2030-01-01")))),
                Void.class);
    }

    @Test
    @DisplayName("lança a linha da lista e um item adicionado de uma vez; as duas ficam registradas e os lotes são baixados")
    void lancaTudo() {
        String ordem = ordemEmProducao();
        BigDecimal acoAntes = saldo(aco);
        BigDecimal colaAntes = saldo(cola);

        ResponseEntity<List> resposta = lancar(token, ordem, corpo(List.of(linha(ordem, aco, "25", null)),
                List.of(item(cola, "4", "Faltou na lista"))));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> lancados = resposta.getBody();
        assertThat(lancados).hasSize(2);
        assertThat(lancados).extracting(l -> l.get("registrado")).containsOnly(true);
        assertThat(lancados).extracting(l -> l.get("naoPlanejado")).containsExactly(false, true);
        assertThat(saldo(aco)).isEqualByComparingTo(acoAntes.subtract(new BigDecimal("25")));
        assertThat(saldo(cola)).isEqualByComparingTo(colaAntes.subtract(new BigDecimal("4")));
        assertThat(consumosDa(ordem)).hasSize(2).allSatisfy(c -> assertThat(c).containsEntry("registrado", true));
    }

    @Test
    @DisplayName("TUDO OU NADA: se uma linha é recusada, nenhuma é lançada e nenhum saldo de lote é baixado")
    void tudoOuNada() {
        String ordem = ordemEmProducao();
        BigDecimal acoAntes = saldo(aco);
        BigDecimal colaAntes = saldo(cola);

        // A linha do aço é válida; o item de cola não tem justificativa (o desvio é tudo o que foi consumido).
        ResponseEntity<Map> recusado = lancarComoMapa(token, ordem, corpo(List.of(linha(ordem, aco, "25", null)),
                List.of(item(cola, "4", null))));

        assertThat(recusado.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat((String) recusado.getBody().get("detail")).contains("Material").contains("Justificativa");
        assertThat(saldo(aco)).as("o aço da linha válida não pode ter sido baixado").isEqualByComparingTo(acoAntes);
        assertThat(saldo(cola)).isEqualByComparingTo(colaAntes);
        assertThat(consumosDa(ordem)).hasSize(1).allSatisfy(c -> assertThat(c).containsEntry("registrado", false));

        // Depois de corrigir, o mesmo lançamento passa.
        assertThat(lancar(token, ordem, corpo(List.of(linha(ordem, aco, "25", null)), List.of(item(cola, "4", "ok")))).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("desvio sem justificativa na linha da lista devolve 422 com o material, e nada é lançado")
    void desvioSemJustificativa() {
        String ordem = ordemEmProducao();
        BigDecimal acoAntes = saldo(aco);

        ResponseEntity<Map> resposta = lancarComoMapa(token, ordem, corpo(List.of(linha(ordem, aco, "26", null)), List.of()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat((String) resposta.getBody().get("detail")).startsWith("Material ");
        assertThat(saldo(aco)).isEqualByComparingTo(acoAntes);
    }

    @Test
    @DisplayName("ordem que não está em produção devolve 422; ordem inexistente devolve 404; corpo vazio devolve 422")
    void regrasDaOrdem() {
        String planejada = criarOrdem();
        assertThat(lancarComoMapa(token, planejada, corpo(List.of(), List.of(item(cola, "1", "x")))).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(lancarComoMapa(token, "00000000-0000-0000-0000-0000000000bb", corpo(List.of(), List.of(item(cola, "1", "x")))).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(lancarComoMapa(token, ordemEmProducao(), corpo(List.of(), List.of())).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    @DisplayName("permissões: sem token 401; Visualizador 403; quem registra mas não adiciona itens lança linhas (200) e leva 403 se mandar itens")
    void permissoes() {
        String ordem = ordemEmProducao();
        Map<String, Object> soLinhas = corpo(List.of(linha(ordem, aco, "25", null)), List.of());

        assertThat(rest.postForEntity(ORDENS + "/" + ordem + "/consumos/lancamento", new HttpEntity<>(soLinhas, jsonSemAuth()), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        rest.postForEntity("/api/v1/auth/registrar", new HttpEntity<>(Map.of("nome", "Visual", "email", "visual@pcp.com", "senha", SENHA), jsonSemAuth()), Void.class);
        assertThat(lancarComoMapa(login("visual@pcp.com", SENHA), ordem, soLinhas).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        String perfil = idDe(rest.postForEntity("/api/v1/acessos/perfis", comAuth(Map.of("nome", "Só registra", "descricao", "",
                "permissoes", List.of("CONSUMO_REGISTRAR", "CONSUMO_VISUALIZAR"))), Map.class));
        String operador = usuarioComPerfil("operador@pcp.com", perfil);

        assertThat(lancarComoMapa(operador, ordem, corpo(List.of(), List.of(item(cola, "1", "x")))).getStatusCode())
                .as("itens adicionais exigem CONSUMO_ADICIONAR_ITEM").isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(lancar(operador, ordem, soLinhas).getStatusCode()).as("só linhas da lista basta CONSUMO_REGISTRAR").isEqualTo(HttpStatus.OK);
    }

    // ------------------------------------------------------------------ apoio

    private static Map<String, Object> corpo(List<Map<String, Object>> consumos, List<Map<String, Object>> itens) {
        return Map.of("consumos", consumos, "itensAdicionais", itens);
    }

    private Map<String, Object> linha(String ordem, String materialId, String qtd, String justificativa) {
        Map<String, Object> consumo = consumosDa(ordem).stream().filter(c -> materialId.equals(c.get("materialId"))).findFirst().orElseThrow();
        Map<String, Object> linha = new java.util.HashMap<>(Map.of("consumoId", consumo.get("id"), "quantidadeConsumida", new BigDecimal(qtd),
                "alocacoes", List.of(Map.of("loteId", loteDe(materialId), "quantidade", new BigDecimal(qtd)))));
        if (justificativa != null) linha.put("justificativa", justificativa);
        return linha;
    }

    private Map<String, Object> item(String materialId, String qtd, String justificativa) {
        Map<String, Object> item = new java.util.HashMap<>(Map.of("materialId", materialId, "quantidadeConsumida", new BigDecimal(qtd),
                "alocacoes", List.of(Map.of("loteId", loteDe(materialId), "quantidade", new BigDecimal(qtd)))));
        if (justificativa != null) item.put("justificativa", justificativa);
        return item;
    }

    private String loteDe(String materialId) {
        return (String) lotesDisponiveis(materialId).get(0).get("id");
    }

    private BigDecimal saldo(String materialId) {
        return new BigDecimal(String.valueOf(lotesDisponiveis(materialId).get(0).get("saldo")));
    }

    private List<Map<String, Object>> lotesDisponiveis(String materialId) {
        return rest.exchange("/api/v1/lotes?materialId=" + materialId + "&disponiveis=true", HttpMethod.GET, comAuth(null), List.class).getBody();
    }

    private List<Map<String, Object>> consumosDa(String ordem) {
        return rest.exchange(ORDENS + "/" + ordem + "/consumos", HttpMethod.GET, comAuth(null), List.class).getBody();
    }

    private ResponseEntity<List> lancar(String tokenDoUsuario, String ordem, Map<String, Object> corpo) {
        return rest.exchange(ORDENS + "/" + ordem + "/consumos/lancamento", HttpMethod.POST, comToken(tokenDoUsuario, corpo), List.class);
    }

    private ResponseEntity<Map> lancarComoMapa(String tokenDoUsuario, String ordem, Map<String, Object> corpo) {
        return rest.exchange(ORDENS + "/" + ordem + "/consumos/lancamento", HttpMethod.POST, comToken(tokenDoUsuario, corpo), Map.class);
    }

    private String usuarioComPerfil(String email, String perfilId) {
        rest.postForEntity("/api/v1/acessos/usuarios", comAuth(Map.of("nome", "Operador", "email", email,
                "senhaProvisoria", SENHA, "perfilId", perfilId)), Map.class);
        String provisorio = login(email, SENHA);
        rest.exchange("/api/v1/acessos/minha-senha", HttpMethod.PUT,
                comToken(provisorio, Map.of("senhaAtual", SENHA, "novaSenha", SENHA + "!nova")), Void.class);
        return login(email, SENHA + "!nova");
    }

    private String ordemEmProducao() {
        String ordem = criarOrdem();
        avancar(ordem, "LIBERADA");
        avancar(ordem, "EM_PRODUCAO");
        return ordem;
    }

    private String criarOrdem() {
        ResponseEntity<Map> resposta = rest.postForEntity(ORDENS, comAuth(Map.of(
                "codigo", "OP-LC-" + (++sequencia), "materialId", produto, "listaTecnicaId", lista, "centroDeTrabalho", "Usinagem",
                "quantidade", 10, "inicioPlanejado", LocalDate.now().toString(), "fimPlanejado", LocalDate.now().plusDays(10).toString())), Map.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) resposta.getBody().get("id");
    }

    private void avancar(String ordemId, String status) {
        assertThat(rest.exchange(ORDENS + "/" + ordemId + "/status", HttpMethod.PATCH, comAuth(Map.of("status", status)), Map.class)
                .getStatusCode()).as("avançar para " + status).isEqualTo(HttpStatus.OK);
    }

    private String login(String email, String senha) {
        return (String) rest.postForEntity("/api/v1/auth/login", new HttpEntity<>(Map.of("email", email, "senha", senha), jsonSemAuth()), Map.class)
                .getBody().get("token");
    }

    private String material(String descricao, String tipo, String unidade) {
        return idDe(rest.postForEntity("/api/v1/materiais", comAuth(Map.of("descricao", descricao, "tipo", tipo, "unidadeDeMedida", unidade)), Void.class));
    }

    private static String idDe(ResponseEntity<?> resposta) {
        if (resposta.getHeaders().getLocation() != null) {
            String location = resposta.getHeaders().getLocation().toString();
            return location.substring(location.lastIndexOf('/') + 1);
        }
        return (String) ((Map<String, Object>) resposta.getBody()).get("id");
    }

    private HttpEntity<Object> comAuth(Object corpo) {
        return comToken(token, corpo);
    }

    private static HttpEntity<Object> comToken(String tokenDoUsuario, Object corpo) {
        HttpHeaders headers = jsonSemAuth();
        headers.setBearerAuth(tokenDoUsuario);
        return new HttpEntity<>(corpo, headers);
    }

    private static HttpHeaders jsonSemAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
