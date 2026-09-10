package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.application.lista.CadastrarListaTecnica;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemListaTecnicaRequest(

        @NotNull(message = "O ID do material componente é obrigatório.")
        UUID materialComponenteId,

        @NotNull(message = "A quantidade planejada é obrigatória.")
        @DecimalMin(value = "0.0001", message = "A quantidade planejada deve ser maior que zero.")
        BigDecimal quantidadePlanejada,

        @NotBlank(message = "A unidade de medida do componente é obrigatória.")
        @Size(max = 10, message = "A unidade de medida deve ter no máximo 10 caracteres.")
        String unidadeDeMedida

) {
    public CadastrarListaTecnica.ItemComando paraComando() {
        return new CadastrarListaTecnica.ItemComando(
                materialComponenteId, quantidadePlanejada, unidadeDeMedida);
    }
}
