package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.BancoDeTesteIsolado;
import com.krsoliveira.pcp.domain.usuario.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dois administradores rebaixam um ao outro ao mesmo tempo. Cada um contava dois administradores, os dois passavam na
 * regra do "último", e o sistema ficava sem ninguém para gerenciar acessos.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.admin.nome=Administrador de Teste",
                "app.admin.email=admin@pcp.com",
                "app.admin.senha=Senha@123",
                "pcp.seguranca.rate-limit.login=1000",
                "pcp.seguranca.rate-limit.trocar-senha=1000"
        })
@BancoDeTesteIsolado
@AutoConfigureTestRestTemplate
@SuppressWarnings("unchecked")
class UltimoAdministradorConcorrenteApiTest {

    private static final int RODADAS = 10;
    private static final String ACESSOS = "/api/v1/acessos";
    private static final String SENHA = "Senha@123";
    private static final String SENHA_NOVA = "Senha@123!nova";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private UsuarioRepository usuarios;

    @Test
    @DisplayName("rebaixarem um ao outro ao mesmo tempo: uma troca vale, a outra é recusada e sempre sobra um administrador")
    void rebaixamentoCruzado() throws Exception {
        String tokenA = login("admin@pcp.com", SENHA);
        List<Map<String, Object>> perfis = rest.exchange(ACESSOS + "/perfis", HttpMethod.GET, comToken(tokenA, null), List.class).getBody();
        String administrador = idDoPerfil(perfis, "Administrador");
        String visualizador = idDoPerfil(perfis, "Visualizador");
        String idA = idDoUsuario(tokenA, "admin@pcp.com");

        rest.postForEntity(ACESSOS + "/usuarios", comToken(tokenA, Map.of("nome", "Segundo", "email", "segundo@pcp.com",
                "senhaProvisoria", SENHA, "perfilId", administrador)), Map.class);
        rest.exchange(ACESSOS + "/minha-senha", HttpMethod.PUT, comToken(login("segundo@pcp.com", SENHA),
                Map.of("senhaAtual", SENHA, "novaSenha", SENHA_NOVA)), Void.class);
        String idB = idDoUsuario(tokenA, "segundo@pcp.com");
        String tokenB = login("segundo@pcp.com", SENHA_NOVA);

        for (int rodada = 0; rodada < RODADAS; rodada++) {
            List<Integer> respostas = emParalelo(() -> atribuir(tokenA, idB, visualizador), () -> atribuir(tokenB, idA, visualizador));

            assertThat(usuarios.contarPorPerfil(UUID.fromString(administrador))).as("administradores que sobraram").isEqualTo(1);
            assertThat(respostas).contains(200);
            assertThat(respostas.stream().filter(s -> s != 200).toList()).hasSize(1).first().isIn(403, 422);

            // Quem sobrou devolve o perfil ao outro, para a próxima rodada.
            boolean sobrouA = respostas.get(0) == 200;
            assertThat(atribuir(sobrouA ? tokenA : tokenB, sobrouA ? idB : idA, administrador)).isEqualTo(200);
        }
    }

    private int atribuir(String token, String usuarioId, String perfilId) {
        return rest.exchange(ACESSOS + "/usuarios/" + usuarioId + "/perfil", HttpMethod.PATCH,
                comToken(token, Map.of("perfilId", perfilId)), String.class).getStatusCode().value();
    }

    private List<Integer> emParalelo(Callable<Integer> primeira, Callable<Integer> segunda) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch largada = new CountDownLatch(1);
            List<Future<Integer>> futuros = new ArrayList<>();
            for (Callable<Integer> chamada : List.of(primeira, segunda)) {
                futuros.add(pool.submit(() -> {
                    largada.await();
                    return chamada.call();
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

    private String login(String email, String senha) {
        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.setContentType(MediaType.APPLICATION_JSON);
        return (String) rest.postForEntity("/api/v1/auth/login", new HttpEntity<>(Map.of("email", email, "senha", senha), cabecalhos), Map.class)
                .getBody().get("token");
    }

    private static String idDoPerfil(List<Map<String, Object>> perfis, String nome) {
        return (String) perfis.stream().filter(p -> nome.equals(p.get("nome"))).findFirst().orElseThrow().get("id");
    }

    private String idDoUsuario(String token, String email) {
        List<Map<String, Object>> lista = rest.exchange(ACESSOS + "/usuarios", HttpMethod.GET, comToken(token, null), List.class).getBody();
        return (String) lista.stream().filter(u -> email.equals(u.get("email"))).findFirst().orElseThrow().get("id");
    }

    private static HttpEntity<Object> comToken(String token, Object corpo) {
        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.setContentType(MediaType.APPLICATION_JSON);
        cabecalhos.setBearerAuth(token);
        return new HttpEntity<>(corpo, cabecalhos);
    }
}
