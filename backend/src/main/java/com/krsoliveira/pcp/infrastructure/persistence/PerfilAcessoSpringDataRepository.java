package com.krsoliveira.pcp.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PerfilAcessoSpringDataRepository extends JpaRepository<PerfilAcessoJpaEntity, UUID> {

    /** SELECT ... FOR UPDATE: segura a linha do perfil até o fim da transação. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PerfilAcessoJpaEntity p WHERE p.id = :id")
    Optional<PerfilAcessoJpaEntity> buscarParaAtualizar(@Param("id") UUID id);

    Optional<PerfilAcessoJpaEntity> findByNomeIgnoreCase(String nome);

    Optional<PerfilAcessoJpaEntity> findByAdministradorTrue();

    Optional<PerfilAcessoJpaEntity> findByPadraoDoAutocadastroTrue();

    List<PerfilAcessoJpaEntity> findAllByOrderByNomeAsc();
}
