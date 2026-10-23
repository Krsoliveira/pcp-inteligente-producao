package com.krsoliveira.pcp.domain.acesso;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída: persistência dos perfis de acesso e das permissões de cada um. */
public interface PerfilAcessoRepository {

    PerfilAcesso salvar(PerfilAcesso perfil);

    Optional<PerfilAcesso> buscarPorId(UUID id);

    /** Procura pelo nome sem diferenciar maiúsculas e minúsculas. */
    Optional<PerfilAcesso> buscarPorNome(String nome);

    /** Por nome, do primeiro ao último em ordem alfabética. */
    List<PerfilAcesso> listarTodos();

    /** O perfil que o autocadastro público recebe. */
    Optional<PerfilAcesso> buscarPadraoDoAutocadastro();

    Optional<PerfilAcesso> buscarAdministrador();

    void excluir(UUID id);
}
