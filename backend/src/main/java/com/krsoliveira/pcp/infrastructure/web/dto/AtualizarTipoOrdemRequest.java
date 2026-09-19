package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.application.ordem.AtualizarTipoOrdem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AtualizarTipoOrdemRequest(

        @NotBlank(message = "nome é obrigatório")
        @Size(max = 60, message = "nome deve ter no máximo 60 caracteres")
        String nome,

        @Size(max = 200, message = "descrição deve ter no máximo 200 caracteres")
        String descricao,

        @NotBlank(message = "cor é obrigatória")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "cor deve ser um código hex válido (ex.: #1565c0)")
        String cor) {

    public AtualizarTipoOrdem.Comando paraComando(UUID id) {
        return new AtualizarTipoOrdem.Comando(id, nome, descricao, cor);
    }
}
