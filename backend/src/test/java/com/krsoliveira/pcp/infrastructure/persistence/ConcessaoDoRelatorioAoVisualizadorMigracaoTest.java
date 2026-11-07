package com.krsoliveira.pcp.infrastructure.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** A V19 concede o relatório de consumo ao Visualizador, assina o perfil e deixa um único evento na trilha. */
@SpringBootTest(properties = {
        "app.admin.nome=Administrador de Teste",
        "app.admin.email=admin@pcp.com",
        "app.admin.senha=Senha@123"
})
@Testcontainers
class ConcessaoDoRelatorioAoVisualizadorMigracaoTest {

    private static final String VISUALIZADOR = "00000000-0000-0000-0000-000000000002";
    private static final String PERMISSAO = "RELATORIO_CONSUMO_VISUALIZAR";
    private static final String MIGRACAO = "sistema:migracao-v19";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("o Visualizador tem a permissão; o perfil fica assinado pela migração e a trilha registra a concessão")
    void concedeAssinaERegistra() {
        assertThat(temAPermissao()).isTrue();
        assertThat(jdbc.queryForObject("SELECT atualizado_por FROM perfil_acesso WHERE id = ?::uuid", String.class, VISUALIZADOR))
                .isEqualTo(MIGRACAO);

        Map<String, Object> evento = jdbc.queryForMap(
                "SELECT tipo_entidade, referencia, acao, detalhes->>'permissoesAdicionadas' AS adicionadas, detalhes->>'perfil' AS perfil"
                        + " FROM evento_auditoria WHERE usuario = ? AND entidade_id = ?::uuid", MIGRACAO, VISUALIZADOR);
        assertThat(evento).containsEntry("tipo_entidade", "PERFIL_ACESSO").containsEntry("acao", "ALTERADO")
                .containsEntry("referencia", "Visualizador").containsEntry("perfil", "Visualizador")
                .containsEntry("adicionadas", PERMISSAO);
    }

    @Test
    @DisplayName("não mexe em outros perfis: só o Visualizador recebe a permissão pela migração")
    void soOVisualizador() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM perfil_acesso_permissao WHERE permissao = ?", Integer.class, PERMISSAO))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rodar de novo, com a permissão já concedida, não duplica a concessão nem o evento")
    void semEfeitoQuandoJaConcedida() throws IOException {
        jdbc.execute(new ClassPathResource("db/migration/V19__conceder_relatorio_ao_visualizador.sql")
                .getContentAsString(StandardCharsets.UTF_8));

        assertThat(temAPermissao()).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM evento_auditoria WHERE usuario = ?", Integer.class, MIGRACAO))
                .isEqualTo(1);
    }

    private boolean temAPermissao() {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM perfil_acesso_permissao WHERE perfil_id = ?::uuid AND permissao = ?)",
                Boolean.class, VISUALIZADOR, PERMISSAO));
    }
}
