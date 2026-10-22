package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

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

    @Query("SELECT o.status, COUNT(o) FROM OrdemProducaoJpaEntity o GROUP BY o.status")
    List<Object[]> contarAgrupadoPorStatus();

    long countByStatusInAndFimPlanejadoBefore(Collection<StatusOrdemProducao> status, LocalDate data);
}
