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
 * ADR-0019, de ponta a ponta com PostgreSQL real: várias versões ativas ao mesmo tempo, retirada de
 * uso decidida pelo usuário, escolha validada na ordem e comparação entre versões.
 *
 * Massa: a Viga tem as versões v1 (aço 2,5 kg + tinta 1 l) e v2 (aço 3 kg + parafuso 8 un), ambas
 * ativas, e uma v3 em revisão; a Chapa tem a própria lista (v1 ativa).
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "pcp.seguranca.rate-limit.login=1000",
                "pcp.seguranca.rate-limit.registrar=1000"
        })
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VersoesDeListaTecnicaApiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String LISTAS = "/api/v1/listas-tecnicas";
    private static final String ORDENS = "/api/v1/ordens-producao";

    @Autowired
    private TestRestTemplate rest;

    private String token;
    private String idAco;
    private String idTinta;
    private String idParafuso;
    private String idViga;
    private String idV1;
    private String idV2;
    private String idV3;
    private String idChapaV1;

    @BeforeAll
    void prepararMassa() {
        rest.postForEntity("/api/v1/auth/registrar", semAuth(Map.of(
                "nome", "Planejador", "email", "versoes@pcp.com", "senha", "Senha@123")), Map.class);
        token = (String) rest.postForEntity("/api/v1/auth/login",
                semAuth(Map.of("email", "versoes@pcp.com", "senha", "Senha@123")), Map.class).getBody().get("token");

        idAco = criarMaterial("Aço inox", "MATERIA_PRIMA", "kg");
        idTinta = criarMaterial("Tinta", "MATERIA_PRIMA", "l");
        idParafuso = criarMaterial("Parafuso", "MATERIA_PRIMA", "un");
        idViga = criarMaterial("Viga metálica", "PRODUTO_ACABADO", "un");
        String idChapa = criarMaterial("Chapa dobrada", "PRODUTO_ACABADO", "un");

        idV1 = cadastrarVersao(idViga, "v1", item(idAco, 2.5, "kg"), item(idTinta, 1.0, "l"));
        idV2 = cadastrarVersao(idViga, "v2", item(idAco, 3.0, "kg"), item(idParafuso, 8.0, "un"));
        idV3 = cadastrarVersao(idViga, "v3", item(idAco, 3.0, "kg"));
        idChapaV1 = cadastrarVersao(idChapa, "v1", item(idAco, 1.0, "kg"));

        ativar(idV1);
        ativar(idV2);
        ativar(idChapaV1);
    }

    @Test
    @DisplayName("as versões do mesmo material compartilham o listaId; materiais diferentes têm listas diferentes")
    void listaIdPorMaterial() {
        Map<String, Object> v1 = buscar(idV1);
        Map<String, Object> v2 = buscar(idV2);
        Map<String, Object> chapa = buscar(idChapaV1);

        assertThat(v1.get("listaId")).isNotNull().isEqualTo(v2.get("listaId"));
        assertThat(v1.get("id")).isNotEqualTo(v2.get("id"));
        assertThat(chapa.get("listaId")).isNotEqualTo(v1.get("listaId"));
    }

    @Test
    @DisplayName("ativar uma versão não obsoleta as outras: v1 e v2 ficam ativas ao mesmo tempo")
    void variasVersoesAtivas() {
        assertThat(buscar(idV1)).containsEntry("status", "ATIVA");
        assertThat(buscar(idV2)).containsEntry("status", "ATIVA");
        assertThat(buscar(idV3)).containsEntry("status", "EM_REVISAO");
    }

    @Test
    @DisplayName("ordem aceita qualquer versão ativa, recusa a em revisão (422) e a de outro material (422)")
    void escolhaDaVersaoNaOrdem() {
        assertThat(criarOrdem("OP-V-001", idViga, idV1).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(criarOrdem("OP-V-002", idViga, idV2).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(criarOrdem("OP-V-003", idViga, idV3).getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(criarOrdem("OP-V-004", idViga, idChapaV1).getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("comparar v1 com v2 mostra adicionado, removido, alterado e a ordem de a e b")
    @SuppressWarnings("unchecked")
    void comparaVersoes() {
        ResponseEntity<Map> resposta = comparar(idV1, idV2);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> corpo = resposta.getBody();
        assertThat(corpo).containsEntry("temDiferencas", true);
        assertThat((Map<String, Object>) corpo.get("versaoA")).containsEntry("versao", "v1");
        assertThat((Map<String, Object>) corpo.get("versaoB")).containsEntry("versao", "v2");
        assertThat(componentes(corpo, "adicionados")).containsExactly(idParafuso);
        assertThat(componentes(corpo, "removidos")).containsExactly(idTinta);
        assertThat(componentes(corpo, "alterados")).containsExactly(idAco);
        assertThat(componentes(corpo, "iguais")).isEmpty();
        Map<String, Object> aco = ((List<Map<String, Object>>) corpo.get("alterados")).getFirst();
        assertThat(((Number) aco.get("quantidadeA")).doubleValue()).isEqualTo(2.5);
        assertThat(((Number) aco.get("quantidadeB")).doubleValue()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("comparar v2 com v3 (mesmo aço) separa o que ficou igual do que foi removido")
    @SuppressWarnings("unchecked")
    void comparaComComponenteIgual() {
        Map<String, Object> corpo = comparar(idV2, idV3).getBody();

        assertThat(componentes(corpo, "iguais")).containsExactly(idAco);
        assertThat(componentes(corpo, "removidos")).containsExactly(idParafuso);
        assertThat(componentes(corpo, "adicionados")).isEmpty();
    }

    @Test
    @DisplayName("comparar versões de listas diferentes ou a mesma versão duas vezes devolve 422")
    void comparacaoInvalida() {
        assertThat(comparar(idV1, idChapaV1).getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(comparar(idV1, idV1).getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("comparar versão inexistente devolve 404, parâmetro ausente 400 e sem token 401")
    void comparacaoEntradasInvalidas() {
        assertThat(comparar(idV1, "00000000-0000-0000-0000-000000000000").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(rest.exchange(LISTAS + "/comparar?a=" + idV1, HttpMethod.GET, comAuth(null), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rest.getForEntity(LISTAS + "/comparar?a=" + idV1 + "&b=" + idV2, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("retirar de uso: só a versão escolhida vira OBSOLETA, a outra segue ativa e a ordem recusa a retirada")
    void retirarDeUso() {
        String idV4 = cadastrarVersao(idViga, "v4", item(idAco, 9.0, "kg"));
        ativar(idV4);
        assertThat(criarOrdem("OP-V-010", idViga, idV4).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<Void> resposta = rest.exchange(LISTAS + "/" + idV4 + "/retirar-de-uso", HttpMethod.PATCH,
                comAuth(null), Void.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(buscar(idV4)).containsEntry("status", "OBSOLETA");
        assertThat(buscar(idV2)).containsEntry("status", "ATIVA");
        assertThat(criarOrdem("OP-V-011", idViga, idV4).getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("retirar de uso uma versão que não está ativa devolve 422; versão inexistente, 404; sem token, 401")
    void retirarDeUsoInvalido() {
        assertThat(rest.exchange(LISTAS + "/" + idV3 + "/retirar-de-uso", HttpMethod.PATCH, comAuth(null), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(rest.exchange(LISTAS + "/00000000-0000-0000-0000-000000000000/retirar-de-uso", HttpMethod.PATCH,
                comAuth(null), Map.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(rest.exchange(LISTAS + "/" + idV1 + "/retirar-de-uso", HttpMethod.PATCH, semAuth(null), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ------------------------------------------------------------------ apoio

    @SuppressWarnings("unchecked")
    private List<String> componentes(Map<String, Object> corpo, String grupo) {
        return ((List<Map<String, Object>>) corpo.get(grupo)).stream()
                .map(i -> (String) i.get("materialComponenteId")).toList();
    }

    private ResponseEntity<Map> comparar(String a, String b) {
        return rest.exchange(LISTAS + "/comparar?a=" + a + "&b=" + b, HttpMethod.GET, comAuth(null), Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buscar(String id) {
        return rest.exchange(LISTAS + "/" + id, HttpMethod.GET, comAuth(null), Map.class).getBody();
    }

    private void ativar(String id) {
        ResponseEntity<Void> resposta = rest.exchange(LISTAS + "/" + id + "/ativar", HttpMethod.PATCH,
                comAuth(null), Void.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private static Map<String, Object> item(String componenteId, double quantidade, String unidade) {
        return Map.of("materialComponenteId", componenteId, "quantidadePlanejada", quantidade,
                "unidadeDeMedida", unidade);
    }

    @SafeVarargs
    private String cadastrarVersao(String materialId, String versao, Map<String, Object>... itens) {
        return idDe(rest.postForEntity(LISTAS, comAuth(Map.of(
                "materialId", materialId, "versao", versao, "itens", List.of(itens))), Void.class));
    }

    private String criarMaterial(String descricao, String tipo, String unidade) {
        return idDe(rest.postForEntity("/api/v1/materiais", comAuth(Map.of(
                "descricao", descricao, "tipo", tipo, "unidadeDeMedida", unidade)), Void.class));
    }

    private ResponseEntity<Map> criarOrdem(String codigo, String materialId, String listaId) {
        return rest.postForEntity(ORDENS, comAuth(Map.of(
                "codigo", codigo, "materialId", materialId, "listaTecnicaId", listaId,
                "centroDeTrabalho", "Usinagem", "quantidade", 10,
                "inicioPlanejado", "2999-01-01", "fimPlanejado", "2999-01-10")), Map.class);
    }

    private static String idDe(ResponseEntity<?> resposta) {
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
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
