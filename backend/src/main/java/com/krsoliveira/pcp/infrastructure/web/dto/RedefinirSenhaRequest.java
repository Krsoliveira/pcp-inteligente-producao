package com.krsoliveira.pcp.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequest(
        @NotBlank(message = "Informe a senha provisória")
        @Size(min = 8, max = 72, message = "A senha provisória deve ter entre 8 e 72 caracteres")
        String senhaProvisoria
) {}
