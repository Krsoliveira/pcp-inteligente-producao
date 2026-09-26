package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.StatusLote;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LoteResponse(UUID id,
                           String numeroLote,
                           UUID materialId,
                           UUID ordemProducaoId,
                           String origem,
                           String fornecedor,
                           String notaFiscal,
                           BigDecimal quantidade,
                           String unidadeDeMedida,
                           LocalDate dataFabricacao,
                           LocalDate dataValidade,
                           StatusLote status,
                           Instant criadoEm) {

    public static LoteResponse de(Lote lote) {
        return new LoteResponse(
                lote.getId(),
                lote.getNumeroLote(),
                lote.getMaterialId(),
                lote.getOrdemProducaoId(),
                lote.ehDeCompra() ? "COMPRA" : "PRODUCAO",
                lote.getFornecedor(),
                lote.getNotaFiscal(),
                lote.getQuantidade(),
                lote.getUnidadeDeMedida(),
                lote.getDataFabricacao(),
                lote.getDataValidade(),
                lote.getStatus(),
                lote.getCriadoEm());
    }
}
