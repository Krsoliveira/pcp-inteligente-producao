package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.domain.acesso.Permissao;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Troca de senha no primeiro acesso e redefinição pelo gestor (ADR-0021), de ponta a ponta com PostgreSQL
 * real: a senha provisória só abre "quem sou eu" e a troca, a troca libera o mesmo token, a redefinição
 * trava de novo na hora, um gerente não toma a conta de um Administrador e a trilha nunca guarda a senha.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.admin.nome=Administrador de Teste",
                "app.admin.email=admin@pcp.com",
                "app.admin.senha=SenhaDoAdmin1",
                "pcp.seguranca.rate-limit.login=1000",
                "pcp.seguranca.rate-limit.registrar=1000",
                "pcp.seguranca.rate-limit.trocar-senha=1000"
        })
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings({"unchecked", "rawtypes"})
class TrocaDeSenhaApiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String ACESSOS = "/api/v1/acessos";
    private static final String PROVISORIA = "Provisoria#2026";
    private static final String SENHA_ADMIN = "SenhaDoAdmin1";
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private TestRestTemplate rest;

    private String admin;

    @BeforeAll
    void entrarComoAdministrador() {
        admin = login("admin@pcp.com", SENHA_ADMIN);
    }

    @Test
    @DisplayName("o administrador inicial e o autocadastro não precisam trocar a senha")
    void quemEscolheAPropriaSenhaNaoTroca() {
        Map<String, Object> adminEu = chamar(admin, HttpMethod.GET, ACESSOS + "/eu", null).getBody();
        assertThat(adminEu).containsEntry("deveTrocarSenha", false);
        assertThat((List<?>) adminEu.get("permissoes")).isNotEmpty();

        String email = "auto" + SEQ.incrementAndGet() + "@pcp.com";
        rest.postForEntity("/api/v1/auth/registrar", json(null, Map.of("nome", "Auto", "email", email, "senha", "MinhaSenha123")), Void.class);
        Map<String, Object> autoEu = chamar(login(email, "MinhaSenha123"), HttpMethod.GET, ACESSOS + "/eu", null).getBody();
        assertThat(autoEu).containsEntry("deveTrocarSenha", false);
        assertThat(chamar(login(email, "MinhaSenha123"), HttpMethod.GET, "/api/v1/ordens-producao/pesquisa", null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("usuário criado por um gestor entra com a provisória, mas o servidor só aceita 'quem sou eu' e a troca")
    void provisoriaBloqueiaTudoMenosATroca() {
        String email = criarUsuario(admin, "novo", idDoPerfil("Visualizador"));
        String token = login(email, PROVISORIA);

        Map<String, Object> eu = chamar(token, HttpMethod.GET, ACESSOS + "/eu", null).getBody();
        assertThat(eu).containsEntry("deveTrocarSenha", true);
        assertThat((List<?>) eu.get("permissoes")).isEmpty();

        for (String url : List.of("/api/v1/ordens-producao/pesquisa", "/api/v1/dashboard/resumo", "/api/v1/materiais/pesquisa",
                "/api/v1/listas-tecnicas", ACESSOS + "/perfis", "/api/v1/estoque/resumo")) {
            ResponseEntity<Map> resposta = chamar(token, HttpMethod.GET, url, null);
            assertThat(resposta.getStatusCode()).as(url).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(resposta.getBody()).as(url).containsEntry("codigo", "TROCAR_SENHA")
                    .containsEntry("detail", "Defina uma nova senha para continuar.");
        }
        assertThat(chamar(token, HttpMethod.POST, "/api/v1/materiais",
                Map.of("descricao", "X", "tipo", "MATERIA_PRIMA", "unidadeDeMedida", "kg")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("trocar a senha libera o mesmo token na hora; a provisória deixa de valer e a nova passa a valer")
    void trocaLibera() {
        String email = criarUsuario(admin, "troca", idDoPerfil("Visualizador"));
        String token = login(email, PROVISORIA);

        ResponseEntity<Void> troca = rest.exchange(ACESSOS + "/minha-senha", HttpMethod.PUT,
                json(token, Map.of("senhaAtual", PROVISORIA, "novaSenha", "MinhaNovaSenha1")), Void.class);

        assertThat(troca.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        Map<String, Object> eu = chamar(token, HttpMethod.GET, ACESSOS + "/eu", null).getBody();
        assertThat(eu).containsEntry("deveTrocarSenha", false);
        assertThat((List<String>) eu.get("permissoes")).contains("ORDEM_VISUALIZAR");
        assertThat(chamar(token, HttpMethod.GET, "/api/v1/ordens-producao/pesquisa", null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rest.postForEntity("/api/v1/auth/login", json(null, Map.of("email", email, "senha", PROVISORIA)), Map.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(login(email, "MinhaNovaSenha1")).isNotBlank();
    }

    @Test
    @DisplayName("senha atual errada (422), igual à atual (422), curta ou longa demais (400) e ausente (400) são recusadas; a marca continua")
    void validacoes() {
        String email = criarUsuario(admin, "valida", idDoPerfil("Visualizador"));
        String token = login(email, PROVISORIA);

        assertThat(trocar(token, "errada12345", "MinhaNovaSenha1").getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        ResponseEntity<Map> igual = trocar(token, PROVISORIA, PROVISORIA);
        assertThat(igual.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(igual.getBody().get("detail").toString()).contains("diferente da atual");
        assertThat(trocar(token, PROVISORIA, "curta").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(trocar(token, PROVISORIA, "a".repeat(73)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rest.exchange(ACESSOS + "/minha-senha", HttpMethod.PUT, json(token, Map.of("novaSenha", "MinhaNovaSenha1")), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(chamar(token, HttpMethod.GET, ACESSOS + "/eu", null).getBody()).containsEntry("deveTrocarSenha", true);
    }

    @Test
    @DisplayName("senha atual errada devolve 422 e nunca 401 (o frontend desloga em 401)")
    void senhaAtualErradaNaoDa401() {
        String token = login(criarUsuario(admin, "nao401", idDoPerfil("Visualizador")), PROVISORIA);

        assertThat(trocar(token, "errada12345", "MinhaNovaSenha1").getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("quem já trocou pode trocar de novo quando quiser, pedindo a senha atual")
    void trocaVoluntaria() {
        String token = login("admin@pcp.com", SENHA_ADMIN);
        // o administrador não tem a marca; troca e volta à senha original para não afetar os outros testes
        assertThat(trocar(token, SENHA_ADMIN, "OutraSenhaDoAdmin2").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(login("admin@pcp.com", "OutraSenhaDoAdmin2")).isNotBlank();
        String novoToken = login("admin@pcp.com", "OutraSenhaDoAdmin2");
        assertThat(trocar(novoToken, "OutraSenhaDoAdmin2", SENHA_ADMIN).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("sem token a troca de senha devolve 401")
    void semToken() {
        assertThat(rest.exchange(ACESSOS + "/minha-senha", HttpMethod.PUT,
                json(null, Map.of("senhaAtual", "x1234567", "novaSenha", "MinhaNovaSenha1")), Map.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ------------------------------------------------------------------ redefinição pelo gestor

    @Test
    @DisplayName("redefinir trava o usuário na hora (mesmo token), a senha antiga deixa de valer e a provisória nova abre só a troca")
    void redefinicao() {
        String email = criarUsuario(admin, "redef", idDoPerfil("Visualizador"));
        String token = login(email, PROVISORIA);
        trocar(token, PROVISORIA, "MinhaSenhaEscolhida1");
        assertThat(chamar(token, HttpMethod.GET, "/api/v1/ordens-producao/pesquisa", null).getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Void> reset = rest.exchange(ACESSOS + "/usuarios/" + idDoUsuario(email) + "/redefinir-senha", HttpMethod.POST,
                json(admin, Map.of("senhaProvisoria", "NovaProvisoria#1")), Void.class);

        assertThat(reset.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        ResponseEntity<Map> bloqueado = chamar(token, HttpMethod.GET, "/api/v1/ordens-producao/pesquisa", null);
        assertThat(bloqueado.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(bloqueado.getBody()).containsEntry("codigo", "TROCAR_SENHA");
        assertThat(rest.postForEntity("/api/v1/auth/login", json(null, Map.of("email", email, "senha", "MinhaSenhaEscolhida1")), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        String novoToken = login(email, "NovaProvisoria#1");
        assertThat(chamar(novoToken, HttpMethod.GET, ACESSOS + "/eu", null).getBody()).containsEntry("deveTrocarSenha", true);
    }

    @Test
    @DisplayName("um gerente não redefine a senha de um Administrador (assumiria a conta), nem a própria, mas redefine a de quem está ao alcance")
    void gerenteNaoTomaContas() {
        String perfilGerente = criarPerfil("Gerente de senhas", Permissao.ACESSO_VISUALIZAR, Permissao.USUARIO_GERENCIAR, Permissao.ORDEM_VISUALIZAR);
        String emailGerente = criarUsuario(admin, "gerentes", perfilGerente);
        String gerente = login(emailGerente, PROVISORIA);
        trocar(gerente, PROVISORIA, "SenhaDoGerente1");
        gerente = login(emailGerente, "SenhaDoGerente1");
        String emailAoAlcance = criarUsuario(admin, "baixo", criarPerfil("Só ordens do chão", Permissao.ORDEM_VISUALIZAR));
        String emailAcima = criarUsuario(admin, "acima", idDoPerfil("Visualizador"));

        // o Administrador, o próprio gerente e um perfil com permissões que o gerente não tem: tudo recusado
        assertThat(redefinir(gerente, idDoUsuario("admin@pcp.com"), "Sequestro#2026").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(redefinir(gerente, idDoUsuario(emailGerente), "Sequestro#2026").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(redefinir(gerente, idDoUsuario(emailAcima), "Sequestro#2026").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        // quem está ao alcance do gerente é redefinido
        assertThat(redefinir(gerente, idDoUsuario(emailAoAlcance), "NovaProvisoria#2").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        // as contas protegidas continuam com as senhas delas
        assertThat(login("admin@pcp.com", SENHA_ADMIN)).isNotBlank();
        assertThat(login(emailAcima, PROVISORIA)).isNotBlank();
    }

    @Test
    @DisplayName("redefinir exige USUARIO_GERENCIAR (403), usuário inexistente devolve 404 e senha curta, 400")
    void redefinirValidacoes() {
        String semPermissao = login(criarUsuario(admin, "sempermissao", criarPerfil("Só ordens", Permissao.ORDEM_VISUALIZAR)), PROVISORIA);
        trocar(semPermissao, PROVISORIA, "SenhaSemPermissao1");
        String token = login(emailDoUltimo(), "SenhaSemPermissao1");

        assertThat(redefinir(token, idDoUsuario("admin@pcp.com"), "NovaProvisoria#3").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(redefinir(admin, "00000000-0000-0000-0000-0000000000aa", "NovaProvisoria#3").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        String alvo = criarUsuario(admin, "alvo", idDoPerfil("Visualizador"));
        assertThat(redefinir(admin, idDoUsuario(alvo), "curta").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rest.exchange(ACESSOS + "/usuarios/" + idDoUsuario(alvo) + "/redefinir-senha", HttpMethod.POST,
                json(null, Map.of("senhaProvisoria", "NovaProvisoria#3")), Map.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ------------------------------------------------------------------ rastreio

    @Test
    @DisplayName("a trilha registra troca e redefinição, e nenhuma senha nem hash aparece nela")
    void trilhaSemSegredos() {
        String email = criarUsuario(admin, "trilha", idDoPerfil("Visualizador"));
        String token = login(email, PROVISORIA);
        trocar(token, PROVISORIA, "SenhaSecretaDoUsuario1");
        redefinir(admin, idDoUsuario(email), "ProvisoriaSecreta#9");

        String corpo = rest.exchange("/api/v1/auditoria/eventos?tipoEntidade=USUARIO&entidadeId=" + idDoUsuario(email) + "&tamanho=50",
                HttpMethod.GET, json(admin, null), String.class).getBody();

        assertThat(corpo).contains("SENHA_ALTERADA").contains("SENHA_REDEFINIDA").contains("\"eraProvisoria\":true");
        assertThat(corpo).doesNotContain("SenhaSecretaDoUsuario1").doesNotContain(PROVISORIA).doesNotContain("ProvisoriaSecreta#9")
                .doesNotContain("$2a$").doesNotContain("$2b$");
    }

    // ------------------------------------------------------------------ apoio

    private String ultimoEmail;

    private String emailDoUltimo() { return ultimoEmail; }

    private String login(String email, String senha) {
        ResponseEntity<Map> resposta = rest.postForEntity("/api/v1/auth/login", json(null, Map.of("email", email, "senha", senha)), Map.class);
        assertThat(resposta.getStatusCode()).as("login de " + email).isEqualTo(HttpStatus.OK);
        return (String) resposta.getBody().get("token");
    }

    private ResponseEntity<Map> trocar(String token, String atual, String nova) {
        return rest.exchange(ACESSOS + "/minha-senha", HttpMethod.PUT, json(token, Map.of("senhaAtual", atual, "novaSenha", nova)), Map.class);
    }

    private ResponseEntity<Map> redefinir(String token, String usuarioId, String senha) {
        return rest.exchange(ACESSOS + "/usuarios/" + usuarioId + "/redefinir-senha", HttpMethod.POST,
                json(token, Map.of("senhaProvisoria", senha)), Map.class);
    }

    private String criarPerfil(String nome, Permissao... permissoes) {
        ResponseEntity<Map> resposta = chamar(admin, HttpMethod.POST, ACESSOS + "/perfis", Map.of("nome", nome + SEQ.incrementAndGet(),
                "descricao", "", "permissoes", Arrays.stream(permissoes).map(Enum::name).toList()));
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) resposta.getBody().get("id");
    }

    private String criarUsuario(String token, String apelido, String perfilId) {
        String email = apelido + SEQ.incrementAndGet() + "@pcp.com";
        ResponseEntity<Map> resposta = chamar(token, HttpMethod.POST, ACESSOS + "/usuarios",
                Map.of("nome", apelido, "email", email, "senhaProvisoria", PROVISORIA, "perfilId", perfilId));
        assertThat(resposta.getStatusCode()).as("criar usuário: " + resposta.getBody()).isEqualTo(HttpStatus.CREATED);
        ultimoEmail = email;
        return email;
    }

    private String idDoPerfil(String nome) {
        List<Map<String, Object>> perfis = lista(ACESSOS + "/perfis");
        return (String) perfis.stream().filter(p -> nome.equals(p.get("nome"))).findFirst().orElseThrow().get("id");
    }

    private String idDoUsuario(String email) {
        List<Map<String, Object>> usuarios = lista(ACESSOS + "/usuarios");
        return (String) usuarios.stream().filter(u -> email.equals(u.get("email"))).findFirst().orElseThrow().get("id");
    }

    private List<Map<String, Object>> lista(String url) {
        return rest.exchange(url, HttpMethod.GET, json(admin, null), List.class).getBody();
    }

    private ResponseEntity<Map> chamar(String token, HttpMethod metodo, String url, Object corpo) {
        return rest.exchange(url, metodo, json(token, corpo), Map.class);
    }

    private static HttpEntity<Object> json(String token, Object corpo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.setBearerAuth(token);
        return new HttpEntity<>(corpo, headers);
    }
}
