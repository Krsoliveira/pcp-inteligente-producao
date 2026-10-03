package com.krsoliveira.pcp.application.estoque;

import com.krsoliveira.pcp.domain.estoque.ConsultaEstoque;
import com.krsoliveira.pcp.domain.estoque.PosicaoEstoque;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caso de uso: estoque por material — todos os materiais, inclusive os sem saldo, com a
 * posição calculada a partir dos lotes. Ordenado por código.
 */
public class ConsultarEstoque {

    private final ConsultaEstoque consultaEstoque;
    private final MaterialRepository materialRepository;

    public ConsultarEstoque(ConsultaEstoque consultaEstoque, MaterialRepository materialRepository) {
        this.consultaEstoque = consultaEstoque;
        this.materialRepository = materialRepository;
    }

    public record ItemEstoque(Material material, PosicaoEstoque posicao) {}

    public List<ItemEstoque> executar(LocalDate hoje) {
        Map<UUID, PosicaoEstoque> posicoes = consultaEstoque.posicoes(hoje).stream()
                .collect(Collectors.toMap(PosicaoEstoque::materialId, Function.identity()));
        return materialRepository.listarTodos().stream()
                .sorted(Comparator.comparing(Material::getCodigo))
                .map(m -> new ItemEstoque(m, posicoes.getOrDefault(m.getId(), PosicaoEstoque.vazia(m.getId()))))
                .toList();
    }
}
