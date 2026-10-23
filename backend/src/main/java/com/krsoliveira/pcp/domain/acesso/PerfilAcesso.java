package com.krsoliveira.pcp.domain.acesso;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.Assinatura;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Perfil de acesso (ADR-0020): um conjunto de {@link Permissao} com id, nome e descrição. O usuário
 * tem um perfil; as permissões dele são as do perfil.
 *
 * Há dois perfis de sistema, criados pela migração: o ADMINISTRADOR (todas as permissões, sempre, sem
 * edição nem exclusão) e o padrão do autocadastro (permissões editáveis, sem exclusão).
 *
 * Classe de domínio PURA: sem anotações de framework.
 */
public class PerfilAcesso {

    public static final int NOME_MAXIMO = 60;
    public static final int DESCRICAO_MAXIMA = 200;

    private final UUID id;
    private String nome;
    private String descricao;
    private final boolean administrador;
    private final boolean padraoDoAutocadastro;
    private final Set<Permissao> permissoes;
    private Assinatura assinatura;

    private PerfilAcesso(UUID id, String nome, String descricao, boolean administrador,
                         boolean padraoDoAutocadastro, Set<Permissao> permissoes, Assinatura assinatura) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.administrador = administrador;
        this.padraoDoAutocadastro = padraoDoAutocadastro;
        this.permissoes = permissoes;
        this.assinatura = assinatura;
    }

    /** Perfil novo, escolhido pelo gerente: nunca é de sistema. */
    public static PerfilAcesso criar(String nome, String descricao, Collection<Permissao> permissoes,
                                     String usuario) {
        return new PerfilAcesso(UUID.randomUUID(), nomeValido(nome), descricaoValida(descricao),
                false, false, permissoesValidas(permissoes), Assinatura.nova(usuario));
    }

    public static PerfilAcesso reconstituir(UUID id, String nome, String descricao, boolean administrador,
                                            boolean padraoDoAutocadastro, Collection<Permissao> permissoes,
                                            Assinatura assinatura) {
        Set<Permissao> conjunto = permissoes.isEmpty() ? EnumSet.noneOf(Permissao.class) : EnumSet.copyOf(permissoes);
        return new PerfilAcesso(id, nome, descricao, administrador, padraoDoAutocadastro, conjunto, assinatura);
    }

    /** O Administrador, perfil de sistema com todas as permissões, nunca muda. */
    public void exigirQuePodeSerAlterado() {
        if (administrador) {
            throw new RegraDeNegocioException("O perfil Administrador não pode ser alterado.");
        }
    }

    /** Troca nome, descrição e permissões. O perfil Administrador não muda. */
    public void redefinir(String novoNome, String novaDescricao, Collection<Permissao> novasPermissoes,
                          String usuario) {
        exigirQuePodeSerAlterado();
        String nomeValido = nomeValido(novoNome);
        String descricaoValida = descricaoValida(novaDescricao);
        Set<Permissao> permissoesValidas = permissoesValidas(novasPermissoes);
        Assinatura alterada = assinatura.alterada(usuario);
        this.nome = nomeValido;
        this.descricao = descricaoValida;
        this.permissoes.clear();
        this.permissoes.addAll(permissoesValidas);
        this.assinatura = alterada;
    }

    /** O Administrador sempre tem todas as permissões, inclusive as que forem criadas no futuro. */
    public Set<Permissao> permissoesEfetivas() {
        return administrador
                ? Collections.unmodifiableSet(EnumSet.allOf(Permissao.class))
                : Collections.unmodifiableSet(permissoes);
    }

    /** Este perfil pode ser excluído? Perfis de sistema não podem. */
    public boolean podeSerExcluido() {
        return !administrador && !padraoDoAutocadastro;
    }

    private static String nomeValido(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("O nome do perfil é obrigatório.");
        }
        String aparado = nome.trim();
        if (aparado.length() > NOME_MAXIMO) {
            throw new RegraDeNegocioException("O nome do perfil tem no máximo %d caracteres.".formatted(NOME_MAXIMO));
        }
        return aparado;
    }

    private static String descricaoValida(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            return "";
        }
        String aparada = descricao.trim();
        if (aparada.length() > DESCRICAO_MAXIMA) {
            throw new RegraDeNegocioException(
                    "A descrição do perfil tem no máximo %d caracteres.".formatted(DESCRICAO_MAXIMA));
        }
        return aparada;
    }

    private static Set<Permissao> permissoesValidas(Collection<Permissao> permissoes) {
        if (permissoes == null || permissoes.isEmpty()) {
            throw new RegraDeNegocioException("Marque ao menos uma permissão para o perfil.");
        }
        return EnumSet.copyOf(permissoes);
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public boolean isAdministrador() { return administrador; }
    public boolean isPadraoDoAutocadastro() { return padraoDoAutocadastro; }
    public Assinatura getAssinatura() { return assinatura; }
}
