package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.ordem.TipoOrdem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Espelho da tabela {@code tipo_ordem} para o JPA/Hibernate.
 * Conversão entre domínio e persistência via {@link #deDominio} e {@link #paraDominio}.
 */
@Entity
@Table(name = "tipo_ordem")
public class TipoOrdemJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 60)
    private String nome;

    @Column(length = 200)
    private String descricao;

    @Column(nullable = false, length = 7)
    private String cor;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected TipoOrdemJpaEntity() {}

    public static TipoOrdemJpaEntity deDominio(TipoOrdem tipo) {
        TipoOrdemJpaEntity entity = new TipoOrdemJpaEntity();
        entity.id = tipo.getId();
        entity.nome = tipo.getNome();
        entity.descricao = tipo.getDescricao();
        entity.cor = tipo.getCor();
        entity.criadoEm = tipo.getCriadoEm();
        entity.atualizadoEm = tipo.getAtualizadoEm();
        return entity;
    }

    public TipoOrdem paraDominio() {
        return TipoOrdem.reconstituir(id, nome, descricao, cor, criadoEm, atualizadoEm);
    }
}
