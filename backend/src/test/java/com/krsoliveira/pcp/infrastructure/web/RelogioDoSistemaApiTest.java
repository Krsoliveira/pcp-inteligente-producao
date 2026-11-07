package com.krsoliveira.pcp.infrastructure.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * "Hoje" é o dia no fuso da fábrica, não o do servidor. O instante fixo é 01:00 UTC do dia 15, ainda 22:00 do dia 14 em
 * Brasília: uma ordem com fim no dia 14 não está atrasada; com o dia do servidor em UTC, estaria. A data fica no futuro
 * distante para o teste reprovar também quem ignorar o relógio.
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
@AutoConfigureTestRestTemplate
@Import(RelogioDoSistemaApiTest.RelogioFixo.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class RelogioDoSistemaApiTest {

    @TestConfiguration
    static class RelogioFixo {
        @Bean
        @Primary
        Clock relogioFixo() {
            return Clock.fixed(Instant.parse("2040-01-15T01:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        }
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DisplayName("a ordem só fica atrasada quando o dia no fuso configurado passa do fim planejado, na resposta e no resumo")
    void atrasadaPeloDiaNoFuso() {
        String token = (String) rest.postForEntity("/api/v1/auth/login",
                new HttpEntity<>(Map.of("email", "admin@pcp.com", "senha", "Senha@123"), json()), Map.class).getBody().get("token");
        String aco = material(token, "Aço", "MATERIA_PRIMA", "kg");
        String produto = material(token, "Viga", "PRODUTO_ACABADO", "un");
        String lista = idDe(rest.postForEntity("/api/v1/listas-tecnicas", comAuth(token, Map.of("materialId", produto, "versao", "v1",
                "itens", List.of(Map.of("materialComponenteId", aco, "quantidadePlanejada", 2.5, "unidadeDeMedida", "kg")))), Void.class));
        rest.exchange("/api/v1/listas-tecnicas/" + lista + "/ativar", org.springframework.http.HttpMethod.PATCH, comAuth(token, null), Void.class);

        Map<String, Object> noPrazo = criar(token, "OP-HOJE", produto, lista, "2040-01-14");
        Map<String, Object> vencida = criar(token, "OP-ONTEM", produto, lista, "2040-01-13");

        assertThat(noPrazo).containsEntry("atrasada", false);
        assertThat(vencida).containsEntry("atrasada", true);
        Map<String, Object> resumo = rest.exchange("/api/v1/dashboard/resumo", org.springframework.http.HttpMethod.GET, comAuth(token, null), Map.class).getBody();
        assertThat(((Number) resumo.get("ordensAtrasadas")).intValue()).isEqualTo(1);
    }

    private Map<String, Object> criar(String token, String codigo, String produto, String lista, String fim) {
        return rest.postForEntity("/api/v1/ordens-producao", comAuth(token, Map.of("codigo", codigo, "materialId", produto, "listaTecnicaId", lista,
                "centroDeTrabalho", "Usinagem", "quantidade", 10, "inicioPlanejado", "2040-01-01", "fimPlanejado", fim)), Map.class).getBody();
    }

    private String material(String token, String descricao, String tipo, String unidade) {
        return idDe(rest.postForEntity("/api/v1/materiais", comAuth(token, Map.of("descricao", descricao, "tipo", tipo, "unidadeDeMedida", unidade)), Void.class));
    }

    private static String idDe(ResponseEntity<?> resposta) {
        String caminho = resposta.getHeaders().getLocation().getPath();
        return caminho.substring(caminho.lastIndexOf('/') + 1);
    }

    private static HttpEntity<Object> comAuth(String token, Object corpo) {
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
