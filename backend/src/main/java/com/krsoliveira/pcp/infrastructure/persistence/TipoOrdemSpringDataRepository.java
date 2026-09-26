package com.krsoliveira.pcp.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TipoOrdemSpringDataRepository extends JpaRepository<TipoOrdemJpaEntity, UUID> {

    boolean existsByNomeIgnoreCase(String nome);
}
