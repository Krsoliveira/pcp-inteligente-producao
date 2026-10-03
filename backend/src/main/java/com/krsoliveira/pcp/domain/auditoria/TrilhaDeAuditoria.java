package com.krsoliveira.pcp.domain.auditoria;

import java.util.List;

/**
 * Porta de persistência da trilha de auditoria. Só permite <b>acrescentar</b> e
 * consultar — não há operação de alteração nem de exclusão.
 */
public interface TrilhaDeAuditoria {

    void registrar(List<EventoAuditoria> eventos);

    /** Eventos que atendem ao filtro, do mais recente para o mais antigo. */
    Pagina<EventoAuditoria> consultar(FiltroEventos filtro, int pagina, int tamanho);
}
