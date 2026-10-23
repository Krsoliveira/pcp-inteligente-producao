package com.krsoliveira.pcp.application.dashboard;

/**
 * @param diasDeAtrasoDaMaisAntiga há quantos dias a ordem aberta mais atrasada do centro passou do fim planejado
 */
public record AtrasoDoCentro(String centroDeTrabalho, long ordensAtrasadas, long diasDeAtrasoDaMaisAntiga) {
}
