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
 * ADR-0037, de ponta a ponta com PostgreSQL real: a unidade vai para maiúsculas em todo ponto de entrada (material e
 * item da lista técnica), unidade fora do padrão é recusada (422) e as já cadastradas são oferecidas sem duplicidade.
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
@BancoDeTesteIsolado
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureTestRestTemplate
class UnidadesDeMedidaApiTest {

    private static final String MATERIAIS = "/api/v1/materiais";
    private static final String UNIDADES = MATERIAIS + "/unidades";
    private static final String SENHA_COMUM = "Comum@12345";
    private static final String SENHA_NOVA = "Nova@123456";

    @Autowired
    private TestRestTemplate rest;

    private String token;
    private String idAco;
    private String idViga;

    @BeforeAll
    void prepararMassa() {
        token = logar("admin@pcp.com", "Senha@123");
        idAco = criarMaterial("Aço", "MATERIA_PRIMA", " kg ");
        criarMaterial("Tinta", "MATERIA_PRIMA", "Kg");
        criarMaterial("Parafuso", "MATERIA_PRIMA", "un");
        idViga = criarMaterial("Viga", "PRODUTO_ACABADO", "UN");
        // o item da lista tem a própria unidade (g), que também entra na lista de sugestões
        ResponseEntity<Void> lista = rest.postForEntity("/api/v1/listas-tecnicas", comAuth(Map.of(
                "materialId", idViga, "versao", "v1", "itens", List.of(Map.of(
                        "materialComponenteId", idAco, "quantidadePlanejada", 500.0, "unidadeDeMedida", "g")))), Void.class);
        assertThat(lista.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("o material guarda a unidade em maiúsculas e sem espaços, qualquer que seja a escrita")
    void materialGuardaEmMaiusculas() {
        assertThat(unidadeDoMaterial(idAco)).isEqualTo("KG");
    }

    @Test
    @DisplayName("o item da lista técnica também guarda a unidade em maiúsculas")
    @SuppressWarnings("unchecked")
    void itemDaListaGuardaEmMaiusculas() {
        List<Map<String, Object>> listas = rest.exchange("/api/v1/listas-tecnicas?materialId=" + idViga, HttpMethod.GET,
                comAuth(null), List.class).getBody();

        List<Map<String, Object>> itens = (List<Map<String, Object>>) listas.getFirst().get("itens");
        assertThat(itens.getFirst()).containsEntry("unidadeDeMedida", "G");
    }

    @Test
    @DisplayName("as unidades já cadastradas vêm sem duplicidade, em ordem alfabética, incluindo as dos itens de lista")
    void listaAsUnidadesSemDuplicidade() {
        ResponseEntity<List> resposta = rest.exchange(UNIDADES, HttpMethod.GET, comAuth(null), List.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsExactly("G", "KG", "UN");
    }

    @Test
    @DisplayName("unidade fora do padrão (espaço no meio, símbolo) devolve 422; com mais de 10 caracteres, 400 da validação do corpo")
    void unidadeInvalidaEhRecusada() {
        for (String invalida : List.of("k g", "kg;")) {
            ResponseEntity<Map> resposta = rest.postForEntity(MATERIAIS, comAuth(Map.of(
                    "descricao", "Teste", "tipo", "MATERIA_PRIMA", "unidadeDeMedida", invalida)), Map.class);

            assertThat(resposta.getStatusCode()).as(invalida).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }
        assertThat(rest.postForEntity(MATERIAIS, comAuth(Map.of(
                "descricao", "Teste", "tipo", "MATERIA_PRIMA", "unidadeDeMedida", "abcdefghijk")), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("sem token a consulta devolve 401; sem a permissão de visualizar material, 403")
    void autenticacaoEAutorizacao() {
        assertThat(rest.exchange(UNIDADES, HttpMethod.GET, semAuth(null), Map.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.setBearerAuth(tokenDeQuemSoViListas());
        assertThat(rest.exchange(UNIDADES, HttpMethod.GET, new HttpEntity<>(cabecalhos), Map.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ------------------------------------------------------------------ apoio

    /** Perfil só com LISTA_VISUALIZAR_ATIVAS (sem MATERIAL_VISUALIZAR); a senha provisória já vem trocada. */
    private String tokenDeQuemSoViListas() {
        String perfil = (String) rest.postForEntity("/api/v1/acessos/perfis", comAuth(Map.of(
                "nome", "Só listas", "descricao", "", "permissoes", List.of("LISTA_VISUALIZAR_ATIVAS"))), Map.class)
                .getBody().get("id");
        String email = "so-listas@pcp.com";
        assertThat(rest.postForEntity("/api/v1/acessos/usuarios", comAuth(Map.of(
                "nome", "Só listas", "email", email, "senhaProvisoria", SENHA_COMUM, "perfilId", perfil)), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.CREATED);
        HttpHeaders provisorio = new HttpHeaders();
        provisorio.setContentType(MediaType.APPLICATION_JSON);
        provisorio.setBearerAuth(logar(email, SENHA_COMUM));
        rest.exchange("/api/v1/acessos/minha-senha", HttpMethod.PUT,
                new HttpEntity<>(Map.of("senhaAtual", SENHA_COMUM, "novaSenha", SENHA_NOVA), provisorio), String.class);
        return logar(email, SENHA_NOVA);
    }

    private String unidadeDoMaterial(String id) {
        return (String) rest.exchange(MATERIAIS + "/" + id, HttpMethod.GET, comAuth(null), Map.class)
                .getBody().get("unidadeDeMedida");
    }

    private String criarMaterial(String descricao, String tipo, String unidade) {
        ResponseEntity<Void> resposta = rest.postForEntity(MATERIAIS, comAuth(Map.of(
                "descricao", descricao, "tipo", tipo, "unidadeDeMedida", unidade)), Void.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String location = resposta.getHeaders().getLocation().toString();
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private String logar(String email, String senha) {
        return (String) rest.postForEntity("/api/v1/auth/login", semAuth(Map.of("email", email, "senha", senha)), Map.class)
                .getBody().get("token");
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
