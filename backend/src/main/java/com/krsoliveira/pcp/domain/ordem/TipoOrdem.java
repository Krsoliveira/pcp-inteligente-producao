package com.krsoliveira.pcp.domain.ordem;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;

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
    private final Instant criadoEm;
    private Instant atualizadoEm;

    private TipoOrdem(UUID id, String nome, String descricao, String cor,
                      Instant criadoEm, Instant atualizadoEm) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.cor = cor;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    /**
     * Fábrica para um tipo de ordem NOVO.
     *
     * @param cor código hexadecimal para badge no frontend (ex.: "#1565c0")
     */
    public static TipoOrdem criar(String nome, String descricao, String cor) {
        validarNome(nome);
        validarCor(cor);
        Instant agora = Instant.now();
        return new TipoOrdem(UUID.randomUUID(), nome.trim(), descricao != null ? descricao.trim() : null,
                cor.trim(), agora, agora);
    }

    /**
     * Reconstrói um tipo de ordem EXISTENTE a partir do banco de dados.
     */
    public static TipoOrdem reconstituir(UUID id, String nome, String descricao, String cor,
                                         Instant criadoEm, Instant atualizadoEm) {
        return new TipoOrdem(id, nome, descricao, cor, criadoEm, atualizadoEm);
    }

    /**
     * Atualiza os dados editáveis do tipo de ordem.
     */
    public void atualizar(String nome, String descricao, String cor) {
        validarNome(nome);
        validarCor(cor);
        this.nome = nome.trim();
        this.descricao = descricao != null ? descricao.trim() : null;
        this.cor = cor.trim();
        this.atualizadoEm = Instant.now();
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
    public String getCor() { return cor; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
}
