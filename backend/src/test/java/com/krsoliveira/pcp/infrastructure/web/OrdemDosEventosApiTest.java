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
 * Ordem dos eventos da trilha de auditoria: do mais recente ao mais antigo (padrão, como sempre foi) ou do mais antigo
 * ao mais recente (histórico lido como o fluxo do registro). Prova com PostgreSQL real, num registro com três eventos:
 * a versão da lista técnica é criada, ativada e retirada de uso.
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
class OrdemDosEventosApiTest {

    private static final String EVENTOS = "/api/v1/auditoria/eventos";
    private static final String LISTAS = "/api/v1/listas-tecnicas";

    @Autowired
    private TestRestTemplate rest;

    private String token;
    private String idLista;

    @BeforeAll
    void prepararMassa() {
        token = (String) rest.postForEntity("/api/v1/auth/login",
                semAuth(Map.of("email", "admin@pcp.com", "senha", "Senha@123")), Map.class).getBody().get("token");
        String aco = idDe(rest.postForEntity("/api/v1/materiais", comAuth(Map.of(
                "descricao", "Aço", "tipo", "MATERIA_PRIMA", "unidadeDeMedida", "kg")), Void.class));
        String viga = idDe(rest.postForEntity("/api/v1/materiais", comAuth(Map.of(
                "descricao", "Viga", "tipo", "PRODUTO_ACABADO", "unidadeDeMedida", "un")), Void.class));
        idLista = idDe(rest.postForEntity(LISTAS, comAuth(Map.of("materialId", viga, "versao", "v1",
                "itens", List.of(Map.of("materialComponenteId", aco, "quantidadePlanejada", 1.0, "unidadeDeMedida", "kg")))),
                Void.class));
        assertThat(rest.exchange(LISTAS + "/" + idLista + "/ativar", HttpMethod.PATCH, comAuth(null), Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(rest.exchange(LISTAS + "/" + idLista + "/retirar-de-uso", HttpMethod.PATCH, comAuth(null), Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("sem informar a ordem, vem do mais recente ao mais antigo (padrão que já existia)")
    void padraoDoMaisRecenteAoMaisAntigo() {
        assertThat(acoes("")).containsExactly("OBSOLETADA", "ATIVADA", "CRIADO");
    }

    @Test
    @DisplayName("ANTIGOS_PRIMEIRO traz o fluxo do registro: criado, ativado, retirado de uso")
    void doMaisAntigoAoMaisRecente() {
        assertThat(acoes("&ordem=ANTIGOS_PRIMEIRO")).containsExactly("CRIADO", "ATIVADA", "OBSOLETADA");
    }

    @Test
    @DisplayName("RECENTES_PRIMEIRO pedido de forma explícita é igual ao padrão")
    void recentesPrimeiroExplicito() {
        assertThat(acoes("&ordem=RECENTES_PRIMEIRO")).containsExactly("OBSOLETADA", "ATIVADA", "CRIADO");
    }

    @Test
    @DisplayName("a paginação respeita a ordem pedida: a primeira página crescente começa pelo evento mais antigo")
    void paginacaoNaOrdemPedida() {
        assertThat(acoes("&ordem=ANTIGOS_PRIMEIRO&tamanho=1")).containsExactly("CRIADO");
        assertThat(acoes("&ordem=ANTIGOS_PRIMEIRO&tamanho=1&pagina=2")).containsExactly("OBSOLETADA");
    }

    @Test
    @DisplayName("ordem desconhecida devolve 400, e sem token continua 401")
    void entradasInvalidas() {
        assertThat(rest.exchange(consulta("&ordem=ALEATORIA"), HttpMethod.GET, comAuth(null), Map.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rest.exchange(consulta("&ordem=ANTIGOS_PRIMEIRO"), HttpMethod.GET, semAuth(null), Map.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ------------------------------------------------------------------ apoio

    private String consulta(String complemento) {
        return EVENTOS + "?tipoEntidade=LISTA_TECNICA&entidadeId=" + idLista + complemento;
    }

    @SuppressWarnings("unchecked")
    private List<String> acoes(String complemento) {
        ResponseEntity<Map> resposta = rest.exchange(consulta(complemento), HttpMethod.GET, comAuth(null), Map.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return ((List<Map<String, Object>>) resposta.getBody().get("itens")).stream()
                .map(evento -> (String) evento.get("acao")).toList();
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
