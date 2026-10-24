package com.krsoliveira.pcp.application.dashboard;

import java.time.YearMonth;

/**
 * Como terminaram (ou estão terminando) as ordens cujo fim planejado cai no mês. Canceladas ficam de fora.
 *
 * @param noPrazo           concluídas até o fim planejado
 * @param comAtraso         concluídas depois do fim planejado
 * @param abertasAtrasadas  ainda abertas e já depois do fim planejado
 * @param abertasNoPrazo    ainda abertas, com o fim planejado hoje ou adiante
 */
public record EntregasDoMes(YearMonth mes, long noPrazo, long comAtraso, long abertasAtrasadas, long abertasNoPrazo) {

    /** Concluídas no mês, com ou sem atraso: a base da aderência ao prazo. */
    public long concluidas() {
        return noPrazo + comAtraso;
    }
}
