package com.krsoliveira.pcp.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface NotaFiscalEntradaSpringDataRepository extends JpaRepository<NotaFiscalEntradaJpaEntity, UUID> {

    @Query("SELECT COUNT(n) > 0 FROM NotaFiscalEntradaJpaEntity n "
            + "WHERE LOWER(n.fornecedor) = LOWER(:fornecedor) AND n.numero = :numero")
    boolean existe(@Param("fornecedor") String fornecedor, @Param("numero") String numero);

    @Query("SELECT n FROM NotaFiscalEntradaJpaEntity n "
            + "WHERE (CAST(:de AS LocalDate) IS NULL OR n.dataRecebimento >= :de) "
            + "AND (CAST(:ate AS LocalDate) IS NULL OR n.dataRecebimento <= :ate) "
            + "ORDER BY n.dataRecebimento DESC, n.criadoEm DESC")
    List<NotaFiscalEntradaJpaEntity> porRecebimento(@Param("de") LocalDate de, @Param("ate") LocalDate ate);
}
