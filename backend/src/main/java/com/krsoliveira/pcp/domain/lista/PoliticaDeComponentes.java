package com.krsoliveira.pcp.domain.lista;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
import com.krsoliveira.pcp.domain.ordem.TipoOrdem;

import java.util.Collection;

/**
 * Regra inegociável (ADR-0024): produto acabado não é consumido numa ordem comum. Para usá-lo, abre-se uma ordem do
 * tipo Conversão, que converte o acabado em semiacabado.
 *
 * <ul>
 *   <li>Lista de produto acabado: só matéria-prima e semiacabado como componentes.</li>
 *   <li>Lista de semiacabado: pode ter produto acabado, mas só pode ser usada em ordem do tipo Conversão.</li>
 *   <li>Ordem do tipo Conversão: só produz semiacabado.</li>
 * </ul>
 *
 * Classe de domínio PURA: sem anotações de framework.
 */
public final class PoliticaDeComponentes {

    private PoliticaDeComponentes() {}

    /** Valida os componentes de uma lista do material {@code produzido}. */
    public static void validarLista(Material produzido, Collection<Material> componentes) {
        if (produzido.getTipo() == TipoMaterial.SEMIACABADO) return;
        componentes.stream().filter(PoliticaDeComponentes::ehProdutoAcabado).findFirst().ifPresent(acabado -> {
            throw new RegraDeNegocioException(
                    "O produto acabado %s não pode ser componente da lista de %s: só listas de semiacabado "
                            .formatted(acabado.getCodigo(), produzido.getCodigo())
                            + "(conversão) consomem produto acabado.");
        });
    }

    /** Valida a criação de uma ordem para {@code produzido}, com esses componentes e esse tipo (pode ser nulo). */
    public static void validarOrdem(Material produzido, Collection<Material> componentes, TipoOrdem tipo) {
        boolean conversao = tipo != null && tipo.isConversao();
        if (conversao && produzido.getTipo() != TipoMaterial.SEMIACABADO) {
            throw new RegraDeNegocioException(
                    "O tipo de ordem Conversão só vale para ordens que produzem semiacabado.");
        }
        if (!conversao && consomeProdutoAcabado(componentes)) {
            throw new RegraDeNegocioException(
                    "Esta lista consome produto acabado: a ordem precisa ser do tipo Conversão.");
        }
    }

    public static boolean consomeProdutoAcabado(Collection<Material> componentes) {
        return componentes.stream().anyMatch(PoliticaDeComponentes::ehProdutoAcabado);
    }

    private static boolean ehProdutoAcabado(Material material) {
        return material.getTipo() == TipoMaterial.PRODUTO_ACABADO;
    }
}
