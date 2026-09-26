package com.krsoliveira.pcp.application.material;

import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: consultar materiais (individual ou listagem).
 */
public class ConsultarMateriais {

    private final MaterialRepository materialRepository;

    public ConsultarMateriais(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    public Material buscarPorId(UUID id) {
        return materialRepository.buscarPorId(id)
                .orElseThrow(() -> new MaterialNaoEncontradoException(id));
    }

    public List<Material> listarTodos() {
        return materialRepository.listarTodos();
    }
}