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
 * Controle de acesso por perfis (ADR-0020), de ponta a ponta com PostgreSQL real: o autocadastro recebe o
 * perfil de menor poder, cada endpoint exige a permissão, "só se concede o que se tem", mudanças de perfil
 * valem na hora, a segurança por objeto nas listas e o rastreio na trilha de auditoria.
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
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings({"unchecked", "rawtypes"})
class ControleDeAcessoApiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String ACESSOS = "/api/v1/acessos";
    private static final String SENHA = "Senha@123";
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private TestRestTemplate rest;

    private String admin;

    @BeforeAll
    void entrarComoAdministrador() {
        admin = login("admin@pcp.com");
    }

    // ------------------------------------------------------------------ eu, perfis de sistema e catálogo

    @Test
    @DisplayName("o administrador inicial tem o perfil Administrador, com todas as permissões do catálogo")
    void administradorTemTudo() {
        Map<String, Object> eu = chamar(admin, HttpMethod.GET, ACESSOS + "/eu", null).getBody();

        assertThat(((Map<String, Object>) eu.get("perfil"))).containsEntry("nome", "Administrador");
        assertThat((List<String>) eu.get("permissoes"))
                .containsExactlyInAnyOrderElementsOf(Arrays.stream(Permissao.values()).map(Enum::name).toList());
    }

    @Test
    @DisplayName("o autocadastro recebe o perfil Visualizador (só consulta), nunca um perfil com poder de escrita")
    void autocadastroRecebeVisualizador() {
        String email = autocadastrar();
        String token = login(email);

        Map<String, Object> eu = chamar(token, HttpMethod.GET, ACESSOS + "/eu", null).getBody();

        assertThat(((Map<String, Object>) eu.get("perfil"))).containsEntry("nome", "Visualizador");
        List<String> permissoes = (List<String>) eu.get("permissoes");
        assertThat(permissoes).contains("DASHBOARD_VISUALIZAR", "LISTA_VISUALIZAR_ATIVAS", "ORDEM_VISUALIZAR")
                .noneMatch(p -> p.contains("CADASTRAR") || p.contains("CRIAR") || p.contains("ATIVAR")
                        || p.contains("GERENCIAR") || p.contains("REGISTRAR") || p.contains("RETIRAR") || p.contains("CONCLUIR"));
    }

    @Test
    @DisplayName("um campo 'perfil' enviado no autocadastro é ignorado: ninguém se cadastra como Administrador")
    void autocadastroIgnoraPerfilEnviado() {
        String email = "esperto" + SEQ.incrementAndGet() + "@pcp.com";
        rest.postForEntity("/api/v1/auth/registrar", json(null, Map.of(
                "nome", "Esperto", "email", email, "senha", SENHA, "perfil", "Administrador", "perfilId",
                "00000000-0000-0000-0000-000000000001")), Void.class);

        Map<String, Object> eu = chamar(login(email), HttpMethod.GET, ACESSOS + "/eu", null).getBody();

        assertThat(((Map<String, Object>) eu.get("perfil"))).containsEntry("nome", "Visualizador");
    }

    @Test
    @DisplayName("o catálogo de permissões lista todas, com área e descrição")
    void catalogo() {
        List<Map<String, Object>> catalogo = lista(admin, ACESSOS + "/permissoes");

        assertThat(catalogo).hasSize(Permissao.values().length);
        assertThat(catalogo.getFirst()).containsKeys("permissao", "area", "descricao");
    }

    // ------------------------------------------------------------------ 401 / 403 em todos os módulos

    @Test
    @DisplayName("sem token: 401 em endpoints novos e antigos")
    void semTokenDa401() {
        for (String url : List.of(ACESSOS + "/eu", ACESSOS + "/perfis", "/api/v1/ordens-producao/pesquisa",
                "/api/v1/listas-tecnicas/comparar?a=x&b=y")) {
            assertThat(rest.getForEntity(url, Map.class).getStatusCode()).as(url).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    @DisplayName("Visualizador consulta o que o perfil permite, e recebe 403 (Problem Details) nas escritas e nas áreas restritas")
    void visualizadorLeEMasNaoEscreve() {
        String token = login(autocadastrar());

        assertThat(chamar(token, HttpMethod.GET, "/api/v1/ordens-producao/pesquisa", null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(chamar(token, HttpMethod.GET, "/api/v1/materiais/pesquisa", null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(chamar(token, HttpMethod.GET, "/api/v1/dashboard/resumo", null).getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Map<String, Object>> proibidos = Map.of(
                "POST /api/v1/materiais", Map.of("descricao", "X", "tipo", "MATERIA_PRIMA", "unidadeDeMedida", "kg"),
                "POST /api/v1/ordens-producao", Map.of("codigo", "OP-X", "materialId", UUID0, "listaTecnicaId", UUID0,
                        "centroDeTrabalho", "Usinagem", "quantidade", 1, "inicioPlanejado", "2999-01-01", "fimPlanejado", "2999-01-02"),
                "POST /api/v1/tipos-ordem", Map.of("nome", "X", "cor", "#112233"),
                "POST /api/v1/listas-tecnicas", Map.of("materialId", UUID0, "versao", "v1", "itens",
                        List.of(Map.of("materialComponenteId", UUID0, "quantidadePlanejada", 1.0, "unidadeDeMedida", "kg"))),
                "POST /api/v1/notas-fiscais", Map.of("fornecedor", "X", "numero", "1", "dataEmissao", "2026-01-01",
                        "dataRecebimento", "2026-01-02", "itens", List.of(Map.of("materialId", UUID0, "quantidade", 1,
                                "numeroLote", "L1", "dataFabricacao", "2026-01-01", "dataValidade", "2030-01-01"))),
                "POST /api/v1/acessos/perfis", Map.of("nome", "X", "descricao", "", "permissoes", List.of("ORDEM_VISUALIZAR")),
                "POST /api/v1/acessos/usuarios", Map.of("nome", "X", "email", "x@pcp.com", "senhaProvisoria", SENHA, "perfilId", UUID0));
        proibidos.forEach((rotulo, corpo) -> {
            String[] partes = rotulo.split(" ");
            ResponseEntity<Map> resposta = chamar(token, HttpMethod.valueOf(partes[0]), partes[1], corpo);
            assertThat(resposta.getStatusCode()).as(rotulo).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(resposta.getBody()).as(rotulo).containsEntry("title", "Acesso negado").containsEntry("status", 403);
        });

        for (String url : List.of("/api/v1/auditoria/eventos", ACESSOS + "/perfis", ACESSOS + "/usuarios", ACESSOS + "/permissoes",
                "/api/v1/tipos-ordem")) {
            assertThat(rest.exchange(url, HttpMethod.GET, json(token, null), String.class).getStatusCode())
                    .as(url).isEqualTo(HttpStatus.FORBIDDEN);
        }
        assertThat(chamar(token, HttpMethod.PATCH, "/api/v1/listas-tecnicas/" + UUID0 + "/ativar", null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(chamar(token, HttpMethod.PATCH, "/api/v1/ordens-producao/" + UUID0 + "/status", Map.of("status", "LIBERADA")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private static final String UUID0 = "00000000-0000-0000-0000-0000000000ff";

    // ------------------------------------------------------------------ delegação: o administrador cria o gerente, o gerente cria o resto

    @Test
    @DisplayName("o Administrador cria o perfil Gerente e um usuário com ele; o gerente cria o restante, dentro do seu alcance")
    void cadeiaDeDelegacao() {
        String perfilGerente = criarPerfil(admin, "Gerente", Permissao.ACESSO_VISUALIZAR, Permissao.PERFIL_GERENCIAR,
                Permissao.USUARIO_GERENCIAR, Permissao.ORDEM_VISUALIZAR, Permissao.ORDEM_CRIAR);
        String emailGerente = criarUsuario(admin, "gerente", perfilGerente);
        String gerente = login(emailGerente);

        // dentro do alcance
        String perfilOperador = criarPerfil(gerente, "Operador", Permissao.ORDEM_VISUALIZAR);
        String emailOperador = criarUsuario(gerente, "operador", perfilOperador);
        assertThat(login(emailOperador)).isNotBlank();

        // fora do alcance (o gerente não tem LISTA_ATIVAR nem NOTA_FISCAL_REGISTRAR)
        ResponseEntity<Map> acima = chamar(gerente, HttpMethod.POST, ACESSOS + "/perfis",
                Map.of("nome", "Acima", "descricao", "", "permissoes", List.of("ORDEM_VISUALIZAR", "LISTA_ATIVAR")));
        assertThat(acima.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(acima.getBody().get("detail").toString()).contains("não pode conceder");

        // não atribui o perfil Administrador a ninguém, nem a si
        String idAdministrador = idDoPerfil(admin, "Administrador");
        String idOperador = idDoUsuario(gerente, emailOperador);
        assertThat(chamar(gerente, HttpMethod.PATCH, ACESSOS + "/usuarios/" + idOperador + "/perfil",
                Map.of("perfilId", idAdministrador)).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        String idGerente = idDoUsuario(gerente, emailGerente);
        ResponseEntity<Map> proprio = chamar(gerente, HttpMethod.PATCH, ACESSOS + "/usuarios/" + idGerente + "/perfil",
                Map.of("perfilId", perfilOperador));
        assertThat(proprio.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(proprio.getBody().get("detail").toString()).contains("próprio perfil");

        // o perfil Administrador não pode ser alterado nem excluído
        assertThat(chamar(gerente, HttpMethod.PUT, ACESSOS + "/perfis/" + idAdministrador,
                Map.of("nome", "X", "descricao", "", "permissoes", List.of("ORDEM_VISUALIZAR"))).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(chamar(admin, HttpMethod.DELETE, ACESSOS + "/perfis/" + idAdministrador, null).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("mudar as permissões de um perfil vale na hora, com o mesmo token; trocar o perfil do usuário também")
    void mudancaValeNaHora() {
        String perfil = criarPerfil(admin, "Consulta de ordens", Permissao.ORDEM_VISUALIZAR);
        String email = criarUsuario(admin, "consulta", perfil);
        String token = login(email);
        String url = "/api/v1/ordens-producao/pesquisa";
        assertThat(chamar(token, HttpMethod.GET, url, null).getStatusCode()).isEqualTo(HttpStatus.OK);

        // tira a permissão: o mesmo token passa a receber 403
        chamar(admin, HttpMethod.PUT, ACESSOS + "/perfis/" + perfil,
                Map.of("nome", "Consulta de ordens", "descricao", "", "permissoes", List.of("DASHBOARD_VISUALIZAR")));
        assertThat(chamar(token, HttpMethod.GET, url, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(chamar(token, HttpMethod.GET, "/api/v1/dashboard/resumo", null).getStatusCode()).isEqualTo(HttpStatus.OK);

        // troca o perfil do usuário para o Visualizador (que vê ordens): volta a ter acesso, sem novo login
        chamar(admin, HttpMethod.PATCH, ACESSOS + "/usuarios/" + idDoUsuario(admin, email) + "/perfil",
                Map.of("perfilId", idDoPerfil(admin, "Visualizador")));
        assertThat(chamar(token, HttpMethod.GET, url, null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ------------------------------------------------------------------ regras de perfis

    @Test
    @DisplayName("nome de perfil repetido (sem diferenciar maiúsculas) devolve 409; dados inválidos devolvem 400")
    void validacoesDePerfil() {
        criarPerfil(admin, "Qualidade única", Permissao.LISTA_COMPARAR);

        assertThat(chamar(admin, HttpMethod.POST, ACESSOS + "/perfis",
                Map.of("nome", "QUALIDADE ÚNICA", "descricao", "", "permissoes", List.of("LISTA_COMPARAR"))).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(chamar(admin, HttpMethod.POST, ACESSOS + "/perfis",
                Map.of("nome", " ", "descricao", "", "permissoes", List.of("LISTA_COMPARAR"))).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(chamar(admin, HttpMethod.POST, ACESSOS + "/perfis",
                Map.of("nome", "Sem permissões", "descricao", "", "permissoes", List.of())).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(chamar(admin, HttpMethod.POST, ACESSOS + "/perfis",
                Map.of("nome", "Permissão inventada", "descricao", "", "permissoes", List.of("PODE_TUDO"))).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("excluir: recusa perfil em uso e perfil de sistema (422); aceita perfil sem usuários (204); depois some (404)")
    void excluirPerfil() {
        String emUso = criarPerfil(admin, "Em uso", Permissao.ORDEM_VISUALIZAR);
        criarUsuario(admin, "emuso", emUso);
        String livre = criarPerfil(admin, "Livre", Permissao.ORDEM_VISUALIZAR);

        assertThat(chamar(admin, HttpMethod.DELETE, ACESSOS + "/perfis/" + emUso, null).getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(chamar(admin, HttpMethod.DELETE, ACESSOS + "/perfis/" + idDoPerfil(admin, "Visualizador"), null).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(chamar(admin, HttpMethod.DELETE, ACESSOS + "/perfis/" + livre, null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(chamar(admin, HttpMethod.GET, ACESSOS + "/perfis/" + livre, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("a lista de usuários nunca expõe senha nem hash")
    void listaDeUsuariosNaoVazaSenha() {
        criarUsuario(admin, "anonimo", idDoPerfil(admin, "Visualizador"));

        String corpo = rest.exchange(ACESSOS + "/usuarios", HttpMethod.GET, json(admin, null), String.class).getBody();

        assertThat(corpo).contains("perfilNome").doesNotContainIgnoringCase("senha").doesNotContainIgnoringCase("hash");
    }

    // ------------------------------------------------------------------ segurança por objeto nas listas

    @Test
    @DisplayName("listas: com só 'ver ativas' a versão em revisão não existe (404) e a comparação a esconde; com 'ver todas' aparece")
    void listasPorObjeto() {
        String componente = criarMaterial("Componente", "MATERIA_PRIMA", "kg");
        String produto = criarMaterial("Produto", "PRODUTO_ACABADO", "un");
        String ativa = cadastrarVersao(produto, "v1", componente);
        String emRevisao = cadastrarVersao(produto, "v2", componente);
        chamar(admin, HttpMethod.PATCH, "/api/v1/listas-tecnicas/" + ativa + "/ativar", null);

        String soAtivas = login(criarUsuario(admin, "soativas", criarPerfil(admin, "Só ativas",
                Permissao.LISTA_VISUALIZAR_ATIVAS, Permissao.LISTA_COMPARAR)));
        String todas = login(criarUsuario(admin, "todas", criarPerfil(admin, "Vê todas",
                Permissao.LISTA_VISUALIZAR_TODAS, Permissao.LISTA_COMPARAR)));

        List<Map<String, Object>> vistas = lista(soAtivas, "/api/v1/listas-tecnicas?materialId=" + produto);
        assertThat(vistas).extracting(l -> l.get("versao")).containsExactly("v1");
        assertThat(chamar(soAtivas, HttpMethod.GET, "/api/v1/listas-tecnicas/" + emRevisao, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(chamar(soAtivas, HttpMethod.GET, "/api/v1/listas-tecnicas/comparar?a=" + ativa + "&b=" + emRevisao, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        Map<String, Object> pesquisa = chamar(soAtivas, HttpMethod.GET,
                "/api/v1/listas-tecnicas/pesquisa?status=EM_REVISAO", null).getBody();
        assertThat(pesquisa).containsEntry("total", 0);

        assertThat(chamar(todas, HttpMethod.GET, "/api/v1/listas-tecnicas/" + emRevisao, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(chamar(todas, HttpMethod.GET, "/api/v1/listas-tecnicas/comparar?a=" + ativa + "&b=" + emRevisao, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("comparar exige LISTA_COMPARAR mesmo para quem vê as listas (403)")
    void compararExigePermissaoPropria() {
        String soVe = login(criarUsuario(admin, "sove", criarPerfil(admin, "Só vê", Permissao.LISTA_VISUALIZAR_TODAS)));

        assertThat(chamar(soVe, HttpMethod.GET, "/api/v1/listas-tecnicas/comparar?a=" + UUID0 + "&b=" + UUID0, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ------------------------------------------------------------------ rastreio

    @Test
    @DisplayName("toda mudança de acesso entra na trilha: perfil criado, alterado e excluído; perfil atribuído a usuário")
    void trilhaDeAuditoria() {
        String perfil = criarPerfil(admin, "Auditado", Permissao.ORDEM_VISUALIZAR, Permissao.ORDEM_CRIAR);
        chamar(admin, HttpMethod.PUT, ACESSOS + "/perfis/" + perfil,
                Map.of("nome", "Auditado", "descricao", "", "permissoes", List.of("ORDEM_VISUALIZAR", "ESTOQUE_VISUALIZAR")));
        String email = criarUsuario(admin, "auditado", idDoPerfil(admin, "Visualizador"));
        chamar(admin, HttpMethod.PATCH, ACESSOS + "/usuarios/" + idDoUsuario(admin, email) + "/perfil", Map.of("perfilId", perfil));
        chamar(admin, HttpMethod.PATCH, ACESSOS + "/usuarios/" + idDoUsuario(admin, email) + "/perfil",
                Map.of("perfilId", idDoPerfil(admin, "Visualizador")));
        chamar(admin, HttpMethod.DELETE, ACESSOS + "/perfis/" + perfil, null);

        List<Map<String, Object>> doPerfil = eventos("PERFIL_ACESSO", perfil);
        assertThat(doPerfil).extracting(e -> e.get("acao")).containsExactlyInAnyOrder("CRIADO", "ALTERADO", "EXCLUIDO");
        Map<String, Object> alterado = doPerfil.stream().filter(e -> "ALTERADO".equals(e.get("acao"))).findFirst().orElseThrow();
        Map<String, Object> detalhes = (Map<String, Object>) alterado.get("detalhes");
        assertThat(detalhes).containsEntry("permissoesAdicionadas", "ESTOQUE_VISUALIZAR").containsEntry("permissoesRemovidas", "ORDEM_CRIAR");
        assertThat(alterado.get("usuario")).isEqualTo("admin@pcp.com");

        List<Map<String, Object>> doUsuario = eventos("USUARIO", idDoUsuario(admin, email));
        assertThat(doUsuario).extracting(e -> e.get("acao")).contains("USUARIO_REGISTRADO", "PERFIL_ATRIBUIDO");
    }

    // ------------------------------------------------------------------ apoio

    private List<Map<String, Object>> eventos(String tipo, String entidadeId) {
        Map<String, Object> pagina = chamar(admin, HttpMethod.GET,
                "/api/v1/auditoria/eventos?tipoEntidade=" + tipo + "&entidadeId=" + entidadeId + "&tamanho=50", null).getBody();
        return (List<Map<String, Object>>) pagina.get("itens");
    }

    private String autocadastrar() {
        String email = "visual" + SEQ.incrementAndGet() + "@pcp.com";
        assertThat(rest.postForEntity("/api/v1/auth/registrar", json(null, Map.of("nome", "Visual", "email", email, "senha", SENHA)),
                Void.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return email;
    }

    /** Senha de quem já fez o primeiro acesso (trocou a provisória); quem não está aqui usa {@link #SENHA}. */
    private final Map<String, String> senhaAposPrimeiroAcesso = new java.util.concurrent.ConcurrentHashMap<>();

    private String login(String email) {
        ResponseEntity<Map> resposta = rest.postForEntity("/api/v1/auth/login",
                json(null, Map.of("email", email, "senha", senhaAposPrimeiroAcesso.getOrDefault(email, SENHA))), Map.class);
        assertThat(resposta.getStatusCode()).as("login de " + email).isEqualTo(HttpStatus.OK);
        return (String) resposta.getBody().get("token");
    }

    /** Cria um perfil e devolve o id. */
    private String criarPerfil(String token, String nome, Permissao... permissoes) {
        ResponseEntity<Map> resposta = chamar(token, HttpMethod.POST, ACESSOS + "/perfis", Map.of("nome", nome, "descricao", "",
                "permissoes", Arrays.stream(permissoes).map(Enum::name).toList()));
        assertThat(resposta.getStatusCode()).as("criar perfil " + nome + ": " + resposta.getBody()).isEqualTo(HttpStatus.CREATED);
        return (String) resposta.getBody().get("id");
    }

    /** Cria um usuário com o perfil e devolve o e-mail. */
    private String criarUsuario(String token, String apelido, String perfilId) {
        String email = apelido + SEQ.incrementAndGet() + "@pcp.com";
        ResponseEntity<Map> resposta = chamar(token, HttpMethod.POST, ACESSOS + "/usuarios",
                Map.of("nome", apelido, "email", email, "senhaProvisoria", SENHA, "perfilId", perfilId));
        assertThat(resposta.getStatusCode()).as("criar usuário " + email + ": " + resposta.getBody()).isEqualTo(HttpStatus.CREATED);
        primeiroAcesso(email);
        return email;
    }

    /** Usuário criado por gestor entra com a senha provisória e precisa trocá-la antes de usar o sistema (ADR-0021). */
    private void primeiroAcesso(String email) {
        String token = login(email);
        String definitiva = SENHA + "!definitiva";
        ResponseEntity<Void> troca = rest.exchange(ACESSOS + "/minha-senha", HttpMethod.PUT,
                json(token, Map.of("senhaAtual", SENHA, "novaSenha", definitiva)), Void.class);
        assertThat(troca.getStatusCode()).as("primeiro acesso de " + email).isEqualTo(HttpStatus.NO_CONTENT);
        senhaAposPrimeiroAcesso.put(email, definitiva);
    }

    private String idDoPerfil(String token, String nome) {
        List<Map<String, Object>> perfis = lista(token, ACESSOS + "/perfis");
        return (String) perfis.stream().filter(p -> nome.equals(p.get("nome"))).findFirst().orElseThrow().get("id");
    }

    private String idDoUsuario(String token, String email) {
        List<Map<String, Object>> usuarios = lista(token, ACESSOS + "/usuarios");
        return (String) usuarios.stream().filter(u -> email.equals(u.get("email"))).findFirst().orElseThrow().get("id");
    }

    private String criarMaterial(String descricao, String tipo, String unidade) {
        return idDe(chamar(admin, HttpMethod.POST, "/api/v1/materiais", Map.of("descricao", descricao, "tipo", tipo, "unidadeDeMedida", unidade)));
    }

    private String cadastrarVersao(String materialId, String versao, String componenteId) {
        return idDe(chamar(admin, HttpMethod.POST, "/api/v1/listas-tecnicas", Map.of("materialId", materialId, "versao", versao,
                "itens", List.of(Map.of("materialComponenteId", componenteId, "quantidadePlanejada", 1.0, "unidadeDeMedida", "kg")))));
    }

    private static String idDe(ResponseEntity<?> resposta) {
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String location = resposta.getHeaders().getLocation().toString();
        return location.substring(location.lastIndexOf('/') + 1);
    }

    /** GET de um endpoint que devolve uma lista JSON. */
    private List<Map<String, Object>> lista(String token, String url) {
        ResponseEntity<List> resposta = rest.exchange(url, HttpMethod.GET, json(token, null), List.class);
        assertThat(resposta.getStatusCode()).as(url).isEqualTo(HttpStatus.OK);
        return resposta.getBody();
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
