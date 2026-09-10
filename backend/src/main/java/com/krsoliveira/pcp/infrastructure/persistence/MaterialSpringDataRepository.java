package com.krsoliveira.pcp.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MaterialSpringDataRepository extends JpaRepository<MaterialJpaEntity, UUID> {

    Optional<MaterialJpaEntity> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}
