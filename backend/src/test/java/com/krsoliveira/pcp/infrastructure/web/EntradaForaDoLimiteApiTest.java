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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Entrada que passa pela validação do DTO, mas que o banco não aceita (número acima da coluna, byte nulo em texto), e
 * página fora do que a consulta consegue calcular: são erro de quem chamou, nunca 500. Precisa do PostgreSQL real, porque
 * é ele quem recusa esses valores.
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
class EntradaForaDoLimiteApiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    private static final String ORDENS = "/api/v1/ordens-producao";
    private static final String MENSAGEM = "Um dos valores informados está fora do limite ou do formato aceito.";

    @Autowired
    private TestRestTemplate rest;

    private String token;
    private String aco;
    private String produto;
    private String lista;

    @BeforeAll
    void prepararMassa() {
        token = (String) rest.postForEntity("/api/v1/auth/login",
                new HttpEntity<>(Map.of("email", "admin@pcp.com", "senha", "Senha@123"), json()), Map.class).getBody().get("token");
        aco = idDe(rest.exchange("/api/v1/materiais", HttpMethod.POST,
                comAuth(Map.of("descricao", "Aço", "tipo", "MATERIA_PRIMA", "unidadeDeMedida", "kg")), Map.class));
        produto = idDe(rest.exchange("/api/v1/materiais", HttpMethod.POST,
                comAuth(Map.of("descricao", "Viga", "tipo", "PRODUTO_ACABADO", "unidadeDeMedida", "un")), Map.class));
        lista = idDe(rest.exchange("/api/v1/listas-tecnicas", HttpMethod.POST, comAuth(Map.of("materialId", produto, "versao", "v1",
                "itens", List.of(Map.of("materialComponenteId", aco, "quantidadePlanejada", 2.5, "unidadeDeMedida", "kg")))), Map.class));
        rest.exchange("/api/v1/listas-tecnicas/" + lista + "/ativar", HttpMethod.PATCH, comAuth(null), Void.class);
    }

    @Test
    @DisplayName("quantidade acima do que a coluna guarda: 400, e nada é gravado")
    void numeroAcimaDaColuna() {
        String corpo = """
                {"fornecedor":"F","numero":"NF-GIGANTE","dataEmissao":"2026-08-01","dataRecebimento":"2026-08-02",
                 "itens":[{"materialId":"%s","quantidade":1e30,"numeroLote":"G-1","dataFabricacao":"2026-07-01","dataValidade":"2030-01-01"}]}
                """.formatted(aco);

        ResponseEntity<Map> resposta = rest.exchange("/api/v1/notas-fiscais", HttpMethod.POST, comAuth(corpo), Map.class);

        assertErroDeEntrada(resposta);
        assertThat(rest.exchange("/api/v1/notas-fiscais/pesquisa?busca=NF-GIGANTE", HttpMethod.GET, comAuth(null), Map.class).getBody())
                .containsEntry("total", 0);
    }

    @Test
    @DisplayName("ordem cuja projeção de consumo não cabe na coluna: 400, e a ordem não fica criada pela metade")
    void projecaoAcimaDaColuna() {
        ResponseEntity<Map> resposta = rest.exchange(ORDENS, HttpMethod.POST, comAuth(Map.of(
                "codigo", "OP-GIGANTE", "materialId", produto, "listaTecnicaId", lista, "centroDeTrabalho", "Usinagem",
                "quantidade", 2_000_000_000, "inicioPlanejado", "2026-01-01", "fimPlanejado", "2026-02-01")), Map.class);

        assertErroDeEntrada(resposta);
        assertThat(rest.exchange(ORDENS + "/pesquisa?busca=OP-GIGANTE", HttpMethod.GET, comAuth(null), Map.class).getBody())
                .containsEntry("total", 0);
    }

    @Test
    @DisplayName("byte nulo em texto do corpo: 400")
    void byteNuloNoCorpo() {
        ResponseEntity<Map> resposta = rest.exchange("/api/v1/materiais", HttpMethod.POST,
                comAuth(Map.of("descricao", "a\u0000b", "tipo", "MATERIA_PRIMA", "unidadeDeMedida", "kg")), Map.class);

        assertErroDeEntrada(resposta);
    }

    @Test
    @DisplayName("byte nulo em parâmetro de busca: 400")
    void byteNuloNaBusca() {
        ResponseEntity<Map> resposta = rest.exchange(URI.create(rest.getRootUri() + ORDENS + "/pesquisa?busca=a%00b"),
                HttpMethod.GET, comAuth(null), Map.class);

        assertErroDeEntrada(resposta);
    }

    @Test
    @DisplayName("conflito de dados não é confundido com entrada inválida: código repetido continua 409")
    void conflitoContinuaConflito() {
        Map<String, Object> ordem = Map.of("codigo", "OP-REPETIDA", "materialId", produto, "listaTecnicaId", lista,
                "centroDeTrabalho", "Usinagem", "quantidade", 10, "inicioPlanejado", "2026-01-01", "fimPlanejado", "2026-02-01");
        assertThat(rest.exchange(ORDENS, HttpMethod.POST, comAuth(ordem), Map.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertThat(rest.exchange(ORDENS, HttpMethod.POST, comAuth(ordem), Map.class).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("página acima do que a consulta calcula: 200 com a lista vazia, como qualquer página depois da última")
    void paginaGigante() {
        ResponseEntity<Map> resposta = rest.exchange(ORDENS + "/pesquisa?pagina=2147483647&tamanho=100", HttpMethod.GET, comAuth(null), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) resposta.getBody().get("itens")).isEmpty();
    }

    @Test
    @DisplayName("página acima do limite na trilha de auditoria: 200 com a lista vazia")
    void paginaGiganteNaAuditoria() {
        ResponseEntity<Map> resposta = rest.exchange("/api/v1/auditoria/eventos?pagina=2147483647&tamanho=200", HttpMethod.GET, comAuth(null), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) resposta.getBody().get("itens")).isEmpty();
    }

    private static void assertErroDeEntrada(ResponseEntity<Map> resposta) {
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(resposta.getBody()).containsEntry("type", "about:blank").containsEntry("status", 400)
                .containsEntry("title", "Requisição inválida").containsEntry("detail", MENSAGEM);
    }

    private static HttpHeaders json() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpEntity<Object> comAuth(Object corpo) {
        HttpHeaders headers = json();
        headers.setBearerAuth(token);
        return new HttpEntity<>(corpo, headers);
    }

    private static String idDe(ResponseEntity<Map> resposta) {
        if (resposta.getHeaders().getLocation() != null) {
            String location = resposta.getHeaders().getLocation().toString();
            return location.substring(location.lastIndexOf('/') + 1);
        }
        return (String) resposta.getBody().get("id");
    }
}
