package com.krsoliveira.pcp.domain.ordem;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.Assinatura;

import java.time.Instant;
import java.util.UUID;

/**
 * Categorização de ordens de produção definida pelo usuário.
 * Exemplos: Produção Normal, Manutenção, Revenda, Retrabalho.
 *
 * Diferente do {@link StatusOrdemProducao} (ciclo de vida),
 * o tipo de ordem agrupa ordens em categorias de negócio.
 *
 * Classe de domínio PURA: sem anotações de framework.
 */
public class TipoOrdem {

    private final UUID id;
    private String nome;
    private String descricao;
    private String cor;
    private final boolean conversao;
    private Assinatura assinatura;

    private TipoOrdem(UUID id, String nome, String descricao, String cor, boolean conversao, Assinatura assinatura) {
        this.id = id;
        this.conversao = conversao;
        this.nome = nome;
        this.descricao = descricao;
        this.cor = cor;
        this.assinatura = assinatura;
    }

    /**
     * Fábrica para um tipo de ordem NOVO.
     *
     * @param cor código hexadecimal para badge no frontend (ex.: "#1565c0")
     */
    public static TipoOrdem criar(String nome, String descricao, String cor, String usuario) {
        validarNome(nome);
        validarCor(cor);
        return new TipoOrdem(UUID.randomUUID(), nome.trim(), descricao != null ? descricao.trim() : null,
                cor.trim(), false, Assinatura.nova(usuario));
    }

    /**
     * Reconstrói um tipo de ordem EXISTENTE a partir do banco de dados.
     */
    public static TipoOrdem reconstituir(UUID id, String nome, String descricao, String cor,
                                         Assinatura assinatura) {
        return new TipoOrdem(id, nome, descricao, cor, false, assinatura);
    }

    /** Reconstrói um tipo de ordem existente, dizendo se é o tipo de sistema Conversão. */
    public static TipoOrdem reconstituir(UUID id, String nome, String descricao, String cor, boolean conversao,
                                         Assinatura assinatura) {
        return new TipoOrdem(id, nome, descricao, cor, conversao, assinatura);
    }

    /**
     * Atualiza os dados editáveis do tipo de ordem.
     */
    public void atualizar(String nome, String descricao, String cor, String usuario) {
        if (conversao) {
            throw new RegraDeNegocioException("O tipo Conversão é do sistema e não pode ser alterado.");
        }
        Assinatura alterada = assinatura.alterada(usuario);
        validarNome(nome);
        validarCor(cor);
        this.nome = nome.trim();
        this.descricao = descricao != null ? descricao.trim() : null;
        this.cor = cor.trim();
        this.assinatura = alterada;
    }

    private static void validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("O nome do tipo de ordem é obrigatório.");
        }
    }

    private static void validarCor(String cor) {
        if (cor == null || cor.isBlank()) {
            throw new RegraDeNegocioException("A cor do tipo de ordem é obrigatória.");
        }
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    /** Tipo de sistema que pode consumir produto acabado (e só produz semiacabado). */
    public boolean isConversao() { return conversao; }

    public String getCor() { return cor; }
    public Assinatura getAssinatura() { return assinatura; }
    public Instant getCriadoEm() { return assinatura.criadoEm(); }
    public Instant getAtualizadoEm() { return assinatura.alteradoEm(); }
}
