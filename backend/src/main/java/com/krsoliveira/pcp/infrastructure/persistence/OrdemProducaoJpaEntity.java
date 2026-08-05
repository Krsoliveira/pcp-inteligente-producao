package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Espelho da tabela {@code ordem_producao} para o JPA/Hibernate.
 *
 * É uma classe DIFERENTE da entidade de domínio de propósito: aqui vivem as
 * anotações de persistência; lá vivem as regras de negócio. A conversão entre
 * as duas acontece em {@link #deDominio} e {@link #paraDominio}.
 */
@Entity
@Table(name = "ordem_producao")
public class OrdemProducaoJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String produto;

    @Column(name = "centro_de_trabalho", nullable = false, length = 60)
    private String centroDeTrabalho;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "inicio_planejado", nullable = false)
    private LocalDate inicioPlanejado;

    @Column(name = "fim_planejado", nullable = false)
    private LocalDate fimPlanejado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOrdemProducao status;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    /** Exigido pelo JPA; não usar diretamente. */
    protected OrdemProducaoJpaEntity() {
    }

    public static OrdemProducaoJpaEntity deDominio(OrdemProducao ordem) {
        OrdemProducaoJpaEntity entity = new OrdemProducaoJpaEntity();
        entity.id = ordem.getId();
        entity.codigo = ordem.getCodigo();
        entity.produto = ordem.getProduto();
        entity.centroDeTrabalho = ordem.getCentroDeTrabalho();
        entity.quantidade = ordem.getQuantidade();
        entity.inicioPlanejado = ordem.getInicioPlanejado();
        entity.fimPlanejado = ordem.getFimPlanejado();
        entity.status = ordem.getStatus();
        entity.criadaEm = ordem.getCriadaEm();
        entity.atualizadaEm = ordem.getAtualizadaEm();
        return entity;
    }

    public OrdemProducao paraDominio() {
        return OrdemProducao.reconstituir(id, codigo, produto, centroDeTrabalho, quantidade,
                inicioPlanejado, fimPlanejado, status, criadaEm, atualizadaEm);
    }
}