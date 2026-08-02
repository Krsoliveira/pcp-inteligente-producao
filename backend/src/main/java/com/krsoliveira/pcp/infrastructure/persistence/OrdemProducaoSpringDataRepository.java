package com.krsoliveira.pcp.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositório Spring Data: as consultas são geradas a partir do nome dos métodos.
 */
public interface OrdemProducaoSpringDataRepository extends JpaRepository<OrdemProducaoJpaEntity, UUID> {

    boolean existsByCodigo(String codigo);
}
