package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.BancoDeTesteIsolado;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** A pesquisa paginada de lotes com PostgreSQL real: filtros, limite de tamanho e permissão. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.admin.nome=Administrador de Teste",
                "app.admin.email=admin@pcp.com",
                "app.admin.senha=Senha@123",
                "pcp.seguranca.rate-limit.login=1000",
                "pcp.seguranca.rate-limit.trocar-senha=1000"
        })
@BancoDeTesteIsolado
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureTestRestTemplate
@SuppressWarnings({"unchecked", "rawtypes"})
class PesquisaDeLotesApiTest {

    private static final String PESQUISA = "/api/v1/lotes/pesquisa";
    private static final String SENHA = "Senha@123";
    private static final int TOTAL_DE_LOTES = 3;

    @Autowired
    private TestRestTemplate rest;

    private String token;
    private String aco;
    private String cola;

    @BeforeAll
    void prepararMassa() {
        token = login("admin@pcp.com", SENHA);
        aco = material("Aço");
        cola = material("Cola");
        rest.postForEntity("/api/v1/notas-fiscais", comToken(token, Map.of(
                "fornecedor", "Fornecedor", "numero", "NF-PL", "dataEmissao", "2026-08-01", "dataRecebimento", "2026-08-02",
                "itens", List.of(
                        item(aco, "ACO-1"),
                        item(aco, "ACO-2"),
                        item(cola, "COLA-1")))), Void.class);
    }

    @Test
    @DisplayName("sem filtro, devolve a página com o total; os mais recentes vêm primeiro")
    void semFiltro() {
        Map<String, Object> pagina = pesquisar("?tamanho=2");

        assertThat(pagina).containsEntry("total", TOTAL_DE_LOTES).containsEntry("tamanho", 2).containsEntry("totalPaginas", 2);
        assertThat((List<?>) pagina.get("itens")).hasSize(2);
    }

    @Test
    @DisplayName("filtra por material, por status e por trecho do número, sem diferenciar maiúsculas")
    void filtros() {
        assertThat(numeros(pesquisar("?materialId=" + aco))).containsExactlyInAnyOrder("ACO-1", "ACO-2");
        assertThat(numeros(pesquisar("?busca=cola"))).containsExactly("COLA-1");
        assertThat(numeros(pesquisar("?status=DISPONIVEL&materialId=" + cola))).containsExactly("COLA-1");
        assertThat(numeros(pesquisar("?status=VENCIDO"))).isEmpty();
    }

    @Test
    @DisplayName("% digitado é texto comum, não curinga")
    void buscaSemCuringa() {
        assertThat(numeros(pesquisar("?busca=%25"))).isEmpty();
    }

    @Test
    @DisplayName("tamanho acima do máximo é corrigido: ninguém pede todos os registros")
    void tamanhoLimitado() {
        assertThat(pesquisar("?tamanho=100000")).containsEntry("tamanho", 100);
    }

    @Test
    @DisplayName("sem login é 401; sem a permissão de estoque é 403")
    void permissao() {
        assertThat(rest.getForEntity(PESQUISA, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        String perfil = (String) rest.exchange("/api/v1/acessos/perfis", HttpMethod.POST, comToken(token,
                Map.of("nome", "Só ordens", "descricao", "", "permissoes", List.of("ORDEM_VISUALIZAR"))), Map.class).getBody().get("id");
        rest.postForEntity("/api/v1/acessos/usuarios", comToken(token, Map.of("nome", "Operador", "email", "operador@pcp.com",
                "senhaProvisoria", SENHA, "perfilId", perfil)), Map.class);
        rest.exchange("/api/v1/acessos/minha-senha", HttpMethod.PUT, comToken(login("operador@pcp.com", SENHA),
                Map.of("senhaAtual", SENHA, "novaSenha", SENHA + "!nova")), Void.class);
        String semPermissao = login("operador@pcp.com", SENHA + "!nova");

        assertThat(rest.exchange(PESQUISA, HttpMethod.GET, comToken(semPermissao, null), String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private Map<String, Object> pesquisar(String parametros) {
        ResponseEntity<Map> resposta = rest.exchange(PESQUISA + parametros, HttpMethod.GET, comToken(token, null), Map.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resposta.getBody();
    }

    private static List<String> numeros(Map<String, Object> pagina) {
        return ((List<Map<String, Object>>) pagina.get("itens")).stream().map(l -> (String) l.get("numeroLote")).toList();
    }

    private static Map<String, Object> item(String materialId, String numero) {
        return Map.of("materialId", materialId, "quantidade", 10, "numeroLote", numero, "dataFabricacao", "2026-07-01", "dataValidade", "2031-01-01");
    }

    private String material(String descricao) {
        ResponseEntity<Void> resposta = rest.postForEntity("/api/v1/materiais",
                comToken(token, Map.of("descricao", descricao, "tipo", "MATERIA_PRIMA", "unidadeDeMedida", "kg")), Void.class);
        String caminho = resposta.getHeaders().getLocation().getPath();
        return caminho.substring(caminho.lastIndexOf('/') + 1);
    }

    private String login(String email, String senha) {
        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.setContentType(MediaType.APPLICATION_JSON);
        return (String) rest.postForEntity("/api/v1/auth/login", new HttpEntity<>(Map.of("email", email, "senha", senha), cabecalhos), Map.class)
                .getBody().get("token");
    }

    private static HttpEntity<Object> comToken(String token, Object corpo) {
        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.setContentType(MediaType.APPLICATION_JSON);
        cabecalhos.setBearerAuth(token);
        return new HttpEntity<>(corpo, cabecalhos);
    }
}
