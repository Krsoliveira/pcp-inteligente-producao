package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.EventoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record EventoAuditoriaResponse(
        UUID id,
        TipoEntidade tipoEntidade,
        UUID entidadeId,
        String referencia,
        AcaoAuditoria acao,
        String usuario,
        Instant ocorridoEm,
        Map<String, Object> detalhes
) {
    public static EventoAuditoriaResponse de(EventoAuditoria evento) {
        return new EventoAuditoriaResponse(evento.getId(), evento.getTipoEntidade(),
                evento.getEntidadeId(), evento.getReferencia(), evento.getAcao(),
                evento.getUsuario(), evento.getOcorridoEm(), evento.getDetalhes());
    }
}
