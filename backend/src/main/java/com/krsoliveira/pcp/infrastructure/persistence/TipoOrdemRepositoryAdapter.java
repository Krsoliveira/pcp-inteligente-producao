package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.ordem.TipoOrdem;
import com.krsoliveira.pcp.domain.ordem.TipoOrdemRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ADAPTADOR: implementa a porta {@link TipoOrdemRepository} usando JPA/PostgreSQL.
 */
@Repository
public class TipoOrdemRepositoryAdapter implements TipoOrdemRepository {

    private final TipoOrdemSpringDataRepository springData;

    public TipoOrdemRepositoryAdapter(TipoOrdemSpringDataRepository springData) {
        this.springData = springData;
    }

    @Override
    public TipoOrdem salvar(TipoOrdem tipoOrdem) {
        return springData.save(TipoOrdemJpaEntity.deDominio(tipoOrdem)).paraDominio();
    }

    @Override
    public Optional<TipoOrdem> buscarPorId(UUID id) {
        return springData.findById(id).map(TipoOrdemJpaEntity::paraDominio);
    }

    @Override
    public List<TipoOrdem> listarTodos() {
        return springData.findAll().stream()
                .map(TipoOrdemJpaEntity::paraDominio)
                .toList();
    }

    @Override
    public boolean existePorNome(String nome) {
        return springData.existsByNomeIgnoreCase(nome);
    }
}
