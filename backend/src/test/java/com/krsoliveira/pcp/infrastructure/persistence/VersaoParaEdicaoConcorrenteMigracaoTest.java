package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.BancoDeTesteIsolado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** A V20 acrescenta a versão aos registros editáveis por tela, obrigatória e começando em 0 para os que já existem. */
@SpringBootTest(properties = {
        "app.admin.nome=Administrador de Teste",
        "app.admin.email=admin@pcp.com",
        "app.admin.senha=Senha@123"
})
@BancoDeTesteIsolado
class VersaoParaEdicaoConcorrenteMigracaoTest {

    @Autowired
    private JdbcTemplate jdbc;

    @ParameterizedTest
    @ValueSource(strings = {"perfil_acesso", "tipo_ordem"})
    @DisplayName("a coluna versao existe, é obrigatória e vale 0 por padrão")
    void colunaObrigatoriaComPadraoZero(String tabela) {
        Map<String, Object> coluna = jdbc.queryForMap(
                "SELECT data_type, is_nullable, column_default FROM information_schema.columns"
                        + " WHERE table_name = ? AND column_name = 'versao'", tabela);

        assertThat(coluna).containsEntry("data_type", "bigint").containsEntry("is_nullable", "NO")
                .containsEntry("column_default", "0");
    }

    @Test
    @DisplayName("os perfis criados antes da V20 (os de sistema, da V13) começam na versão 0")
    void registrosAnterioresNaVersaoZero() {
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM perfil_acesso WHERE id IN ('00000000-0000-0000-0000-000000000001',"
                        + " '00000000-0000-0000-0000-000000000002') AND versao = 0", Integer.class))
                .isEqualTo(2);
    }
}
