package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.BancoDeTesteIsolado;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
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
 * Duas ações ao mesmo tempo sobre a mesma versão da lista técnica. Sem a trava, as duas liam o status antigo, passavam
 * na regra e gravavam: a ativação (ou a retirada) aparecia duas vezes na trilha de auditoria. Repetido porque a falha
 * depende do encontro entre as duas requisições.
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
class ConcorrenciaDaListaTecnicaApiTest {

    private static final int REPETICOES = 5;
    private static final String LISTAS = "/api/v1/listas-tecnicas";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    private String token;
    private String aco;
    private String produto;
    private int sequencia = 0;

    @BeforeAll
    void prepararMassa() {
        token = (String) rest.postForEntity("/api/v1/auth/login",
                new HttpEntity<>(Map.of("email", "admin@pcp.com", "senha", "Senha@123"), json()), Map.class)
                .getBody().get("token");
        aco = material("Aço", "MATERIA_PRIMA", "kg");
        produto = material("Viga", "PRODUTO_ACABADO", "un");
    }

    @RepeatedTest(REPETICOES)
    @DisplayName("ativar a mesma versão duas vezes ao mesmo tempo: uma ativa, a outra é recusada, e a trilha registra uma vez")
    void ativacaoSimultanea() throws Exception {
        String lista = novaLista();
        Supplier<Integer> ativar = () -> acao(lista, "ativar");

        assertThat(emParalelo(ativar, ativar)).containsExactlyInAnyOrder(204, 422);

        assertThat(eventos(lista, "ATIVADA")).isEqualTo(1);
    }

    @RepeatedTest(REPETICOES)
    @DisplayName("retirar de uso duas vezes ao mesmo tempo: uma retira, a outra é recusada, e a trilha registra uma vez")
    void retiradaSimultanea() throws Exception {
        String lista = novaLista();
        assertThat(acao(lista, "ativar")).isEqualTo(204);
        Supplier<Integer> retirar = () -> acao(lista, "retirar-de-uso");

        assertThat(emParalelo(retirar, retirar)).containsExactlyInAnyOrder(204, 422);

        assertThat(eventos(lista, "OBSOLETADA")).isEqualTo(1);
    }

    @RepeatedTest(REPETICOES)
    @DisplayName("criar uma ordem enquanto a versão é retirada: a ordem só existe se foi criada antes da retirada")
    void ordemContraRetirada() throws Exception {
        String lista = novaLista();
        assertThat(acao(lista, "ativar")).isEqualTo(204);

        List<Integer> respostas = emParalelo(() -> criarOrdem(lista), () -> acao(lista, "retirar-de-uso"));

        // A retirada sempre vale; a ordem vale se chegou antes (201) ou é recusada por usar versão retirada (422).
        assertThat(respostas.get(1)).isEqualTo(204);
        assertThat(respostas.get(0)).isIn(201, 422);
        if (respostas.get(0) == 201) {
            assertThat(jdbc.queryForObject(
                    "SELECT o.criada_em <= e.ocorrido_em FROM ordem_producao o, evento_auditoria e"
                            + " WHERE o.lista_tecnica_id = ?::uuid AND e.entidade_id = ?::uuid AND e.acao = 'OBSOLETADA'",
                    Boolean.class, lista, lista)).isTrue();
        }
    }

    private String novaLista() {
        return idDe(rest.postForEntity(LISTAS, comAuth(Map.of("materialId", produto, "versao", "v" + (++sequencia),
                "itens", List.of(Map.of("materialComponenteId", aco, "quantidadePlanejada", 2, "unidadeDeMedida", "kg")))),
                Void.class));
    }

    private int acao(String lista, String nome) {
        return rest.exchange(LISTAS + "/" + lista + "/" + nome, HttpMethod.PATCH, comAuth(null), String.class)
                .getStatusCode().value();
    }

    private int criarOrdem(String lista) {
        Map<String, Object> corpo = Map.of("codigo", "OP-LT-" + (++sequencia), "materialId", produto, "listaTecnicaId", lista,
                "centroDeTrabalho", "Usinagem", "quantidade", 10, "inicioPlanejado", LocalDate.now().toString(),
                "fimPlanejado", LocalDate.now().plusDays(10).toString());
        return rest.postForEntity("/api/v1/ordens-producao", comAuth(corpo), String.class).getStatusCode().value();
    }

    private int eventos(String lista, String acao) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM evento_auditoria WHERE entidade_id = ?::uuid AND acao = ?",
                Integer.class, lista, acao);
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

    private String material(String descricao, String tipo, String unidade) {
        return idDe(rest.postForEntity("/api/v1/materiais",
                comAuth(Map.of("descricao", descricao, "tipo", tipo, "unidadeDeMedida", unidade)), Void.class));
    }

    private static String idDe(ResponseEntity<?> resposta) {
        String caminho = resposta.getHeaders().getLocation().getPath();
        return caminho.substring(caminho.lastIndexOf('/') + 1);
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
