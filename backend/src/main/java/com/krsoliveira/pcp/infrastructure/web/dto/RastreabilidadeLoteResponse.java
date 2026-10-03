package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.application.lote.RastrearLote;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Genealogia de um lote, um nível em cada direção: de quais lotes veio o material
 * consumido para produzi-lo ({@code origens}) e onde ele foi usado ({@code destinos}).
 */
public record RastreabilidadeLoteResponse(UUID loteId,
                                          String numeroLote,
                                          List<Origem> origens,
                                          List<Destino> destinos) {

    /** Um consumo da ordem que gerou o lote e os lotes de onde saiu o material. */
    public record Origem(UUID consumoId,
                         UUID materialId,
                         BigDecimal quantidadePlanejada,
                         BigDecimal quantidadeConsumida,
                         String unidadeDeMedida,
                         List<LoteOrigem> lotes) {}

    public record LoteOrigem(UUID loteId,
                             String numeroLote,
                             String origem,
                             String fornecedor,
                             String notaFiscal,
                             UUID ordemProducaoId,
                             BigDecimal quantidade,
                             String alocadoPor,
                             Instant alocadoEm) {}

    /** Um uso do lote: em qual ordem, quanto, e o lote que ela gerou (se concluída). */
    public record Destino(UUID alocacaoId,
                          BigDecimal quantidade,
                          String unidadeDeMedida,
                          String alocadoPor,
                          Instant alocadoEm,
                          UUID ordemProducaoId,
                          String ordemCodigo,
                          StatusOrdemProducao ordemStatus,
                          UUID materialProduzidoId,
                          UUID loteGeradoId,
                          String loteGeradoNumero) {}

    public static RastreabilidadeLoteResponse de(RastrearLote.Resultado r) {
        return new RastreabilidadeLoteResponse(
                r.lote().getId(),
                r.lote().getNumeroLote(),
                r.origens().stream().map(o -> new Origem(
                        o.consumo().getId(),
                        o.consumo().getMaterialId(),
                        o.consumo().getQuantidadePlanejada(),
                        o.consumo().getQuantidadeConsumida(),
                        o.consumo().getUnidadeDeMedida(),
                        o.lotes().stream().map(u -> loteOrigem(u.lote(), u)).toList())).toList(),
                r.destinos().stream().map(d -> new Destino(
                        d.alocacao().getId(),
                        d.alocacao().getQuantidade(),
                        d.consumo().getUnidadeDeMedida(),
                        d.alocacao().getCriadoPor(),
                        d.alocacao().getCriadoEm(),
                        d.ordem().getId(),
                        d.ordem().getCodigo(),
                        d.ordem().getStatus(),
                        d.ordem().getMaterialId(),
                        d.loteGerado().map(Lote::getId).orElse(null),
                        d.loteGerado().map(Lote::getNumeroLote).orElse(null))).toList());
    }

    private static LoteOrigem loteOrigem(Lote lote, RastrearLote.LoteUsado uso) {
        return new LoteOrigem(lote.getId(), lote.getNumeroLote(), lote.ehDeCompra() ? "COMPRA" : "PRODUCAO",
                lote.getFornecedor(), lote.getNotaFiscal(), lote.getOrdemProducaoId(),
                uso.alocacao().getQuantidade(), uso.alocacao().getCriadoPor(), uso.alocacao().getCriadoEm());
    }
}
