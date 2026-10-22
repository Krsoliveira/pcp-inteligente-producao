package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.application.estoque.ConsultarEstoque;
import com.krsoliveira.pcp.domain.material.TipoMaterial;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Posição de estoque de um material, calculada a partir dos lotes. */
public record EstoqueResponse(UUID materialId,
                              String codigo,
                              String descricao,
                              TipoMaterial tipo,
                              String unidadeDeMedida,
                              BigDecimal saldoDisponivel,
                              int lotesDisponiveis,
                              BigDecimal saldoIndisponivel,
                              LocalDate proximoVencimento,
                              Instant ultimaEntrada) {

    public static EstoqueResponse de(ConsultarEstoque.ItemEstoque item) {
        var m = item.material();
        var p = item.posicao();
        return new EstoqueResponse(m.getId(), m.getCodigo(), m.getDescricao(), m.getTipo(),
                m.getUnidadeDeMedida(), p.saldoDisponivel(), p.lotesDisponiveis(), p.saldoIndisponivel(),
                p.proximoVencimento(), p.ultimaEntrada());
    }
}
