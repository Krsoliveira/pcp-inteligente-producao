package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.lista.StatusListaTecnica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListaTecnicaSpringDataRepository
        extends JpaRepository<ListaTecnicaJpaEntity, UUID> {

    List<ListaTecnicaJpaEntity> findByMaterialId(UUID materialId);

    Optional<ListaTecnicaJpaEntity> findByMaterialIdAndStatus(UUID materialId,
                                                               StatusListaTecnica status);

    boolean existsByMaterialIdAndVersao(UUID materialId, String versao);
}
