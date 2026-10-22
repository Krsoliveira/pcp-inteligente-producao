package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.lote.AlocacaoLote;
import com.krsoliveira.pcp.domain.lote.AlocacaoLoteRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** ADAPTADOR: implementa a porta {@link AlocacaoLoteRepository} com JPA/PostgreSQL. */
@Repository
public class AlocacaoLoteRepositoryAdapter implements AlocacaoLoteRepository {

    private final AlocacaoLoteSpringDataRepository springData;

    public AlocacaoLoteRepositoryAdapter(AlocacaoLoteSpringDataRepository springData) {
        this.springData = springData;
    }

    @Override
    public void salvarTodas(List<AlocacaoLote> alocacoes) {
        springData.saveAll(alocacoes.stream().map(AlocacaoLoteJpaEntity::deDominio).toList());
    }

    @Override
    public List<AlocacaoLote> listarPorConsumos(Collection<UUID> consumoMaterialIds) {
        if (consumoMaterialIds.isEmpty()) return List.of();
        return springData.findByConsumoMaterialIdIn(consumoMaterialIds).stream()
                .map(AlocacaoLoteJpaEntity::paraDominio).toList();
    }

    @Override
    public List<AlocacaoLote> listarPorLote(UUID loteId) {
        return springData.findByLoteIdOrderByCriadoEmDesc(loteId).stream()
                .map(AlocacaoLoteJpaEntity::paraDominio).toList();
    }
}
