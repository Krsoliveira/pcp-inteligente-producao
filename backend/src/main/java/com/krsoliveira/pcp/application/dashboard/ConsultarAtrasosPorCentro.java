package com.krsoliveira.pcp.application.dashboard;

import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Caso de uso: onde estão as ordens atrasadas, para apontar o gargalo. Agrupado e contado no banco. */
public class ConsultarAtrasosPorCentro {

    private final OrdemProducaoRepository ordens;

    public ConsultarAtrasosPorCentro(OrdemProducaoRepository ordens) {
        this.ordens = ordens;
    }

    /** @return centros com atraso, do que tem mais ordens atrasadas para o que tem menos (empate: pelo nome) */
    public List<AtrasoDoCentro> executar(LocalDate hoje) {
        return ordens.atrasadasPorCentro(hoje).stream()
                .map(a -> new AtrasoDoCentro(a.centroDeTrabalho(), a.ordensAtrasadas(),
                        ChronoUnit.DAYS.between(a.fimPlanejadoMaisAntigo(), hoje)))
                .toList();
    }
}
