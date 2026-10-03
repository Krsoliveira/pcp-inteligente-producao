package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterialRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ADAPTADOR: implementa a porta {@link ConsumoMaterialRepository} usando JPA/PostgreSQL.
 */
@Repository
public class ConsumoMaterialRepositoryAdapter implements ConsumoMaterialRepository {

    private final ConsumoMaterialSpringDataRepository springData;

    public ConsumoMaterialRepositoryAdapter(ConsumoMaterialSpringDataRepository springData) {
        this.springData = springData;
    }

    @Override
    public ConsumoMaterial salvar(ConsumoMaterial consumo) {
        return springData.save(ConsumoMaterialJpaEntity.deDominio(consumo)).paraDominio();
    }

    @Override
    public List<ConsumoMaterial> salvarTodos(List<ConsumoMaterial> consumos) {
        return springData.saveAll(
                consumos.stream().map(ConsumoMaterialJpaEntity::deDominio).toList()
        ).stream().map(ConsumoMaterialJpaEntity::paraDominio).toList();
    }

    @Override
    public Optional<ConsumoMaterial> buscarPorId(UUID id) {
        return springData.findById(id).map(ConsumoMaterialJpaEntity::paraDominio);
    }

    @Override
    public List<ConsumoMaterial> buscarPorIds(Collection<UUID> ids) {
        if (ids.isEmpty()) return List.of();
        return springData.findAllById(ids).stream().map(ConsumoMaterialJpaEntity::paraDominio).toList();
    }

    @Override
    public List<ConsumoMaterial> listarPorOrdemProducao(UUID ordemProducaoId) {
        return springData.findByOrdemProducaoId(ordemProducaoId).stream()
                .map(ConsumoMaterialJpaEntity::paraDominio)
                .toList();
    }
}
