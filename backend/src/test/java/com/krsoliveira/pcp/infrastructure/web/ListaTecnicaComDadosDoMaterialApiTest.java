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

/**
 * As respostas de listas técnicas trazem o código e a descrição do material produzido e de cada componente: quem só
 * vê listas (sem MATERIAL_VISUALIZAR) enxerga nomes, não ids internos.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.admin.nome=Administrador de Teste",
                "app.admin.email=admin@pcp.com",
                "app.admin.senha=Senha@123",
                "pcp.seguranca.rate-limit.login=1000"
        })
@BancoDeTesteIsolado
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings({"unchecked", "rawtypes"})
@AutoConfigureTestRestTemplate
class ListaTecnicaComDadosDoMaterialApiTest {

    private static final String LISTAS = "/api/v1/listas-tecnicas";
    private static final String SENHA = "Provisoria@123";
    private static final String SENHA_NOVA = "Definitiva@456";

    @Autowired
    private TestRestTemplate rest;

    private String admin;
    private String soListas;
    private String lista;
    private String produto;

    @BeforeAll
    void prepararMassa() {
        admin = token("admin@pcp.com", "Senha@123");
        String aco = material("Chapa de aço", "MATERIA_PRIMA", "kg");
        produto = material("Viga soldada", "PRODUTO_ACABADO", "un");
        lista = idDe(rest.postForEntity(LISTAS, comAuth(admin, Map.of("materialId", produto, "versao", "v1",
                "itens", List.of(Map.of("materialComponenteId", aco, "quantidadePlanejada", 2, "unidadeDeMedida", "kg")))),
                Void.class));
        soListas = usuarioComPermissoes(List.of("LISTA_VISUALIZAR_TODAS"));
    }

    @Test
    @DisplayName("quem só vê listas não consulta materiais (403), mas a lista traz o código e a descrição de tudo")
    void detalheNomeiaMaterialEComponentes() {
        assertThat(rest.exchange("/api/v1/materiais/" + produto, HttpMethod.GET, comAuth(soListas, null), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        Map<String, Object> resposta = rest.exchange(LISTAS + "/" + lista, HttpMethod.GET, comAuth(soListas, null), Map.class)
                .getBody();

        assertThat(resposta).containsEntry("materialDescricao", "Viga soldada");
        assertThat((String) resposta.get("materialCodigo")).matches("\\d{9}");
        Map<String, Object> item = ((List<Map<String, Object>>) resposta.get("itens")).getFirst();
        assertThat(item).containsEntry("componenteDescricao", "Chapa de aço");
        assertThat((String) item.get("componenteCodigo")).matches("\\d{9}");
    }

    @Test
    @DisplayName("a pesquisa paginada e a listagem por material também nomeiam o material")
    void pesquisaEListagemNomeiam() {
        Map<String, Object> pagina = rest.exchange(LISTAS + "/pesquisa", HttpMethod.GET, comAuth(soListas, null), Map.class).getBody();
        Map<String, Object> primeira = ((List<Map<String, Object>>) pagina.get("itens")).getFirst();
        assertThat(primeira).containsEntry("materialDescricao", "Viga soldada");
        assertThat(pagina).containsKeys("pagina", "tamanho", "total");

        List<Map<String, Object>> doMaterial = rest.exchange(LISTAS + "?materialId=" + produto, HttpMethod.GET,
                comAuth(soListas, null), List.class).getBody();
        assertThat(doMaterial).singleElement().satisfies(l -> assertThat(l).containsEntry("materialDescricao", "Viga soldada"));
    }

    private String usuarioComPermissoes(List<String> permissoes) {
        String perfil = (String) rest.postForEntity("/api/v1/acessos/perfis", comAuth(admin, Map.of(
                "nome", "Só listas", "descricao", "", "permissoes", permissoes)), Map.class).getBody().get("id");
        String email = "so-listas@pcp.com";
        assertThat(rest.postForEntity("/api/v1/acessos/usuarios", comAuth(admin, Map.of(
                "nome", "Só listas", "email", email, "senhaProvisoria", SENHA, "perfilId", perfil)), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String provisorio = token(email, SENHA);
        rest.exchange("/api/v1/acessos/minha-senha", HttpMethod.PUT,
                comAuth(provisorio, Map.of("senhaAtual", SENHA, "novaSenha", SENHA_NOVA)), String.class);
        return token(email, SENHA_NOVA);
    }

    private String material(String descricao, String tipo, String unidade) {
        return idDe(rest.postForEntity("/api/v1/materiais",
                comAuth(admin, Map.of("descricao", descricao, "tipo", tipo, "unidadeDeMedida", unidade)), Void.class));
    }

    private String token(String email, String senha) {
        ResponseEntity<Map> resposta = rest.postForEntity("/api/v1/auth/login",
                new HttpEntity<>(Map.of("email", email, "senha", senha), json()), Map.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) resposta.getBody().get("token");
    }

    private static String idDe(ResponseEntity<?> resposta) {
        String caminho = resposta.getHeaders().getLocation().getPath();
        return caminho.substring(caminho.lastIndexOf('/') + 1);
    }

    private HttpEntity<Object> comAuth(String token, Object corpo) {
        HttpHeaders cabecalhos = json();
        cabecalhos.setBearerAuth(token);
        return new HttpEntity<>(corpo, cabecalhos);
    }

    private static HttpHeaders json() {
        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.setContentType(MediaType.APPLICATION_JSON);
        return cabecalhos;
    }
}
