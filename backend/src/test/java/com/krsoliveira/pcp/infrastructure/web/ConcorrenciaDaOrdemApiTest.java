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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
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
 * Duas requisições ao mesmo tempo sobre a mesma ordem. Só o PostgreSQL real mostra a corrida: as duas liam o estado
 * antigo, passavam na checagem e gravavam, gerando dois lotes na conclusão e baixa de saldo em dobro no consumo.
 * Repetido porque a falha depende do encontro entre as duas.
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
@BancoDeTesteIsolado
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings({"unchecked", "rawtypes"})
@AutoConfigureTestRestTemplate
class ConcorrenciaDaOrdemApiTest {

    private static final int REPETICOES = 5;
    private static final String ORDENS = "/api/v1/ordens-producao";
    private static final String CONSUMO = "25";

    @Autowired
    private TestRestTemplate rest;

    private String token;
    private String aco;
    private String produto;
    private String lista;
    private String loteA;
    private String loteB;
    private int sequencia = 0;

    @BeforeAll
    void prepararMassa() {
        token = (String) rest.postForEntity("/api/v1/auth/login",
                new HttpEntity<>(Map.of("email", "admin@pcp.com", "senha", "Senha@123"), json()), Map.class)
                .getBody().get("token");
        aco = material("Aço", "MATERIA_PRIMA", "kg");
        produto = material("Viga", "PRODUTO_ACABADO", "un");
        lista = idDe(rest.postForEntity("/api/v1/listas-tecnicas", comAuth(Map.of("materialId", produto, "versao", "v1",
                "itens", List.of(Map.of("materialComponenteId", aco, "quantidadePlanejada", 2.5, "unidadeDeMedida", "kg")))), Void.class));
        rest.exchange("/api/v1/listas-tecnicas/" + lista + "/ativar", HttpMethod.PATCH, comAuth(null), Void.class);
        rest.postForEntity("/api/v1/notas-fiscais", comAuth(Map.of(
                "fornecedor", "Fornecedor", "numero", "NF-CONCORRENCIA", "dataEmissao", "2026-08-01", "dataRecebimento", "2026-08-02",
                "itens", List.of(
                        Map.of("materialId", aco, "quantidade", 5000, "numeroLote", "ACO-A", "dataFabricacao", "2026-07-01", "dataValidade", "2031-01-01"),
                        Map.of("materialId", aco, "quantidade", 5000, "numeroLote", "ACO-B", "dataFabricacao", "2026-07-01", "dataValidade", "2031-01-01")))),
                Void.class);
        List<Map<String, Object>> lotes = lotesDisponiveis();
        loteA = idDoLote(lotes, "ACO-A");
        loteB = idDoLote(lotes, "ACO-B");
    }

    @RepeatedTest(REPETICOES)
    @DisplayName("concluir duas vezes ao mesmo tempo gera um lote só; a segunda é recusada como regra de negócio")
    void conclusaoSimultanea() throws Exception {
        String ordem = ordemComConsumoRegistrado();
        Supplier<Integer> concluir = () -> concluir(ordem);

        assertThat(emParalelo(concluir, concluir)).containsExactlyInAnyOrder(200, 422);

        assertThat(rest.exchange("/api/v1/lotes?ordemProducaoId=" + ordem, HttpMethod.GET, comAuth(null), List.class).getBody()).hasSize(1);
    }

    @RepeatedTest(REPETICOES)
    @DisplayName("registrar o mesmo consumo ao mesmo tempo, com lotes diferentes, baixa o saldo uma vez só")
    void registroSimultaneoComLotesDiferentes() throws Exception {
        String ordem = ordemEmProducao();
        String consumo = consumoDa(ordem);
        BigDecimal antes = saldoTotal();

        assertThat(emParalelo(() -> registrar(ordem, consumo, loteA), () -> registrar(ordem, consumo, loteB)))
                .containsExactlyInAnyOrder(200, 422);

        assertThat(antes.subtract(saldoTotal())).isEqualByComparingTo(CONSUMO);
    }

    @RepeatedTest(REPETICOES)
    @DisplayName("duplo clique: o segundo registro igual é recusado como regra de negócio, não como erro do sistema")
    void duploClique() throws Exception {
        String ordem = ordemEmProducao();
        String consumo = consumoDa(ordem);
        BigDecimal antes = saldoTotal();
        Supplier<Integer> registrar = () -> registrar(ordem, consumo, loteA);

        assertThat(emParalelo(registrar, registrar)).containsExactlyInAnyOrder(200, 422);

        assertThat(antes.subtract(saldoTotal())).isEqualByComparingTo(CONSUMO);
    }

    @RepeatedTest(REPETICOES)
    @DisplayName("cancelar enquanto registra: se o cancelamento vencer, o saldo não é baixado")
    void cancelamentoContraRegistro() throws Exception {
        String ordem = ordemEmProducao();
        String consumo = consumoDa(ordem);
        BigDecimal antes = saldoTotal();

        List<Integer> respostas = emParalelo(() -> registrar(ordem, consumo, loteA), () -> cancelar(ordem));

        // As duas podem valer, na ordem registrar → cancelar; o cancelamento é permitido em produção.
        assertThat(respostas.get(1)).isEqualTo(200);
        BigDecimal baixado = antes.subtract(saldoTotal());
        assertThat(baixado).isEqualByComparingTo(respostas.get(0) == 200 ? CONSUMO : "0");
    }

    @RepeatedTest(REPETICOES)
    @DisplayName("cancelar enquanto conclui: só uma vence, e o lote só existe se a conclusão venceu")
    void cancelamentoContraConclusao() throws Exception {
        String ordem = ordemComConsumoRegistrado();

        List<Integer> respostas = emParalelo(() -> concluir(ordem), () -> cancelar(ordem));

        assertThat(respostas).containsExactlyInAnyOrder(200, 422);
        int lotes = rest.exchange("/api/v1/lotes?ordemProducaoId=" + ordem, HttpMethod.GET, comAuth(null), List.class).getBody().size();
        assertThat(lotes).isEqualTo(respostas.get(0) == 200 ? 1 : 0);
    }

    @RepeatedTest(REPETICOES)
    @DisplayName("criar duas ordens com o mesmo código ao mesmo tempo: a segunda recebe conflito, não erro do sistema")
    void cadastroSimultaneoComMesmoCodigo() throws Exception {
        String codigo = "OP-DUP-" + (++sequencia);
        Map<String, Object> corpo = Map.of("codigo", codigo, "materialId", produto, "listaTecnicaId", lista, "centroDeTrabalho", "Usinagem",
                "quantidade", 10, "inicioPlanejado", LocalDate.now().toString(), "fimPlanejado", LocalDate.now().plusDays(10).toString());
        Supplier<Integer> criar = () -> rest.postForEntity(ORDENS, comAuth(corpo), String.class).getStatusCode().value();

        assertThat(emParalelo(criar, criar)).containsExactlyInAnyOrder(201, 409);
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

    private int concluir(String ordem) {
        Map<String, Object> corpo = Map.of("quantidadeProduzida", 10, "dataFabricacao", LocalDate.now().toString(),
                "dataValidade", LocalDate.now().plusYears(1).toString());
        return rest.exchange(ORDENS + "/" + ordem + "/concluir", HttpMethod.POST, comAuth(corpo), String.class).getStatusCode().value();
    }

    private int registrar(String ordem, String consumo, String lote) {
        Map<String, Object> corpo = Map.of("quantidadeConsumida", new BigDecimal(CONSUMO),
                "alocacoes", List.of(Map.of("loteId", lote, "quantidade", new BigDecimal(CONSUMO))));
        return rest.exchange(ORDENS + "/" + ordem + "/consumos/" + consumo, HttpMethod.PATCH, comAuth(corpo), String.class).getStatusCode().value();
    }

    private int cancelar(String ordem) {
        return rest.exchange(ORDENS + "/" + ordem + "/status", HttpMethod.PATCH, comAuth(Map.of("status", "CANCELADA")), String.class)
                .getStatusCode().value();
    }

    private String ordemComConsumoRegistrado() {
        String ordem = ordemEmProducao();
        assertThat(registrar(ordem, consumoDa(ordem), loteA)).isEqualTo(200);
        return ordem;
    }

    private String ordemEmProducao() {
        ResponseEntity<Map> criada = rest.postForEntity(ORDENS, comAuth(Map.of(
                "codigo", "OP-CC-" + (++sequencia), "materialId", produto, "listaTecnicaId", lista, "centroDeTrabalho", "Usinagem",
                "quantidade", 10, "inicioPlanejado", LocalDate.now().toString(), "fimPlanejado", LocalDate.now().plusDays(10).toString())), Map.class);
        assertThat(criada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String ordem = (String) criada.getBody().get("id");
        for (String status : List.of("LIBERADA", "EM_PRODUCAO")) {
            assertThat(rest.exchange(ORDENS + "/" + ordem + "/status", HttpMethod.PATCH, comAuth(Map.of("status", status)), Map.class)
                    .getStatusCode()).isEqualTo(HttpStatus.OK);
        }
        return ordem;
    }

    private String consumoDa(String ordem) {
        List<Map<String, Object>> consumos = rest.exchange(ORDENS + "/" + ordem + "/consumos", HttpMethod.GET, comAuth(null), List.class).getBody();
        return (String) consumos.get(0).get("id");
    }

    private BigDecimal saldoTotal() {
        return lotesDisponiveis().stream().map(l -> new BigDecimal(String.valueOf(l.get("saldo")))).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Map<String, Object>> lotesDisponiveis() {
        return rest.exchange("/api/v1/lotes?materialId=" + aco + "&disponiveis=true", HttpMethod.GET, comAuth(null), List.class).getBody();
    }

    private static String idDoLote(List<Map<String, Object>> lotes, String numero) {
        return (String) lotes.stream().filter(l -> numero.equals(l.get("numeroLote"))).findFirst().orElseThrow().get("id");
    }

    private String material(String descricao, String tipo, String unidade) {
        return idDe(rest.postForEntity("/api/v1/materiais", comAuth(Map.of("descricao", descricao, "tipo", tipo, "unidadeDeMedida", unidade)), Void.class));
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
