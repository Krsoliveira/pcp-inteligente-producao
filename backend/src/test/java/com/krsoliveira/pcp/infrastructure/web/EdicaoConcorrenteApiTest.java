package com.krsoliveira.pcp.infrastructure.web;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Edição simultânea do mesmo registro (ADR-0034): quem salva sobre uma versão que já não é a atual recebe 409 e não
 * apaga a alteração de quem salvou antes. A corrida de verdade só aparece no PostgreSQL real; repetida porque depende
 * do encontro entre as duas requisições.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.admin.nome=Administrador de Teste",
                "app.admin.email=admin@pcp.com",
                "app.admin.senha=Senha@123",
                "pcp.seguranca.rate-limit.login=1000"
        })
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings({"unchecked", "rawtypes"})
@AutoConfigureTestRestTemplate
class EdicaoConcorrenteApiTest {

    private static final int REPETICOES = 5;
    private static final String PERFIS = "/api/v1/acessos/perfis";
    private static final String TIPOS = "/api/v1/tipos-ordem";
    private static final List<String> PERMISSOES = List.of("ORDEM_VISUALIZAR", "ORDEM_CRIAR");

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private TestRestTemplate rest;

    private String token;
    private int sequencia = 0;

    @BeforeAll
    void entrar() {
        token = (String) rest.postForEntity("/api/v1/auth/login",
                new HttpEntity<>(Map.of("email", "admin@pcp.com", "senha", "Senha@123"), json()), Map.class)
                .getBody().get("token");
    }

    @Nested
    @DisplayName("Perfil de acesso")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Perfil {

        @Test
        @DisplayName("nasce na versão 0, e a listagem e a busca devolvem a versão")
        void devolveAVersao() {
            String id = novoPerfil();

            assertThat(numero(buscar(PERFIS, id).get("versao"))).isZero();
            List<Map<String, Object>> lista = rest.exchange(PERFIS, HttpMethod.GET, comAuth(null), List.class).getBody();
            assertThat(lista).filteredOn(p -> id.equals(p.get("id"))).singleElement()
                    .satisfies(p -> assertThat(numero(p.get("versao"))).isZero());
        }

        @Test
        @DisplayName("com a versão atual, altera e devolve a versão seguinte")
        void versaoAtualAltera() {
            String id = novoPerfil();

            ResponseEntity<Map> resposta = alterarPerfil(id, "primeira", 0L);

            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(numero(resposta.getBody().get("versao"))).isEqualTo(1);
            assertThat(resposta.getBody().get("descricao")).isEqualTo("primeira");
        }

        @Test
        @DisplayName("com versão antiga, responde 409 em RFC 9457 e mantém a alteração de quem salvou antes")
        void versaoAntigaRecusada() {
            String id = novoPerfil();
            assertThat(alterarPerfil(id, "da Ana", 0L).getStatusCode()).isEqualTo(HttpStatus.OK);

            ResponseEntity<Map> resposta = alterarPerfil(id, "do Bruno", 0L);

            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(resposta.getHeaders().getContentType().toString()).contains("application/problem+json");
            assertThat((String) resposta.getBody().get("detail")).contains("foi alterado por outra pessoa");
            assertThat(resposta.getBody().get("codigo")).isEqualTo("EDICAO_CONCORRENTE");
            Map<String, Object> atual = buscar(PERFIS, id);
            assertThat(atual.get("descricao")).isEqualTo("da Ana");
            assertThat(numero(atual.get("versao"))).isEqualTo(1);
        }

        @Test
        @DisplayName("nome repetido continua 409 sem o código de edição simultânea")
        void nomeRepetidoSemCodigo() {
            String outro = novoPerfil();
            String id = novoPerfil();
            Map<String, Object> corpo = new HashMap<>(Map.of("nome", nomeAtual(PERFIS, outro), "descricao", "",
                    "permissoes", PERMISSOES, "versao", 0L));

            ResponseEntity<Map> resposta = rest.exchange(PERFIS + "/" + id, HttpMethod.PUT, comAuth(corpo), Map.class);

            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(resposta.getBody()).doesNotContainKey("codigo");
        }

        @Test
        @DisplayName("sem versão no corpo (cliente antigo), altera como antes")
        void semVersaoCompativel() {
            String id = novoPerfil();

            assertThat(alterarPerfil(id, "sem versão", null).getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(buscar(PERFIS, id).get("descricao")).isEqualTo("sem versão");
        }

        @Test
        @DisplayName("versão negativa é dado inválido (400)")
        void versaoNegativaInvalida() {
            assertThat(alterarPerfil(novoPerfil(), "x", -1L).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @RepeatedTest(REPETICOES)
        @DisplayName("duas alterações ao mesmo tempo sobre a mesma versão: uma grava, a outra recebe 409")
        void alteracaoSimultanea() throws Exception {
            String id = novoPerfil();

            assertThat(emParalelo(() -> alterarPerfil(id, "A", 0L).getStatusCode().value(),
                    () -> alterarPerfil(id, "B", 0L).getStatusCode().value()))
                    .containsExactlyInAnyOrder(200, 409);
            assertThat(numero(buscar(PERFIS, id).get("versao"))).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Tipo de ordem")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Tipo {

        @Test
        @DisplayName("nasce na versão 0, e a listagem e a busca devolvem a versão")
        void devolveAVersao() {
            String id = novoTipo();

            assertThat(numero(buscar(TIPOS, id).get("versao"))).isZero();
            List<Map<String, Object>> lista = rest.exchange(TIPOS, HttpMethod.GET, comAuth(null), List.class).getBody();
            assertThat(lista).filteredOn(t -> id.equals(t.get("id"))).singleElement()
                    .satisfies(t -> assertThat(numero(t.get("versao"))).isZero());
        }

        @Test
        @DisplayName("com versão antiga, responde 409 e mantém a alteração de quem salvou antes")
        void versaoAntigaRecusada() {
            String id = novoTipo();
            ResponseEntity<Map> daAna = alterarTipo(id, "da Ana", 0L);
            assertThat(daAna.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(numero(daAna.getBody().get("versao"))).isEqualTo(1);

            ResponseEntity<Map> resposta = alterarTipo(id, "do Bruno", 0L);

            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat((String) resposta.getBody().get("detail")).contains("foi alterado por outra pessoa");
            assertThat(buscar(TIPOS, id).get("descricao")).isEqualTo("da Ana");
        }

        @Test
        @DisplayName("sem versão no corpo (cliente antigo), altera como antes")
        void semVersaoCompativel() {
            String id = novoTipo();

            assertThat(alterarTipo(id, "sem versão", null).getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @RepeatedTest(REPETICOES)
        @DisplayName("duas alterações ao mesmo tempo sobre a mesma versão: uma grava, a outra recebe 409")
        void alteracaoSimultanea() throws Exception {
            String id = novoTipo();

            assertThat(emParalelo(() -> alterarTipo(id, "A", 0L).getStatusCode().value(),
                    () -> alterarTipo(id, "B", 0L).getStatusCode().value()))
                    .containsExactlyInAnyOrder(200, 409);
            assertThat(numero(buscar(TIPOS, id).get("versao"))).isEqualTo(1);
        }
    }

    private String novoPerfil() {
        ResponseEntity<Map> criado = rest.postForEntity(PERFIS, comAuth(Map.of(
                "nome", "Perfil concorrente " + (++sequencia), "descricao", "", "permissoes", PERMISSOES)), Map.class);
        assertThat(criado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) criado.getBody().get("id");
    }

    private String novoTipo() {
        ResponseEntity<Map> criado = rest.postForEntity(TIPOS, comAuth(Map.of(
                "nome", "Tipo concorrente " + (++sequencia), "cor", "#1565c0")), Map.class);
        assertThat(criado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) criado.getBody().get("id");
    }

    private ResponseEntity<Map> alterarPerfil(String id, String descricao, Long versao) {
        Map<String, Object> corpo = new HashMap<>(Map.of("nome", nomeAtual(PERFIS, id), "descricao", descricao,
                "permissoes", PERMISSOES));
        corpo.put("versao", versao);
        return rest.exchange(PERFIS + "/" + id, HttpMethod.PUT, comAuth(corpo), Map.class);
    }

    private ResponseEntity<Map> alterarTipo(String id, String descricao, Long versao) {
        Map<String, Object> corpo = new HashMap<>(Map.of("nome", nomeAtual(TIPOS, id), "descricao", descricao,
                "cor", "#2e7d32"));
        corpo.put("versao", versao);
        return rest.exchange(TIPOS + "/" + id, HttpMethod.PUT, comAuth(corpo), Map.class);
    }

    private String nomeAtual(String recurso, String id) {
        return (String) buscar(recurso, id).get("nome");
    }

    private Map<String, Object> buscar(String recurso, String id) {
        return rest.exchange(recurso + "/" + id, HttpMethod.GET, comAuth(null), Map.class).getBody();
    }

    private static long numero(Object valor) {
        return ((Number) valor).longValue();
    }

    private List<Integer> emParalelo(Supplier<Integer> primeira, Supplier<Integer> segunda) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch largada = new CountDownLatch(1);
            List<Future<Integer>> futuros = new ArrayList<>();
            for (Supplier<Integer> chamada : List.of(primeira, segunda)) {
                futuros.add(pool.submit(() -> {
                    largada.await();
                    return chamada.get();
                }));
            }
            largada.countDown();
            List<Integer> respostas = new ArrayList<>();
            for (Future<Integer> futuro : futuros) {
                respostas.add(futuro.get());
            }
            return respostas;
        } finally {
            pool.shutdown();
        }
    }

    private HttpEntity<Object> comAuth(Object corpo) {
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
