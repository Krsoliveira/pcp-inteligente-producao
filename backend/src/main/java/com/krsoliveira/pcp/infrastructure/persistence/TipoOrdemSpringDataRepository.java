package com.krsoliveira.pcp.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TipoOrdemSpringDataRepository extends JpaRepository<TipoOrdemJpaEntity, UUID> {

    /** SELECT ... FOR UPDATE: segura a linha do tipo de ordem até o fim da transação. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TipoOrdemJpaEntity t WHERE t.id = :id")
    Optional<TipoOrdemJpaEntity> buscarParaAtualizar(@Param("id") UUID id);

    boolean existsByNomeIgnoreCase(String nome);
}
