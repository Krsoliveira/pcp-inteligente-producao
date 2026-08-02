package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo da requisição de mudança de status.
 */
public record AtualizarStatusRequest(

        @NotNull(message = "status é obrigatório")
        StatusOrdemProducao status) {
}
