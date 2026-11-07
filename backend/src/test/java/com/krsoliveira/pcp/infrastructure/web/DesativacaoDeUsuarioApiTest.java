package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.BancoDeTesteIsolado;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Desativar e reativar usuário pela API: quem pode, o efeito no login e na sessão aberta, e a corrida entre os dois
 * últimos Administradores ativos.
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
class DesativacaoDeUsuarioApiTest {

    private static final String ACESSOS = "/api/v1/acessos";
    private static final String VISUALIZADOR = "00000000-0000-0000-0000-000000000002";
    private static final String ADMINISTRADOR = "00000000-0000-0000-0000-000000000001";
    private static final String SENHA = "Provisoria@123";
    private static final String SENHA_NOVA = "Definitiva@456";

    @Autowired
    private TestRestTemplate rest;

    private String admin;
    private int sequencia = 0;

    @BeforeAll
    void entrar() {
        admin = token("admin@pcp.com", "Senha@123");
    }

    @Test
    @DisplayName("sem login 401; sem USUARIO_GERENCIAR 403; com a permissão 204")
    void autorizacao() {
        String alvo = novoUsuario(VISUALIZADOR).id();
        String visualizador = usuarioQueJaEntrou(VISUALIZADOR).token();

        assertThat(rest.exchange(ACESSOS + "/usuarios/" + alvo + "/desativar", HttpMethod.POST, new HttpEntity<>(json()),
                String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(acao(visualizador, alvo, "desativar")).isEqualTo(403);
        assertThat(acao(admin, alvo, "desativar")).isEqualTo(204);
        assertThat(acao(admin, alvo, "reativar")).isEqualTo(204);
    }

    @Test
    @DisplayName("desativado não entra: mesma resposta de senha errada, para não revelar que a conta existe")
    void desativadoNaoEntra() {
        Credenciais ana = usuarioQueJaEntrou(VISUALIZADOR);
        assertThat(acao(admin, ana.id(), "desativar")).isEqualTo(204);

        ResponseEntity<Map> comSenhaCerta = login(ana.email(), SENHA_NOVA);
        ResponseEntity<Map> comSenhaErrada = login(ana.email(), "Errada@999");

        assertThat(comSenhaCerta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(comSenhaCerta.getBody()).containsEntry("detail", "E-mail ou senha incorretos.");
        assertThat(comSenhaErrada.getBody().get("detail")).isEqualTo(comSenhaCerta.getBody().get("detail"));
    }

    @Test
    @DisplayName("a sessão aberta cai na requisição seguinte à desativação e volta a valer depois de reativado")
    void sessaoAbertaCai() {
        Credenciais ana = usuarioQueJaEntrou(VISUALIZADOR);
        assertThat(eu(ana.token())).isEqualTo(200);

        acao(admin, ana.id(), "desativar");
        assertThat(eu(ana.token())).isEqualTo(401);

        acao(admin, ana.id(), "reativar");
        assertThat(login(ana.email(), SENHA_NOVA).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("a listagem de usuários diz quem está ativo")
    void listagemDizASituacao() {
        Credenciais ana = novoUsuario(VISUALIZADOR);
        acao(admin, ana.id(), "desativar");

        List<Map<String, Object>> lista = rest.exchange(ACESSOS + "/usuarios", HttpMethod.GET, comAuth(admin, null), List.class)
                .getBody();

        assertThat(lista).filteredOn(u -> ana.id().equals(u.get("id"))).singleElement()
                .satisfies(u -> assertThat(u).containsEntry("ativo", false));
        assertThat(lista).filteredOn(u -> "admin@pcp.com".equals(u.get("email"))).singleElement()
                .satisfies(u -> assertThat(u).containsEntry("ativo", true));
    }

    @Test
    @DisplayName("o gestor não desativa o próprio usuário (403)")
    void naoDesativaASiMesmo() {
        List<Map<String, Object>> lista = rest.exchange(ACESSOS + "/usuarios", HttpMethod.GET, comAuth(admin, null), List.class)
                .getBody();
        String eu = (String) lista.stream().filter(u -> "admin@pcp.com".equals(u.get("email"))).findFirst().orElseThrow().get("id");

        assertThat(acao(admin, eu, "desativar")).isEqualTo(403);
    }

    @RepeatedTest(5)
    @DisplayName("os dois últimos Administradores ativos desativam um ao outro ao mesmo tempo: um continua ativo")
    void doisUltimosAdministradores() throws Exception {
        Credenciais ana = usuarioQueJaEntrou(ADMINISTRADOR);
        Credenciais bia = usuarioQueJaEntrou(ADMINISTRADOR);
        deixarAtivosSo(ana, ana.id(), bia.id());

        List<Integer> respostas = emParalelo(() -> acao(ana.token(), bia.id(), "desativar"),
                () -> acao(bia.token(), ana.id(), "desativar"));

        assertThat(respostas).containsExactlyInAnyOrder(204, 422);
        reativarTodos(respostas.get(0) == 204 ? ana : bia);
    }

    /** Desativa os demais Administradores ativos, para sobrarem só os informados. */
    private void deixarAtivosSo(Credenciais gestor, String... manter) {
        List<String> preservar = List.of(manter);
        for (Map<String, Object> u : usuarios(gestor.token())) {
            if (ADMINISTRADOR.equals(u.get("perfilId")) && Boolean.TRUE.equals(u.get("ativo")) && !preservar.contains(u.get("id"))) {
                assertThat(acao(gestor.token(), (String) u.get("id"), "desativar")).isEqualTo(204);
            }
        }
    }

    /** Devolve o estado inicial para os outros testes, que usam o Administrador inicial. */
    private void reativarTodos(Credenciais quemFicouAtivo) {
        for (Map<String, Object> u : usuarios(quemFicouAtivo.token())) {
            if (!Boolean.TRUE.equals(u.get("ativo"))) {
                assertThat(acao(quemFicouAtivo.token(), (String) u.get("id"), "reativar")).isEqualTo(204);
            }
        }
    }

    private List<Map<String, Object>> usuarios(String token) {
        return rest.exchange(ACESSOS + "/usuarios", HttpMethod.GET, comAuth(token, null), List.class).getBody();
    }

    private record Credenciais(String id, String email, String token) {}

    /** Usuário criado pelo gestor: a senha é provisória, então ele ainda não usa o sistema. */
    private Credenciais novoUsuario(String perfilId) {
        String email = "usuario" + (++sequencia) + "@pcp.com";
        ResponseEntity<Map> criado = rest.postForEntity(ACESSOS + "/usuarios", comAuth(admin, Map.of(
                "nome", "Usuário " + sequencia, "email", email, "senhaProvisoria", SENHA, "perfilId", perfilId)), Map.class);
        assertThat(criado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return new Credenciais((String) criado.getBody().get("id"), email, null);
    }

    /** Usuário que já trocou a senha provisória e tem um token de uso normal. */
    private Credenciais usuarioQueJaEntrou(String perfilId) {
        Credenciais novo = novoUsuario(perfilId);
        String provisorio = token(novo.email(), SENHA);
        assertThat(rest.exchange(ACESSOS + "/minha-senha", HttpMethod.PUT,
                comAuth(provisorio, Map.of("senhaAtual", SENHA, "novaSenha", SENHA_NOVA)), String.class)
                .getStatusCode().is2xxSuccessful()).isTrue();
        return new Credenciais(novo.id(), novo.email(), token(novo.email(), SENHA_NOVA));
    }

    private int acao(String token, String usuario, String acao) {
        return rest.exchange(ACESSOS + "/usuarios/" + usuario + "/" + acao, HttpMethod.POST, comAuth(token, null), String.class)
                .getStatusCode().value();
    }

    private int eu(String token) {
        return rest.exchange(ACESSOS + "/eu", HttpMethod.GET, comAuth(token, null), String.class).getStatusCode().value();
    }

    private ResponseEntity<Map> login(String email, String senha) {
        return rest.postForEntity("/api/v1/auth/login", new HttpEntity<>(Map.of("email", email, "senha", senha), json()), Map.class);
    }

    private String token(String email, String senha) {
        ResponseEntity<Map> resposta = login(email, senha);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) resposta.getBody().get("token");
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
