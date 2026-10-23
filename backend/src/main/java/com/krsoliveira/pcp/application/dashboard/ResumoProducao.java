package com.krsoliveira.pcp.application.dashboard;

import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;

import java.util.Map;

/**
 * Números do painel principal, calculados no banco em vez de contados no navegador.
 *
 * @param ordensPorStatus todos os status aparecem, com zero quando não há ordens
 */
public record ResumoProducao(long totalOrdens,
                             Map<StatusOrdemProducao, Long> ordensPorStatus,
                             long ordensAtrasadas,
                             long lotesDisponiveis) {
}
