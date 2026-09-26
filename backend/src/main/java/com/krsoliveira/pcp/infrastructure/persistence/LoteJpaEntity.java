package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.StatusLote;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Espelho da tabela {@code lote} para o JPA/Hibernate.
 * Conversão entre domínio e persistência via {@link #deDominio} e {@link #paraDominio}.
 */
@Entity
@Table(name = "lote")
public class LoteJpaEntity {

    @Id
    private UUID id;

    @Column(name = "numero_lote", nullable = false, unique = true, length = 40)
    private String numeroLote;

    @Column(name = "material_id", nullable = false)
    private UUID materialId;

    @Column(name = "ordem_producao_id")
    private UUID ordemProducaoId;

    @Column(length = 150)
    private String fornecedor;

    @Column(name = "nota_fiscal", length = 44)
    private String notaFiscal;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal quantidade;

    @Column(name = "unidade_de_medida", nullable = false, length = 10)
    private String unidadeDeMedida;

    @Column(name = "data_fabricacao", nullable = false)
    private LocalDate dataFabricacao;

    @Column(name = "data_validade", nullable = false)
    private LocalDate dataValidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatusLote status;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected LoteJpaEntity() {}

    public static LoteJpaEntity deDominio(Lote lote) {
        LoteJpaEntity entity = new LoteJpaEntity();
        entity.id = lote.getId();
        entity.numeroLote = lote.getNumeroLote();
        entity.materialId = lote.getMaterialId();
        entity.ordemProducaoId = lote.getOrdemProducaoId();
        entity.fornecedor = lote.getFornecedor();
        entity.notaFiscal = lote.getNotaFiscal();
        entity.quantidade = lote.getQuantidade();
        entity.unidadeDeMedida = lote.getUnidadeDeMedida();
        entity.dataFabricacao = lote.getDataFabricacao();
        entity.dataValidade = lote.getDataValidade();
        entity.status = lote.getStatus();
        entity.criadoEm = lote.getCriadoEm();
        return entity;
    }

    public Lote paraDominio() {
        return Lote.reconstituir(id, numeroLote, materialId, ordemProducaoId,
                fornecedor, notaFiscal, quantidade, unidadeDeMedida, dataFabricacao, dataValidade,
                status, criadoEm);
    }
}
