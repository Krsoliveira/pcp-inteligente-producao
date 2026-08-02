package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.application.ordem.CriarOrdemProducao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Corpo da requisição de criação de ordem.
 *
 * As anotações de Bean Validation barram requisições malformadas ANTES de
 * chegar ao caso de uso (HTTP 400). As regras de NEGÓCIO continuam no domínio —
 * esta camada valida apenas formato/presença.
 */
public record CriarOrdemProducaoRequest(

        @NotBlank(message = "codigo é obrigatório")
        @Size(max = 30, message = "codigo deve ter no máximo 30 caracteres")
        String codigo,

        @NotBlank(message = "produto é obrigatório")
        @Size(max = 120, message = "produto deve ter no máximo 120 caracteres")
        String produto,

        @NotNull(message = "quantidade é obrigatória")
        @Positive(message = "quantidade deve ser maior que zero")
        Integer quantidade,

        @NotNull(message = "inicioPlanejado é obrigatório")
        LocalDate inicioPlanejado,

        @NotNull(message = "fimPlanejado é obrigatório")
        LocalDate fimPlanejado) {

    public CriarOrdemProducao.Comando paraComando() {
        return new CriarOrdemProducao.Comando(codigo, produto, quantidade, inicioPlanejado, fimPlanejado);
    }
}
