package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.application.material.CadastrarMaterial;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CadastrarMaterialRequest(

        @NotBlank(message = "O código do material é obrigatório.")
        @Size(max = 30, message = "O código deve ter no máximo 30 caracteres.")
        String codigo,

        @NotBlank(message = "A descrição do material é obrigatória.")
        @Size(max = 200, message = "A descrição deve ter no máximo 200 caracteres.")
        String descricao,

        @NotNull(message = "O tipo do material é obrigatório.")
        TipoMaterial tipo,

        @NotBlank(message = "A unidade de medida é obrigatória.")
        @Size(max = 10, message = "A unidade de medida deve ter no máximo 10 caracteres.")
        String unidadeDeMedida

) {
    public CadastrarMaterial.Comando paraComando() {
        return new CadastrarMaterial.Comando(codigo, descricao, tipo, unidadeDeMedida);
    }
}
