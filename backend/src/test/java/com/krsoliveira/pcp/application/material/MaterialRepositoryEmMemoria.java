package com.krsoliveira.pcp.application.material;

import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação em memória do repositório de materiais — usada nos testes de use case
 * para evitar dependência de Spring ou banco de dados.
 */
class MaterialRepositoryEmMemoria implements MaterialRepository {

    private final List<Material> materiais = new ArrayList<>();

    @Override
    public void salvar(Material material) {
        materiais.removeIf(m -> m.getId().equals(material.getId()));
        materiais.add(material);
    }

    @Override
    public Optional<Material> buscarPorId(UUID id) {
        return materiais.stream().filter(m -> m.getId().equals(id)).findFirst();
    }

    @Override
    public Optional<Material> buscarPorCodigo(String codigo) {
        return materiais.stream().filter(m -> m.getCodigo().equals(codigo)).findFirst();
    }

    @Override
    public List<Material> listarTodos() {
        return List.copyOf(materiais);
    }

    @Override
    public boolean existePorCodigo(String codigo) {
        return materiais.stream().anyMatch(m -> m.getCodigo().equals(codigo));
    }
}
