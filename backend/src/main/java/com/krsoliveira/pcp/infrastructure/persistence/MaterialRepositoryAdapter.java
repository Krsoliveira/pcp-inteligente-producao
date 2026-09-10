package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador: implementa a porta {@link MaterialRepository} usando Spring Data JPA.
 */
@Repository
public class MaterialRepositoryAdapter implements MaterialRepository {

    private final MaterialSpringDataRepository springDataRepository;

    public MaterialRepositoryAdapter(MaterialSpringDataRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public void salvar(Material material) {
        springDataRepository.save(MaterialJpaEntity.deDominio(material));
    }

    @Override
    public Optional<Material> buscarPorId(UUID id) {
        return springDataRepository.findById(id).map(MaterialJpaEntity::paraDominio);
    }

    @Override
    public Optional<Material> buscarPorCodigo(String codigo) {
        return springDataRepository.findByCodigo(codigo).map(MaterialJpaEntity::paraDominio);
    }

    @Override
    public List<Material> listarTodos() {
        return springDataRepository.findAll().stream()
                .map(MaterialJpaEntity::paraDominio)
                .toList();
    }

    @Override
    public boolean existePorCodigo(String codigo) {
        return springDataRepository.existsByCodigo(codigo);
    }
}
