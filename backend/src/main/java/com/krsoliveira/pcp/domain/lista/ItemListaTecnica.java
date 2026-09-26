package com.krsoliveira.pcp.domain.lista;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Componente de uma {@link ListaTecnica}: indica qual material é necessário,
 * em que quantidade e unidade, para produzir uma unidade do material-pai.
 *
 * Entidade de valor dentro do agregado ListaTecnica.
 */
public class ItemListaTecnica {

    private final UUID id;
    private final UUID materialComponenteId;
    private final BigDecimal quantidadePlanejada;
    private final String unidadeDeMedida;

    private ItemListaTecnica(UUID id, UUID materialComponenteId,
                             BigDecimal quantidadePlanejada, String unidadeDeMedida) {
        this.id = id;
        this.materialComponenteId = materialComponenteId;
        this.quantidadePlanejada = quantidadePlanejada;
        this.unidadeDeMedida = unidadeDeMedida;
    }

    /**
     * Fábrica para um item NOVO dentro de uma lista em construção.
     */
    public static ItemListaTecnica criar(UUID materialComponenteId, BigDecimal quantidadePlanejada,
                                        String unidadeDeMedida) {
        if (materialComponenteId == null) {
            throw new RegraDeNegocioException("O material do componente é obrigatório.");
        }
        if (quantidadePlanejada == null || quantidadePlanejada.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("A quantidade planejada do componente deve ser maior que zero.");
        }
        if (unidadeDeMedida == null || unidadeDeMedida.isBlank()) {
            throw new RegraDeNegocioException("A unidade de medida do componente é obrigatória.");
        }
        return new ItemListaTecnica(UUID.randomUUID(), materialComponenteId,
                quantidadePlanejada, unidadeDeMedida.trim());
    }

    /**
     * Reconstrói um item EXISTENTE a partir do banco de dados.
     */
    public static ItemListaTecnica reconstituir(UUID id, UUID materialComponenteId,
                                               BigDecimal quantidadePlanejada,
                                               String unidadeDeMedida) {
        return new ItemListaTecnica(id, materialComponenteId, quantidadePlanejada, unidadeDeMedida);
    }

    public UUID getId() { return id; }
    public UUID getMaterialComponenteId() { return materialComponenteId; }
    public BigDecimal getQuantidadePlanejada() { return quantidadePlanejada; }
    public String getUnidadeDeMedida() { return unidadeDeMedida; }
}