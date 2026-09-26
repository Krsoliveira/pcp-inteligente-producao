package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.lista.ItemListaTecnica;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Espelho da tabela {@code item_lista_tecnica} para o JPA/Hibernate.
 */
@Entity
@Table(name = "item_lista_tecnica")
public class ItemListaTecnicaJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lista_tecnica_id", nullable = false)
    private ListaTecnicaJpaEntity listaTecnica;

    @Column(name = "material_componente_id", nullable = false)
    private UUID materialComponenteId;

    @Column(name = "quantidade_planejada", nullable = false, precision = 14, scale = 4)
    private BigDecimal quantidadePlanejada;

    @Column(name = "unidade_de_medida", nullable = false, length = 10)
    private String unidadeDeMedida;

    protected ItemListaTecnicaJpaEntity() {}

    public static ItemListaTecnicaJpaEntity deDominio(ItemListaTecnica item,
                                                      ListaTecnicaJpaEntity listaTecnica) {
        ItemListaTecnicaJpaEntity entity = new ItemListaTecnicaJpaEntity();
        entity.id = item.getId();
        entity.listaTecnica = listaTecnica;
        entity.materialComponenteId = item.getMaterialComponenteId();
        entity.quantidadePlanejada = item.getQuantidadePlanejada();
        entity.unidadeDeMedida = item.getUnidadeDeMedida();
        return entity;
    }

    public ItemListaTecnica paraDominio() {
        return ItemListaTecnica.reconstituir(id, materialComponenteId,
                quantidadePlanejada, unidadeDeMedida);
    }
}
