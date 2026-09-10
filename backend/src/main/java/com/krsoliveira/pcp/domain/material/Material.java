package com.krsoliveira.pcp.domain.material;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;

import java.time.Instant;
import java.util.UUID;

/**
 * Representa qualquer insumo ou produto gerenciado pelo sistema de PCP:
 * produto acabado, semiacabado ou matéria-prima.
 *
 * Classe de domínio PURA: sem anotações de framework.
 */
public class Material {

    private final UUID id;
    private final String codigo;
    private final String descricao;
    private final TipoMaterial tipo;
    private final String unidadeDeMedida;
    private final Instant criadoEm;
    private Instant atualizadoEm;

    private Material(UUID id, String codigo, String descricao, TipoMaterial tipo,
                     String unidadeDeMedida, Instant criadoEm, Instant atualizadoEm) {
        this.id = id;
        this.codigo = codigo;
        this.descricao = descricao;
        this.tipo = tipo;
        this.unidadeDeMedida = unidadeDeMedida;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    /**
     * Fábrica para um material NOVO. Valida todas as invariantes antes de criar.
     */
    public static Material criar(String codigo, String descricao, TipoMaterial tipo,
                                 String unidadeDeMedida) {
        if (codigo == null || codigo.isBlank()) {
            throw new RegraDeNegocioException("O código do material é obrigatório.");
        }
        if (descricao == null || descricao.isBlank()) {
            throw new RegraDeNegocioException("A descrição do material é obrigatória.");
        }
        if (tipo == null) {
            throw new RegraDeNegocioException("O tipo do material é obrigatório.");
        }
        if (unidadeDeMedida == null || unidadeDeMedida.isBlank()) {
            throw new RegraDeNegocioException("A unidade de medida do material é obrigatória.");
        }
        Instant agora = Instant.now();
        return new Material(UUID.randomUUID(), codigo.trim().toUpperCase(),
                descricao.trim(), tipo, unidadeDeMedida.trim(), agora, agora);
    }

    /**
     * Reconstrói um material EXISTENTE a partir do banco de dados.
     * Não revalida invariantes.
     */
    public static Material reconstituir(UUID id, String codigo, String descricao,
                                        TipoMaterial tipo, String unidadeDeMedida,
                                        Instant criadoEm, Instant atualizadoEm) {
        return new Material(id, codigo, descricao, tipo, unidadeDeMedida, criadoEm, atualizadoEm);
    }

    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getDescricao() { return descricao; }
    public TipoMaterial getTipo() { return tipo; }
    public String getUnidadeDeMedida() { return unidadeDeMedida; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
}