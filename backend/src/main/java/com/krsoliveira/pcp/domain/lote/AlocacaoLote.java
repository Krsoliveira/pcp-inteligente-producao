package com.krsoliveira.pcp.domain.lote;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Quanto de um lote foi usado num consumo de material — o elo da genealogia.
 *
 * Com ele se rastreia para trás (produto acabado → lotes consumidos → nota fiscal do
 * fornecedor) e para frente (lote de matéria-prima → ordens e lotes que o usaram).
 * É um fato: nasce em {@link Lote#alocar} e nunca muda.
 */
public final class AlocacaoLote {

    private final UUID id;
    private final UUID consumoMaterialId;
    private final UUID loteId;
    private final BigDecimal quantidade;
    private final String criadoPor;
    private final Instant criadoEm;

    private AlocacaoLote(UUID id, UUID consumoMaterialId, UUID loteId, BigDecimal quantidade,
                         String criadoPor, Instant criadoEm) {
        this.id = id;
        this.consumoMaterialId = consumoMaterialId;
        this.loteId = loteId;
        this.quantidade = quantidade;
        this.criadoPor = criadoPor;
        this.criadoEm = criadoEm;
    }

    /** Só o {@link Lote} cria alocações — depois de validar material, status, validade e saldo. */
    static AlocacaoLote registrar(UUID consumoMaterialId, UUID loteId, BigDecimal quantidade,
                                  String usuario, Instant quando) {
        return new AlocacaoLote(UUID.randomUUID(), consumoMaterialId, loteId, quantidade, usuario, quando);
    }

    public static AlocacaoLote reconstituir(UUID id, UUID consumoMaterialId, UUID loteId,
                                            BigDecimal quantidade, String criadoPor, Instant criadoEm) {
        return new AlocacaoLote(id, consumoMaterialId, loteId, quantidade, criadoPor, criadoEm);
    }

    public UUID getId() { return id; }
    public UUID getConsumoMaterialId() { return consumoMaterialId; }
    public UUID getLoteId() { return loteId; }
    public BigDecimal getQuantidade() { return quantidade; }
    public String getCriadoPor() { return criadoPor; }
    public Instant getCriadoEm() { return criadoEm; }
}
