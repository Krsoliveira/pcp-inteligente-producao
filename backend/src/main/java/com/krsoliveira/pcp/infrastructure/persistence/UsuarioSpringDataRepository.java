package com.krsoliveira.pcp.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data JPA para {@link UsuarioJpaEntity}.
 * Visibilidade de pacote: usado apenas pelo adapter.
 */
interface UsuarioSpringDataRepository extends JpaRepository<UsuarioJpaEntity, UUID> {
    Optional<UsuarioJpaEntity> findByEmail(String email);
}
