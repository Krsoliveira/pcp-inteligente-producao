package com.krsoliveira.pcp.domain.ordem;

import java.time.LocalDate;

/**
 * Ordens abertas e atrasadas de um centro de trabalho.
 *
 * @param fimPlanejadoMaisAntigo o fim planejado mais antigo entre elas (a que está atrasada há mais tempo)
 */
public record AtrasoPorCentro(String centroDeTrabalho, long ordensAtrasadas, LocalDate fimPlanejadoMaisAntigo) {
}
