package com.krsoliveira.pcp.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoteSpringDataRepository extends JpaRepository<LoteJpaEntity, UUID> {

    List<LoteJpaEntity> findByOrdemProducaoId(UUID ordemProducaoId);

    List<LoteJpaEntity> findByOrdemProducaoIdIn(Collection<UUID> ordemProducaoIds);

    /** SELECT ... FOR UPDATE: segura a linha do lote até o fim da transação. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM LoteJpaEntity l WHERE l.id = :id")
    Optional<LoteJpaEntity> buscarParaAtualizar(@Param("id") UUID id);

    @Query("SELECT l FROM LoteJpaEntity l WHERE l.materialId = :materialId "
            + "AND l.status = com.krsoliveira.pcp.domain.lote.StatusLote.DISPONIVEL AND l.saldo > 0 "
            + "ORDER BY l.dataValidade, l.dataFabricacao, l.numeroLote")
    List<LoteJpaEntity> disponiveisPorMaterial(@Param("materialId") UUID materialId);

    List<LoteJpaEntity> findByMaterialId(UUID materialId);

    List<LoteJpaEntity> findByNotaFiscalIdIn(Collection<UUID> notaFiscalIds);

    @Query("SELECT COUNT(l) > 0 FROM LoteJpaEntity l WHERE l.materialId = :materialId "
            + "AND l.numeroLote = :numeroLote AND COALESCE(l.fornecedor, '') = :fornecedor")
    boolean existeLote(@Param("materialId") UUID materialId, @Param("numeroLote") String numeroLote,
                       @Param("fornecedor") String fornecedor);

    /** pg_advisory_xact_lock: liberado automaticamente no fim da transação. */
    @Query(value = "SELECT CAST(pg_advisory_xact_lock(:chave) AS TEXT)", nativeQuery = true)
    String bloquear(@Param("chave") long chave);

    /** Maior número de lote de produção do dia ({@code AAMMDD} + 4 dígitos). */
    @Query("SELECT MAX(l.numeroLote) FROM LoteJpaEntity l WHERE l.ordemProducaoId IS NOT NULL "
            + "AND l.numeroLote LIKE CONCAT(:prefixo, '%')")
    Optional<String> ultimoLoteProducaoDoDia(@Param("prefixo") String prefixo);

    /** Posição de estoque por material (ver {@link EstoqueConsultaAdapter}). */
    @Query("SELECT l.materialId, "
            + "SUM(CASE WHEN l.status = com.krsoliveira.pcp.domain.lote.StatusLote.DISPONIVEL "
            + "         AND l.dataValidade >= :hoje THEN l.saldo ELSE 0 END), "
            + "SUM(CASE WHEN l.status = com.krsoliveira.pcp.domain.lote.StatusLote.DISPONIVEL "
            + "         AND l.dataValidade >= :hoje AND l.saldo > 0 THEN 1 ELSE 0 END), "
            + "SUM(CASE WHEN l.saldo > 0 AND (l.status <> com.krsoliveira.pcp.domain.lote.StatusLote.DISPONIVEL "
            + "         OR l.dataValidade < :hoje) THEN l.saldo ELSE 0 END), "
            + "MIN(CASE WHEN l.status = com.krsoliveira.pcp.domain.lote.StatusLote.DISPONIVEL "
            + "         AND l.dataValidade >= :hoje AND l.saldo > 0 THEN l.dataValidade ELSE NULL END), "
            + "MAX(l.criadoEm) "
            + "FROM LoteJpaEntity l GROUP BY l.materialId")
    List<Object[]> posicoesDeEstoque(@Param("hoje") LocalDate hoje);
}
