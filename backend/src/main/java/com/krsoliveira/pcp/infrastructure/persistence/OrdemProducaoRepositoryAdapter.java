package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ADAPTADOR: implementa a porta {@link OrdemProducaoRepository} (declarada no
 * domínio) usando JPA/PostgreSQL. O domínio não sabe que esta classe existe.
 */
@Repository
public class OrdemProducaoRepositoryAdapter implements OrdemProducaoRepository {

    private final OrdemProducaoSpringDataRepository springData;

    public OrdemProducaoRepositoryAdapter(OrdemProducaoSpringDataRepository springData) {
        this.springData = springData;
    }

    @Override
    public OrdemProducao salvar(OrdemProducao ordem) {
        return springData.save(OrdemProducaoJpaEntity.deDominio(ordem)).paraDominio();
    }

    @Override
    public Optional<OrdemProducao> buscarPorId(UUID id) {
        return springData.findById(id).map(OrdemProducaoJpaEntity::paraDominio);
    }

    @Override
    public List<OrdemProducao> listarTodas() {
        return springData.findAll().stream()
                .map(OrdemProducaoJpaEntity::paraDominio)
                .toList();
    }

    @Override
    public boolean existePorCodigo(String codigo) {
        return springData.existsByCodigo(codigo);
    }
}
