package com.krsoliveira.pcp.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrocarSenhaRequest(
        @NotBlank(message = "Informe a senha atual")
        String senhaAtual,

        @NotBlank(message = "Informe a nova senha")
        @Size(min = 8, max = 72, message = "A nova senha deve ter entre 8 e 72 caracteres")
        String novaSenha
) {}
