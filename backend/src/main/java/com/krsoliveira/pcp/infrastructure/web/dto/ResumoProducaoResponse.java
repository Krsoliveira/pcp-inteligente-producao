package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.application.dashboard.ResumoProducao;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;

import java.util.Map;

/** Totais do painel principal. */
public record ResumoProducaoResponse(long totalOrdens,
                                     Map<StatusOrdemProducao, Long> ordensPorStatus,
                                     long ordensAtrasadas,
                                     long lotesDisponiveis) {

    public static ResumoProducaoResponse de(ResumoProducao resumo) {
        return new ResumoProducaoResponse(resumo.totalOrdens(), resumo.ordensPorStatus(),
                resumo.ordensAtrasadas(), resumo.lotesDisponiveis());
    }
}
