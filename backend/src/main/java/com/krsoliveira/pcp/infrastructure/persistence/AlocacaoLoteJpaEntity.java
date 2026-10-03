package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.lote.AlocacaoLote;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Mapeamento JPA da alocação de lote. {@code @Immutable}: alocações nunca mudam. */
@Entity
@Immutable
@Table(name = "alocacao_lote")
public class AlocacaoLoteJpaEntity {

    @Id
    private UUID id;

    @Column(name = "consumo_material_id", nullable = false)
    private UUID consumoMaterialId;

    @Column(name = "lote_id", nullable = false)
    private UUID loteId;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal quantidade;

    @Column(name = "criado_por", nullable = false, length = 150)
    private String criadoPor;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected AlocacaoLoteJpaEntity() {}

    public static AlocacaoLoteJpaEntity deDominio(AlocacaoLote alocacao) {
        AlocacaoLoteJpaEntity entity = new AlocacaoLoteJpaEntity();
        entity.id = alocacao.getId();
        entity.consumoMaterialId = alocacao.getConsumoMaterialId();
        entity.loteId = alocacao.getLoteId();
        entity.quantidade = alocacao.getQuantidade();
        entity.criadoPor = alocacao.getCriadoPor();
        entity.criadoEm = alocacao.getCriadoEm();
        return entity;
    }

    public AlocacaoLote paraDominio() {
        return AlocacaoLote.reconstituir(id, consumoMaterialId, loteId, quantidade, criadoPor, criadoEm);
    }
}
