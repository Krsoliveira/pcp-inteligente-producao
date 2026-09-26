package com.krsoliveira.pcp.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsumoMaterialSpringDataRepository extends JpaRepository<ConsumoMaterialJpaEntity, UUID> {

    List<ConsumoMaterialJpaEntity> findByOrdemProducaoId(UUID ordemProducaoId);
}
