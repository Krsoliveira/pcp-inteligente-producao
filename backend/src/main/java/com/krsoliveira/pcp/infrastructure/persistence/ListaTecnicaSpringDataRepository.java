package com.krsoliveira.pcp.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface ListaTecnicaSpringDataRepository
        extends JpaRepository<ListaTecnicaJpaEntity, UUID>, JpaSpecificationExecutor<ListaTecnicaJpaEntity> {

    List<ListaTecnicaJpaEntity> findByMaterialId(UUID materialId);

    boolean existsByMaterialIdAndVersao(UUID materialId, String versao);
}
