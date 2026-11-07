package com.krsoliveira.pcp.domain.acesso;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída: persistência dos perfis de acesso e das permissões de cada um. */
public interface PerfilAcessoRepository {

    PerfilAcesso salvar(PerfilAcesso perfil);

    Optional<PerfilAcesso> buscarPorId(UUID id);

    /** Bloqueia o perfil até o fim da transação: quem depende de quantos usuários ele tem conta sem concorrência. */
    Optional<PerfilAcesso> buscarPorIdParaAtualizar(UUID id);

    /**
     * Busca com trava compartilhada (SELECT ... FOR SHARE): o perfil pode ser atribuído a vários usuários ao mesmo
     * tempo, mas ninguém o exclui antes de a atribuição ser gravada.
     */
    Optional<PerfilAcesso> buscarPorIdParaUso(UUID id);

    /** Procura pelo nome sem diferenciar maiúsculas e minúsculas. */
    Optional<PerfilAcesso> buscarPorNome(String nome);

    /** Por nome, do primeiro ao último em ordem alfabética. */
    List<PerfilAcesso> listarTodos();

    /** O perfil que o autocadastro público recebe. */
    Optional<PerfilAcesso> buscarPadraoDoAutocadastro();

    Optional<PerfilAcesso> buscarAdministrador();

    void excluir(UUID id);
}
