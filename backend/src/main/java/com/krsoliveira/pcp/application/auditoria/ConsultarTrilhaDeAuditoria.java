package com.krsoliveira.pcp.application.auditoria;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.EventoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.FiltroEventos;
import com.krsoliveira.pcp.domain.auditoria.Pagina;
import com.krsoliveira.pcp.domain.auditoria.TrilhaDeAuditoria;

/**
 * Caso de uso: consultar a trilha de auditoria com filtros (entidade, registro,
 * usuário, ação, período), do evento mais recente para o mais antigo.
 */
public class ConsultarTrilhaDeAuditoria {

    static final int TAMANHO_MAXIMO = 200;

    private final TrilhaDeAuditoria trilha;

    public ConsultarTrilhaDeAuditoria(TrilhaDeAuditoria trilha) {
        this.trilha = trilha;
    }

    public Pagina<EventoAuditoria> executar(FiltroEventos filtro, int pagina, int tamanho) {
        if (pagina < 0) {
            throw new RegraDeNegocioException("A página deve ser zero ou maior.");
        }
        if (tamanho < 1 || tamanho > TAMANHO_MAXIMO) {
            throw new RegraDeNegocioException(
                    "O tamanho da página deve estar entre 1 e %d.".formatted(TAMANHO_MAXIMO));
        }
        if (filtro.de() != null && filtro.ate() != null && filtro.ate().isBefore(filtro.de())) {
            throw new RegraDeNegocioException("O fim do período não pode ser anterior ao início.");
        }
        return trilha.consultar(filtro, pagina, tamanho);
    }
}
