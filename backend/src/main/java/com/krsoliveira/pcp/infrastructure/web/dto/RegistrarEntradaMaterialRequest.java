package com.krsoliveira.pcp.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RegistrarEntradaMaterialRequest(
        @NotNull(message = "Material é obrigatório")
        UUID materialId,

        @NotBlank(message = "Fornecedor é obrigatório")
        @Size(max = 150, message = "Fornecedor deve ter no máximo 150 caracteres")
        String fornecedor,

        @NotBlank(message = "Nota fiscal é obrigatória")
        @Size(max = 44, message = "Nota fiscal deve ter no máximo 44 caracteres")
        String notaFiscal,

        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(value = "0.0001", message = "Quantidade deve ser maior que zero")
        BigDecimal quantidade,

        @NotNull(message = "Data de fabricação é obrigatória")
        @PastOrPresent(message = "Data de fabricação não pode estar no futuro")
        LocalDate dataFabricacao,

        @NotNull(message = "Data de validade é obrigatória")
        LocalDate dataValidade
) {}
