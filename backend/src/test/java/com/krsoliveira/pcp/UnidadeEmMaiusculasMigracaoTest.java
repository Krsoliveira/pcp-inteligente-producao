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
 * A V23 põe em maiúsculas as unidades que já existem. O teste migra até a V22, grava dados "antigos" com unidades
 * escritas de jeitos diferentes e só então aplica a V23, como acontece em produção. Sem Spring.
 */
class UnidadeEmMaiusculasMigracaoTest {

    @Test
    @DisplayName("unidades antigas (kg, Kg, espaços) ficam em maiúsculas, em material e em item da lista técnica")
    void normalizaUnidadesExistentes() throws SQLException {
        var banco = BancoDeTeste.novoBancoParaMigracao(getClass());
        migrar(banco, "22");
        UUID produto = gravarMaterial(banco, "103000001", "PRODUTO_ACABADO", "un");
        UUID aco = gravarMaterial(banco, "110000001", "MATERIA_PRIMA", " Kg ");
        UUID tinta = gravarMaterial(banco, "110000002", "MATERIA_PRIMA", "L");
        UUID lista = gravarLista(banco, produto);
        gravarItem(banco, lista, aco, "kg");
        gravarItem(banco, lista, tinta, "Litro ");

        migrar(banco, "23");

        assertThat(unidadesDosMateriais(banco)).containsEntry("103000001", "UN")
                .containsEntry("110000001", "KG").containsEntry("110000002", "L");
        assertThat(unidadesDosItens(banco)).containsExactlyInAnyOrder("KG", "LITRO");
    }

    @Test
    @DisplayName("quem já estava em maiúsculas não é tocado")
    void quemJaEstavaCertoFicaIgual() throws SQLException {
        var banco = BancoDeTeste.novoBancoParaMigracao(getClass());
        migrar(banco, "22");
        gravarMaterial(banco, "110000001", "MATERIA_PRIMA", "KG");

        migrar(banco, "23");

        assertThat(unidadesDosMateriais(banco)).containsEntry("110000001", "KG");
    }

    private static void migrar(BancoDeTeste.ConexaoDeTeste banco, String ate) {
        Flyway.configure().dataSource(banco.url(), banco.usuario(), banco.senha()).target(ate).load().migrate();
    }

    private static Connection conectar(BancoDeTeste.ConexaoDeTeste banco) throws SQLException {
        return DriverManager.getConnection(banco.url(), banco.usuario(), banco.senha());
    }

    private static UUID gravarMaterial(BancoDeTeste.ConexaoDeTeste banco, String codigo, String tipo, String unidade)
            throws SQLException {
        UUID id = UUID.randomUUID();
        try (Connection c = conectar(banco); PreparedStatement p = c.prepareStatement(
                "INSERT INTO material (id, codigo, descricao, tipo, unidade_de_medida, criado_em, atualizado_em,"
                        + " criado_por, atualizado_por) VALUES (?, ?, 'Teste', ?, ?, now(), now(), 'teste', 'teste')")) {
            p.setObject(1, id);
            p.setString(2, codigo);
            p.setString(3, tipo);
            p.setString(4, unidade);
            p.executeUpdate();
        }
        return id;
    }

    private static UUID gravarLista(BancoDeTeste.ConexaoDeTeste banco, UUID materialId) throws SQLException {
        UUID id = UUID.randomUUID();
        try (Connection c = conectar(banco); PreparedStatement p = c.prepareStatement(
                "INSERT INTO lista_tecnica (id, material_id, lista_id, numero_lista, versao, status, criada_em,"
                        + " atualizada_em,"
                        + " criada_por, atualizada_por) VALUES (?, ?, ?, 1, 'v1', 'ATIVA', now(), now(), 'teste', 'teste')")) {
            p.setObject(1, id);
            p.setObject(2, materialId);
            p.setObject(3, UUID.randomUUID());
            p.executeUpdate();
        }
        return id;
    }

    private static void gravarItem(BancoDeTeste.ConexaoDeTeste banco, UUID listaId, UUID componenteId, String unidade)
            throws SQLException {
        try (Connection c = conectar(banco); PreparedStatement p = c.prepareStatement(
                "INSERT INTO item_lista_tecnica (id, lista_tecnica_id, material_componente_id, quantidade_planejada,"
                        + " unidade_de_medida) VALUES (?, ?, ?, 1, ?)")) {
            p.setObject(1, UUID.randomUUID());
            p.setObject(2, listaId);
            p.setObject(3, componenteId);
            p.setString(4, unidade);
            p.executeUpdate();
        }
    }

    private static Map<String, String> unidadesDosMateriais(BancoDeTeste.ConexaoDeTeste banco) throws SQLException {
        Map<String, String> unidades = new LinkedHashMap<>();
        try (Connection c = conectar(banco); PreparedStatement p = c.prepareStatement(
                "SELECT codigo, unidade_de_medida FROM material"); ResultSet r = p.executeQuery()) {
            while (r.next()) {
                unidades.put(r.getString(1), r.getString(2));
            }
        }
        return unidades;
    }

    private static java.util.List<String> unidadesDosItens(BancoDeTeste.ConexaoDeTeste banco) throws SQLException {
        java.util.List<String> unidades = new java.util.ArrayList<>();
        try (Connection c = conectar(banco); PreparedStatement p = c.prepareStatement(
                "SELECT unidade_de_medida FROM item_lista_tecnica"); ResultSet r = p.executeQuery()) {
            while (r.next()) {
                unidades.add(r.getString(1));
            }
        }
        return unidades;
    }
}
