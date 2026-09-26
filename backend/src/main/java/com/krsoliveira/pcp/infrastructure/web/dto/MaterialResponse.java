package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.TipoMaterial;

import java.time.Instant;
import java.util.UUID;

public record MaterialResponse(
        UUID id,
        String codigo,
        String descricao,
        TipoMaterial tipo,
        String unidadeDeMedida,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public static MaterialResponse de(Material material) {
        return new MaterialResponse(
                material.getId(),
                material.getCodigo(),
                material.getDescricao(),
                material.getTipo(),
                material.getUnidadeDeMedida(),
                material.getCriadoEm(),
                material.getAtualizadoEm()
        );
    }
}
