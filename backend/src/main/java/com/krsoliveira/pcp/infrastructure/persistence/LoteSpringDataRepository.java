package com.krsoliveira.pcp.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoteSpringDataRepository extends JpaRepository<LoteJpaEntity, UUID> {

    List<LoteJpaEntity> findByOrdemProducaoId(UUID ordemProducaoId);

    Optional<LoteJpaEntity> findByNumeroLote(String numeroLote);

    @Query("SELECT COUNT(l) + 1 FROM LoteJpaEntity l WHERE l.numeroLote LIKE :prefixo%")
    int proximoSequencial(@Param("prefixo") String prefixo);
}
