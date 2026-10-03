package com.krsoliveira.pcp.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AlocacaoLoteSpringDataRepository extends JpaRepository<AlocacaoLoteJpaEntity, UUID> {

    List<AlocacaoLoteJpaEntity> findByConsumoMaterialIdIn(Collection<UUID> consumoMaterialIds);

    List<AlocacaoLoteJpaEntity> findByLoteIdOrderByCriadoEmDesc(UUID loteId);
}
