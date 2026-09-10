package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.application.lista.CadastrarListaTecnica;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CadastrarListaTecnicaRequest(

        @NotNull(message = "O ID do material é obrigatório.")
        UUID materialId,

        @NotBlank(message = "A versão da lista técnica é obrigatória.")
        @Size(max = 50, message = "A versão deve ter no máximo 50 caracteres.")
        String versao,

        @NotEmpty(message = "A lista técnica deve ter ao menos um componente.")
        @Valid
        List<ItemListaTecnicaRequest> itens

) {
    public CadastrarListaTecnica.Comando paraComando() {
        return new CadastrarListaTecnica.Comando(
                materialId,
                versao,
                itens.stream().map(ItemListaTecnicaRequest::paraComando).toList()
        );
    }
}
