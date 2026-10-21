package com.krsoliveira.pcp.application.dashboard;

import com.krsoliveira.pcp.domain.lote.LoteRepository;
import com.krsoliveira.pcp.domain.lote.StatusLote;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

/**
 * Caso de uso: totais do painel principal (ordens por status, atrasadas e lotes disponíveis)
 * sem trazer as listas completas.
 */
public class ConsultarResumoProducao {

    private final OrdemProducaoRepository ordens;
    private final LoteRepository lotes;

    public ConsultarResumoProducao(OrdemProducaoRepository ordens, LoteRepository lotes) {
        this.ordens = ordens;
        this.lotes = lotes;
    }

    /** @param hoje dia usado para decidir o que está atrasado (parâmetro para o resultado ser testável) */
    public ResumoProducao executar(LocalDate hoje) {
        Map<StatusOrdemProducao, Long> contagem = new EnumMap<>(StatusOrdemProducao.class);
        for (StatusOrdemProducao status : StatusOrdemProducao.values()) {
            contagem.put(status, 0L);
        }
        contagem.putAll(ordens.contarPorStatus());

        long total = contagem.values().stream().mapToLong(Long::longValue).sum();
        return new ResumoProducao(total, contagem, ordens.contarAtrasadas(hoje),
                lotes.contarPorStatus(StatusLote.DISPONIVEL));
    }
}
