package com.krsoliveira.pcp.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Repositório Spring Data: as consultas são geradas a partir do nome dos métodos.
 * {@link JpaSpecificationExecutor} permite montar filtros opcionais (pesquisa paginada).
 */
public interface OrdemProducaoSpringDataRepository
        extends JpaRepository<OrdemProducaoJpaEntity, UUID>, JpaSpecificationExecutor<OrdemProducaoJpaEntity> {

    boolean existsByCodigo(String codigo);

    /** SELECT ... FOR UPDATE: segura a linha da ordem até o fim da transação. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrdemProducaoJpaEntity o WHERE o.id = :id")
    Optional<OrdemProducaoJpaEntity> buscarParaAtualizar(@Param("id") UUID id);

    @Query("SELECT o.status, COUNT(o) FROM OrdemProducaoJpaEntity o GROUP BY o.status")
    List<Object[]> contarAgrupadoPorStatus();

    long countByStatusInAndFimPlanejadoBefore(Collection<StatusOrdemProducao> status, LocalDate data);

    List<OrdemProducaoJpaEntity> findByFimPlanejadoBetween(LocalDate de, LocalDate ate);

    @Query("""
            SELECT o.centroDeTrabalho, COUNT(o), MIN(o.fimPlanejado)
            FROM OrdemProducaoJpaEntity o
            WHERE o.status IN :abertos AND o.fimPlanejado < :hoje
            GROUP BY o.centroDeTrabalho
            ORDER BY COUNT(o) DESC, o.centroDeTrabalho
            """)
    List<Object[]> atrasadasAgrupadasPorCentro(@Param("abertos") Collection<StatusOrdemProducao> abertos,
                                               @Param("hoje") LocalDate hoje);
}
