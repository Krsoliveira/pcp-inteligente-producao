package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.application.ordem.ConcluirOrdemProducao;
import java.time.LocalDate;

/**
 * Resposta da conclusão de uma ordem de produção.
 * Retorna a ordem concluída e o lote gerado automaticamente.
 */
public record ConcluirOrdemResponse(OrdemProducaoResponse ordem,
                                    LoteResponse lote) {

    public static ConcluirOrdemResponse de(ConcluirOrdemProducao.Resultado resultado, LocalDate hoje) {
        return new ConcluirOrdemResponse(
                OrdemProducaoResponse.de(resultado.ordem(), hoje),
                LoteResponse.de(resultado.lote()));
    }
}
