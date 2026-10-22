package com.krsoliveira.pcp.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface EventoAuditoriaSpringDataRepository
        extends JpaRepository<EventoAuditoriaJpaEntity, UUID>,
                JpaSpecificationExecutor<EventoAuditoriaJpaEntity> {
}
