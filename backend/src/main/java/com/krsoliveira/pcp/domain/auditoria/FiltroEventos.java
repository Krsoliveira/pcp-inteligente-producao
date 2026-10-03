package com.krsoliveira.pcp.domain.auditoria;

import java.time.Instant;
import java.util.UUID;

/** Critérios opcionais para consultar a trilha de auditoria ({@code null} = sem filtro). */
public record FiltroEventos(TipoEntidade tipoEntidade, UUID entidadeId, String usuario,
                            AcaoAuditoria acao, Instant de, Instant ate) {

    public static FiltroEventos daEntidade(TipoEntidade tipo, UUID id) {
        return new FiltroEventos(tipo, id, null, null, null, null);
    }
}
