package com.krsoliveira.pcp.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListaTecnicaSpringDataRepository
        extends JpaRepository<ListaTecnicaJpaEntity, UUID>, JpaSpecificationExecutor<ListaTecnicaJpaEntity> {

    List<ListaTecnicaJpaEntity> findByMaterialId(UUID materialId);

    /** SELECT ... FOR UPDATE: segura a versão até o fim da transação. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM ListaTecnicaJpaEntity l WHERE l.id = :id")
    Optional<ListaTecnicaJpaEntity> buscarParaAtualizar(@Param("id") UUID id);

    /** SELECT ... FOR SHARE: outras leituras travadas passam; quem quer alterar espera o fim da transação. */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT l FROM ListaTecnicaJpaEntity l WHERE l.id = :id")
    Optional<ListaTecnicaJpaEntity> buscarParaUso(@Param("id") UUID id);

    boolean existsByMaterialIdAndVersao(UUID materialId, String versao);
}
