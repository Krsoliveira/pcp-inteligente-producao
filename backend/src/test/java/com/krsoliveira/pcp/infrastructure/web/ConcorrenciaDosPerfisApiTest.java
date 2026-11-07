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
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;

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
 * Excluir um perfil enquanto alguém o atribui a um usuário. Sem trava, a exclusão contava zero usuários, a atribuição
 * gravava, e a chave estrangeira derrubava uma das duas com erro do sistema (500). Repetido porque depende do encontro.
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
class ConcorrenciaDosPerfisApiTest {

    private static final int REPETICOES = 5;
    private static final String ACESSOS = "/api/v1/acessos";
    private static final String VISUALIZADOR = "00000000-0000-0000-0000-000000000002";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ApplicationContext contexto;

    private String token;
    private int sequencia = 0;

    @BeforeAll
    void entrar() {
        token = (String) rest.postForEntity("/api/v1/auth/login",
                new HttpEntity<>(Map.of("email", "admin@pcp.com", "senha", "Senha@123"), json()), Map.class)
                .getBody().get("token");
    }

    @RepeatedTest(REPETICOES)
    @DisplayName("excluir e atribuir o mesmo perfil ao mesmo tempo: uma vence e a outra é recusada, nunca erro do sistema")
    void exclusaoContraAtribuicao() throws Exception {
        String perfil = novoPerfil();
        String usuario = novoUsuario();

        List<Integer> respostas = emParalelo(
                () -> rest.exchange(ACESSOS + "/perfis/" + perfil, HttpMethod.DELETE, comAuth(null), String.class)
                        .getStatusCode().value(),
                () -> rest.exchange(ACESSOS + "/usuarios/" + usuario + "/perfil", HttpMethod.PATCH,
                        comAuth(Map.of("perfilId", perfil)), String.class).getStatusCode().value());

        // Excluiu primeiro: a atribuição não acha o perfil (404). Atribuiu primeiro: a exclusão vê o usuário (422).
        assertThat(respostas).isIn(List.of(204, 404), List.of(422, 200));
    }

    @Test
    @DisplayName("o login usa um AuthenticationManager montado na configuração, sem AuthenticationProvider solto em bean")
    void semProviderEmBean() {
        // Com o provider em bean, o Spring avisava na subida que ignorava o UserDetailsService (dois caminhos de login).
        assertThat(contexto.getBeansOfType(AuthenticationProvider.class)).isEmpty();
    }

    private String novoPerfil() {
        ResponseEntity<Map> criado = rest.postForEntity(ACESSOS + "/perfis", comAuth(Map.of(
                "nome", "Perfil disputado " + (++sequencia), "descricao", "", "permissoes", List.of("ORDEM_VISUALIZAR"))), Map.class);
        assertThat(criado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) criado.getBody().get("id");
    }

    private String novoUsuario() {
        ResponseEntity<Map> criado = rest.postForEntity(ACESSOS + "/usuarios", comAuth(Map.of(
                "nome", "Usuário " + (++sequencia), "email", "usuario" + sequencia + "@pcp.com",
                "senhaProvisoria", "Provisoria@123", "perfilId", VISUALIZADOR)), Map.class);
        assertThat(criado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) criado.getBody().get("id");
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
