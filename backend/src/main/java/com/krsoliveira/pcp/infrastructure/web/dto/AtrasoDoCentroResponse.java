package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.application.dashboard.AtrasoDoCentro;

/** Ordens atrasadas de um centro de trabalho e há quantos dias a mais antiga passou do prazo. */
public record AtrasoDoCentroResponse(String centroDeTrabalho, long ordensAtrasadas, long diasDeAtrasoDaMaisAntiga) {

    public static AtrasoDoCentroResponse de(AtrasoDoCentro atraso) {
        return new AtrasoDoCentroResponse(atraso.centroDeTrabalho(), atraso.ordensAtrasadas(),
                atraso.diasDeAtrasoDaMaisAntiga());
    }
}
