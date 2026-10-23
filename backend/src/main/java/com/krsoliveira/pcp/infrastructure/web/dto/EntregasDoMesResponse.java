package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.application.dashboard.EntregasDoMes;

/** Um mês do gráfico de entregas; {@code mes} no formato AAAA-MM. */
public record EntregasDoMesResponse(String mes, long noPrazo, long comAtraso, long abertasAtrasadas, long abertasNoPrazo) {

    public static EntregasDoMesResponse de(EntregasDoMes entregas) {
        return new EntregasDoMesResponse(entregas.mes().toString(), entregas.noPrazo(), entregas.comAtraso(),
                entregas.abertasAtrasadas(), entregas.abertasNoPrazo());
    }
}
