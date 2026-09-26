package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Espelho da tabela {@code consumo_material} para o JPA/Hibernate.
 * Conversão entre domínio e persistência via {@link #deDominio} e {@link #paraDominio}.
 */
@Entity
@Table(name = "consumo_material")
public class ConsumoMaterialJpaEntity {

    @Id
    private UUID id;

    @Column(name = "ordem_producao_id", nullable = false)
    private UUID ordemProducaoId;

    @Column(name = "material_id", nullable = false)
    private UUID materialId;

    @Column(name = "quantidade_planejada", nullable = false, precision = 12, scale = 4)
    private BigDecimal quantidadePlanejada;

    @Column(name = "quantidade_consumida", precision = 12, scale = 4)
    private BigDecimal quantidadeConsumida;

    @Column(name = "unidade_de_medida", nullable = false, length = 10)
    private String unidadeDeMedida;

    @Column(columnDefinition = "TEXT")
    private String justificativa;

    @Column(name = "justificado_por", length = 150)
    private String justificadoPor;

    @Column(name = "justificado_em")
    private Instant justificadoEm;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected ConsumoMaterialJpaEntity() {}

    public static ConsumoMaterialJpaEntity deDominio(ConsumoMaterial consumo) {
        ConsumoMaterialJpaEntity entity = new ConsumoMaterialJpaEntity();
        entity.id = consumo.getId();
        entity.ordemProducaoId = consumo.getOrdemProducaoId();
        entity.materialId = consumo.getMaterialId();
        entity.quantidadePlanejada = consumo.getQuantidadePlanejada();
        entity.quantidadeConsumida = consumo.getQuantidadeConsumida();
        entity.unidadeDeMedida = consumo.getUnidadeDeMedida();
        entity.justificativa = consumo.getJustificativa();
        entity.justificadoPor = consumo.getJustificadoPor();
        entity.justificadoEm = consumo.getJustificadoEm();
        entity.criadoEm = consumo.getCriadoEm();
        return entity;
    }

    public ConsumoMaterial paraDominio() {
        return ConsumoMaterial.reconstituir(id, ordemProducaoId, materialId,
                quantidadePlanejada, quantidadeConsumida, unidadeDeMedida,
                justificativa, justificadoPor, justificadoEm, criadoEm);
    }
}
