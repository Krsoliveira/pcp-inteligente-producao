package com.krsoliveira.pcp.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    Optional<LoteJpaEntity> findByNumeroLote(String numeroLote);

    boolean existsByMaterialIdAndFornecedorAndNotaFiscal(UUID materialId, String fornecedor,
                                                         String notaFiscal);

    @Query("SELECT COUNT(l) + 1 FROM LoteJpaEntity l WHERE l.numeroLote LIKE :prefixo%")
    int proximoSequencial(@Param("prefixo") String prefixo);
}
