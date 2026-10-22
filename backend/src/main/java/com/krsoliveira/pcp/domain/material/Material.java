package com.krsoliveira.pcp.domain.material;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.Assinatura;

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
    private final Assinatura assinatura;

    private Material(UUID id, String codigo, String descricao, TipoMaterial tipo,
                     String unidadeDeMedida, Assinatura assinatura) {
        this.id = id;
        this.codigo = codigo;
        this.descricao = descricao;
        this.tipo = tipo;
        this.unidadeDeMedida = unidadeDeMedida;
        this.assinatura = assinatura;
    }

    /**
     * Fábrica para um material NOVO. Valida todas as invariantes antes de criar — inclusive
     * que o código tem 9 dígitos na faixa do tipo ({@link CodigoMaterial}).
     */
    public static Material criar(String codigo, String descricao, TipoMaterial tipo,
                                 String unidadeDeMedida, String usuario) {
        if (descricao == null || descricao.isBlank()) {
            throw new RegraDeNegocioException("A descrição do material é obrigatória.");
        }
        if (tipo == null) {
            throw new RegraDeNegocioException("O tipo do material é obrigatório.");
        }
        if (unidadeDeMedida == null || unidadeDeMedida.isBlank()) {
            throw new RegraDeNegocioException("A unidade de medida do material é obrigatória.");
        }
        CodigoMaterial.validar(codigo, tipo);
        return new Material(UUID.randomUUID(), codigo,
                descricao.trim(), tipo, unidadeDeMedida.trim(), Assinatura.nova(usuario));
    }

    /**
     * Reconstrói um material EXISTENTE a partir do banco de dados.
     * Não revalida invariantes.
     */
    public static Material reconstituir(UUID id, String codigo, String descricao,
                                        TipoMaterial tipo, String unidadeDeMedida,
                                        Assinatura assinatura) {
        return new Material(id, codigo, descricao, tipo, unidadeDeMedida, assinatura);
    }

    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getDescricao() { return descricao; }
    public TipoMaterial getTipo() { return tipo; }
    public String getUnidadeDeMedida() { return unidadeDeMedida; }
    public Assinatura getAssinatura() { return assinatura; }
    public Instant getCriadoEm() { return assinatura.criadoEm(); }
    public Instant getAtualizadoEm() { return assinatura.alteradoEm(); }
}