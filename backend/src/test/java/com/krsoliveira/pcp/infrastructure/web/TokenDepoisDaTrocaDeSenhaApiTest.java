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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Quem troca a senha porque suspeita de um vazamento precisa derrubar o token que está em circulação. O token guarda a
 * emissão em segundos, então os testes esperam o segundo virar antes de mudar a senha.
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
class TokenDepoisDaTrocaDeSenhaApiTest {

    private static final String ACESSOS = "/api/v1/acessos";
    private static final String SENHA = "Senha@12345";
    private static final long PARA_O_SEGUNDO_VIRAR_MS = 1_100;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private TestRestTemplate rest;

    private String admin;

    @BeforeAll
    void entrarComoAdministrador() {
        admin = login("admin@pcp.com", "Senha@123");
    }

    @Test
    @DisplayName("trocar a própria senha derruba o token antigo; o novo login funciona, e a senha antiga não")
    void trocarSenhaDerrubaTokenAntigo() throws InterruptedException {
        String email = cadastrar("troca@teste.local");
        String antigo = login(email, SENHA);
        assertThat(eu(antigo).getStatusCode()).isEqualTo(HttpStatus.OK);

        Thread.sleep(PARA_O_SEGUNDO_VIRAR_MS);
        assertThat(trocarSenha(antigo, SENHA, SENHA + "!nova").getStatusCode().is2xxSuccessful()).isTrue();

        assertThat(eu(antigo).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(eu(login(email, SENHA + "!nova")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tentarLogin(email, SENHA).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("token emitido logo depois da troca, no mesmo segundo, vale: quem acabou de trocar não fica de fora")
    void tokenDoMesmoSegundoVale() {
        String email = cadastrar("mesmo.segundo@teste.local");
        trocarSenha(login(email, SENHA), SENHA, SENHA + "!nova");

        assertThat(eu(login(email, SENHA + "!nova")).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("gestor redefinir a senha derruba o token que o usuário tinha")
    void redefinirSenhaDerrubaTokenAntigo() throws InterruptedException {
        String email = cadastrar("redefinida@teste.local");
        String antigo = login(email, SENHA);

        Thread.sleep(PARA_O_SEGUNDO_VIRAR_MS);
        ResponseEntity<Void> redefinida = rest.exchange(ACESSOS + "/usuarios/" + idDoUsuario(email) + "/redefinir-senha", HttpMethod.POST,
                comToken(admin, Map.of("senhaProvisoria", "Provisoria@123")), Void.class);
        assertThat(redefinida.getStatusCode().is2xxSuccessful()).isTrue();

        assertThat(eu(antigo).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(eu(login(email, "Provisoria@123")).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("quem nunca trocou a senha continua com o token válido, mesmo depois de outra pessoa trocar a dela")
    void quemNaoTrocouNaoEAfetado() throws InterruptedException {
        String tokenDeOutro = login(cadastrar("nao.trocou@teste.local"), SENHA);
        String email = cadastrar("trocou@teste.local");

        Thread.sleep(PARA_O_SEGUNDO_VIRAR_MS);
        trocarSenha(login(email, SENHA), SENHA, SENHA + "!nova");

        assertThat(eu(tokenDeOutro).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("mudar o perfil do usuário não derruba o token: as permissões já são relidas a cada requisição")
    void mudarPerfilNaoDerrubaToken() throws InterruptedException {
        String email = cadastrar("perfil@teste.local");
        String token = login(email, SENHA);
        Object outroPerfil = rest.exchange(ACESSOS + "/perfis", HttpMethod.POST, comToken(admin, Map.of("nome", "Só ordens", "descricao", "",
                "permissoes", List.of("ORDEM_VISUALIZAR"))), Map.class).getBody().get("id");

        Thread.sleep(PARA_O_SEGUNDO_VIRAR_MS);
        assertThat(rest.exchange(ACESSOS + "/usuarios/" + idDoUsuario(email) + "/perfil", HttpMethod.PATCH,
                comToken(admin, Map.of("perfilId", outroPerfil)), Map.class).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(eu(token).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private String cadastrar(String email) {
        rest.postForEntity("/api/v1/auth/registrar", json(Map.of("nome", "Pessoa", "email", email, "senha", SENHA)), Void.class);
        return email;
    }

    private ResponseEntity<Map> tentarLogin(String email, String senha) {
        return rest.postForEntity("/api/v1/auth/login", json(Map.of("email", email, "senha", senha)), Map.class);
    }

    private String login(String email, String senha) {
        return (String) tentarLogin(email, senha).getBody().get("token");
    }

    private ResponseEntity<Map> eu(String token) {
        return rest.exchange(ACESSOS + "/eu", HttpMethod.GET, comToken(token, null), Map.class);
    }

    private ResponseEntity<Void> trocarSenha(String token, String atual, String nova) {
        return rest.exchange(ACESSOS + "/minha-senha", HttpMethod.PUT, comToken(token, Map.of("senhaAtual", atual, "novaSenha", nova)), Void.class);
    }

    private String idDoUsuario(String email) {
        List<Map<String, Object>> usuarios = rest.exchange(ACESSOS + "/usuarios", HttpMethod.GET, comToken(admin, null), List.class).getBody();
        return (String) usuarios.stream().filter(u -> email.equals(u.get("email"))).findFirst().orElseThrow().get("id");
    }

    private static HttpEntity<Object> json(Object corpo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(corpo, headers);
    }

    private static HttpEntity<Object> comToken(String token, Object corpo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return new HttpEntity<>(corpo, headers);
    }
}
