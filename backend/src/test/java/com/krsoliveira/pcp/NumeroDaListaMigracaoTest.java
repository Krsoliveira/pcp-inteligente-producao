package com.krsoliveira.pcp;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A V22 numera as listas que já existem. O teste migra até a V21, grava listas "antigas" e só então aplica a V22,
 * como acontece em produção. Sem Spring: o contexto da aplicação já subiria o banco inteiro migrado.
 */
class NumeroDaListaMigracaoTest {

    @Test
    @DisplayName("um número por lista, na ordem de criação da primeira versão; versões da mesma lista compartilham o número")
    void numeraListasExistentes() throws SQLException {
        var banco = BancoDeTeste.novoBancoParaMigracao(getClass());
        migrar(banco, "21");
        UUID materialRecente = gravarMaterial(banco, "103000001");
        UUID materialAntigo = gravarMaterial(banco, "103000002");
        UUID listaRecente = UUID.randomUUID();
        UUID listaAntiga = UUID.randomUUID();
        gravarVersao(banco, materialRecente, listaRecente, "v1", "2026-01-02T10:00:00Z");
        gravarVersao(banco, materialRecente, listaRecente, "v2", "2026-01-05T10:00:00Z");
        gravarVersao(banco, materialAntigo, listaAntiga, "v1", "2026-01-01T10:00:00Z");

        migrar(banco, "22");

        Map<String, Long> numeros = numerosPorVersao(banco);
        assertThat(numeros).containsEntry("103000002 v1", 1L)
                .containsEntry("103000001 v1", 2L)
                .containsEntry("103000001 v2", 2L);
        assertThat(proximoDaSequencia(banco)).as("a sequência segue do maior número usado").isEqualTo(3L);
    }

    @Test
    @DisplayName("sem nenhuma lista antes, a primeira a ser criada recebe o número 1")
    void bancoSemListas() throws SQLException {
        var banco = BancoDeTeste.novoBancoParaMigracao(getClass());

        migrar(banco, "22");

        assertThat(proximoDaSequencia(banco)).isEqualTo(1L);
    }

    @Test
    @DisplayName("a coluna numero_lista é obrigatória depois da migração")
    void colunaObrigatoria() throws SQLException {
        var banco = BancoDeTeste.novoBancoParaMigracao(getClass());

        migrar(banco, "22");

        try (Connection c = conectar(banco); PreparedStatement p = c.prepareStatement(
                "SELECT is_nullable FROM information_schema.columns WHERE table_name = 'lista_tecnica'"
                        + " AND column_name = 'numero_lista'"); ResultSet r = p.executeQuery()) {
            assertThat(r.next()).isTrue();
            assertThat(r.getString(1)).isEqualTo("NO");
        }
    }

    private static void migrar(BancoDeTeste.ConexaoDeTeste banco, String ate) {
        Flyway.configure().dataSource(banco.url(), banco.usuario(), banco.senha()).target(ate).load().migrate();
    }

    private static Connection conectar(BancoDeTeste.ConexaoDeTeste banco) throws SQLException {
        return DriverManager.getConnection(banco.url(), banco.usuario(), banco.senha());
    }

    private static UUID gravarMaterial(BancoDeTeste.ConexaoDeTeste banco, String codigo) throws SQLException {
        UUID id = UUID.randomUUID();
        try (Connection c = conectar(banco); PreparedStatement p = c.prepareStatement(
                "INSERT INTO material (id, codigo, descricao, tipo, unidade_de_medida, criado_em, atualizado_em,"
                        + " criado_por, atualizado_por) VALUES (?, ?, 'Teste', 'PRODUTO_ACABADO', 'un', now(), now(),"
                        + " 'teste', 'teste')")) {
            p.setObject(1, id);
            p.setString(2, codigo);
            p.executeUpdate();
        }
        return id;
    }

    private static void gravarVersao(BancoDeTeste.ConexaoDeTeste banco, UUID materialId, UUID listaId, String versao,
                                     String criadaEm) throws SQLException {
        try (Connection c = conectar(banco); PreparedStatement p = c.prepareStatement(
                "INSERT INTO lista_tecnica (id, material_id, lista_id, versao, status, criada_em, atualizada_em,"
                        + " criada_por, atualizada_por) VALUES (?, ?, ?, ?, 'ATIVA', CAST(? AS timestamptz),"
                        + " CAST(? AS timestamptz), 'teste', 'teste')")) {
            p.setObject(1, UUID.randomUUID());
            p.setObject(2, materialId);
            p.setObject(3, listaId);
            p.setString(4, versao);
            p.setString(5, criadaEm);
            p.setString(6, criadaEm);
            p.executeUpdate();
        }
    }

    private static Map<String, Long> numerosPorVersao(BancoDeTeste.ConexaoDeTeste banco) throws SQLException {
        Map<String, Long> numeros = new LinkedHashMap<>();
        try (Connection c = conectar(banco); PreparedStatement p = c.prepareStatement(
                "SELECT m.codigo || ' ' || l.versao, l.numero_lista FROM lista_tecnica l"
                        + " JOIN material m ON m.id = l.material_id"); ResultSet r = p.executeQuery()) {
            while (r.next()) {
                numeros.put(r.getString(1), r.getLong(2));
            }
        }
        return numeros;
    }

    private static long proximoDaSequencia(BancoDeTeste.ConexaoDeTeste banco) throws SQLException {
        try (Connection c = conectar(banco); PreparedStatement p = c.prepareStatement(
                "SELECT nextval('lista_tecnica_numero_seq')"); ResultSet r = p.executeQuery()) {
            r.next();
            return r.getLong(1);
        }
    }
}
