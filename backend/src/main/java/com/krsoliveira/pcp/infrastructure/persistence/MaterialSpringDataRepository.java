package com.krsoliveira.pcp.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MaterialSpringDataRepository extends JpaRepository<MaterialJpaEntity, UUID> {

    Optional<MaterialJpaEntity> findByCodigo(String codigo);

    /** pg_advisory_xact_lock: liberado automaticamente no fim da transação. */
    @Query(value = "SELECT CAST(pg_advisory_xact_lock(:chave) AS TEXT)", nativeQuery = true)
    String bloquearFaixaDeCodigo(@Param("chave") long chave);

    @Query("SELECT MAX(m.codigo) FROM MaterialJpaEntity m WHERE m.codigo LIKE CONCAT(:prefixo, '%')")
    Optional<String> ultimoCodigoComPrefixo(@Param("prefixo") String prefixo);
}
