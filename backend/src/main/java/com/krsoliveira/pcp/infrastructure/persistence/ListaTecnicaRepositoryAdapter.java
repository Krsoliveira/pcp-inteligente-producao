package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnicaRepository;
import com.krsoliveira.pcp.domain.lista.StatusListaTecnica;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador: implementa a porta {@link ListaTecnicaRepository} usando Spring Data JPA.
 */
@Repository
public class ListaTecnicaRepositoryAdapter implements ListaTecnicaRepository {

    private final ListaTecnicaSpringDataRepository springDataRepository;

    public ListaTecnicaRepositoryAdapter(ListaTecnicaSpringDataRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public void salvar(ListaTecnica lista) {
        springDataRepository.save(ListaTecnicaJpaEntity.deDominio(lista));
    }

    @Override
    public Optional<ListaTecnica> buscarPorId(UUID id) {
        return springDataRepository.findById(id).map(ListaTecnicaJpaEntity::paraDominio);
    }

    @Override
    public List<ListaTecnica> listarPorMaterial(UUID materialId) {
        return springDataRepository.findByMaterialId(materialId).stream()
                .map(ListaTecnicaJpaEntity::paraDominio)
                .toList();
    }

    @Override
    public Optional<ListaTecnica> buscarAtivaParaMaterial(UUID materialId) {
        return springDataRepository
                .findByMaterialIdAndStatus(materialId, StatusListaTecnica.ATIVA)
                .map(ListaTecnicaJpaEntity::paraDominio);
    }

    @Override
    public boolean existeVersaoParaMaterial(UUID materialId, String versao) {
        return springDataRepository.existsByMaterialIdAndVersao(materialId, versao);
    }
}
