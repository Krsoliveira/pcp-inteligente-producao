package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Espelho da tabela {@code material} para o JPA/Hibernate.
 * Conversão entre domínio e persistência via {@link #deDominio} e {@link #paraDominio}.
 */
@Entity
@Table(name = "material")
public class MaterialJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMaterial tipo;

    @Column(name = "unidade_de_medida", nullable = false, length = 10)
    private String unidadeDeMedida;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected MaterialJpaEntity() {}

    public static MaterialJpaEntity deDominio(Material material) {
        MaterialJpaEntity entity = new MaterialJpaEntity();
        entity.id = material.getId();
        entity.codigo = material.getCodigo();
        entity.descricao = material.getDescricao();
        entity.tipo = material.getTipo();
        entity.unidadeDeMedida = material.getUnidadeDeMedida();
        entity.criadoEm = material.getCriadoEm();
        entity.atualizadoEm = material.getAtualizadoEm();
        return entity;
    }

    public Material paraDominio() {
        return Material.reconstituir(id, codigo, descricao, tipo, unidadeDeMedida,
                criadoEm, atualizadoEm);
    }
}