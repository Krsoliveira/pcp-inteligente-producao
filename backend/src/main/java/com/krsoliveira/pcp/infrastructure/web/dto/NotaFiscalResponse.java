package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.notafiscal.NotaFiscalEntrada;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Nota fiscal de entrada com seus itens — os lotes que ela trouxe. */
public record NotaFiscalResponse(UUID id,
                                 String fornecedor,
                                 String numero,
                                 LocalDate dataEmissao,
                                 LocalDate dataRecebimento,
                                 String registradaPor,
                                 Instant registradaEm,
                                 int quantidadeItens,
                                 List<LoteResponse> itens) {

    public static NotaFiscalResponse de(NotaFiscalEntrada nota, List<Lote> lotes) {
        return new NotaFiscalResponse(nota.getId(), nota.getFornecedor(), nota.getNumero(),
                nota.getDataEmissao(), nota.getDataRecebimento(), nota.getAssinatura().criadoPor(),
                nota.getRegistradaEm(), lotes.size(),
                lotes.stream().sorted(Comparator.comparing(Lote::getNumeroLote)).map(LoteResponse::de).toList());
    }
}
