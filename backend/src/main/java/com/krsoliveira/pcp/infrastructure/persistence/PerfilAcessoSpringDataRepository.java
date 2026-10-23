package com.krsoliveira.pcp.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PerfilAcessoSpringDataRepository extends JpaRepository<PerfilAcessoJpaEntity, UUID> {

    Optional<PerfilAcessoJpaEntity> findByNomeIgnoreCase(String nome);

    Optional<PerfilAcessoJpaEntity> findByAdministradorTrue();

    Optional<PerfilAcessoJpaEntity> findByPadraoDoAutocadastroTrue();

    List<PerfilAcessoJpaEntity> findAllByOrderByNomeAsc();
}
