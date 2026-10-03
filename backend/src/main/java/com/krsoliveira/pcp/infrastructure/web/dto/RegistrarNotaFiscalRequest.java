package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.application.notafiscal.RegistrarEntradaNotaFiscal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Entrada de nota fiscal de compra: cabeçalho e itens (cada item vira um lote). */
public record RegistrarNotaFiscalRequest(

        @NotBlank(message = "O fornecedor é obrigatório.")
        @Size(max = 150, message = "O fornecedor deve ter no máximo 150 caracteres.")
        String fornecedor,

        @NotBlank(message = "O número da nota fiscal é obrigatório.")
        @Size(max = 44, message = "O número da nota fiscal deve ter no máximo 44 caracteres.")
        String numero,

        @NotNull(message = "A data de emissão é obrigatória.")
        @PastOrPresent(message = "A data de emissão não pode estar no futuro.")
        LocalDate dataEmissao,

        @NotNull(message = "A data de recebimento é obrigatória.")
        @PastOrPresent(message = "A data de recebimento não pode estar no futuro.")
        LocalDate dataRecebimento,

        @NotEmpty(message = "Informe pelo menos um item.")
        @Valid
        List<Item> itens) {

    public record Item(
            @NotNull(message = "O material é obrigatório.")
            UUID materialId,

            @NotNull(message = "A quantidade é obrigatória.")
            @DecimalMin(value = "0.0", inclusive = false, message = "A quantidade deve ser maior que zero.")
            BigDecimal quantidade,

            @NotBlank(message = "O lote do fornecedor é obrigatório.")
            @Size(max = 20, message = "O lote deve ter no máximo 20 caracteres.")
            String numeroLote,

            @NotNull(message = "A data de fabricação é obrigatória.")
            @PastOrPresent(message = "A data de fabricação não pode estar no futuro.")
            LocalDate dataFabricacao,

            @NotNull(message = "A data de validade é obrigatória.")
            LocalDate dataValidade) {}

    public RegistrarEntradaNotaFiscal.Comando paraComando() {
        return new RegistrarEntradaNotaFiscal.Comando(fornecedor, numero, dataEmissao, dataRecebimento,
                itens.stream().map(i -> new RegistrarEntradaNotaFiscal.Item(i.materialId(), i.quantidade(),
                        i.numeroLote(), i.dataFabricacao(), i.dataValidade())).toList());
    }
}
