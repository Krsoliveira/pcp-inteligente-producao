package com.krsoliveira.pcp.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data JPA para {@link UsuarioJpaEntity}.
 * Visibilidade de pacote: usado apenas pelo adapter.
 */
interface UsuarioSpringDataRepository extends JpaRepository<UsuarioJpaEntity, UUID> {
    Optional<UsuarioJpaEntity> findByEmail(String email);

    List<UsuarioJpaEntity> findAllByOrderByNomeAsc();

    long countByPerfilId(UUID perfilId);

    long countByPerfilIdAndAtivoTrue(UUID perfilId);

    /** SELECT ... FOR UPDATE: segura o usuário até o fim da transação. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UsuarioJpaEntity u WHERE u.id = :id")
    Optional<UsuarioJpaEntity> buscarParaAtualizar(@Param("id") UUID id);
}
