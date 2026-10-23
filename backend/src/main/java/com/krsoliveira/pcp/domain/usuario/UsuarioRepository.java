package com.krsoliveira.pcp.domain.usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta do domínio para persistência de usuários.
 * A implementação concreta usa JPA/PostgreSQL (na camada de infraestrutura).
 */
public interface UsuarioRepository {

    Usuario salvar(Usuario usuario);

    Optional<Usuario> porEmail(String email);

    Optional<Usuario> porId(UUID id);

    /** Por nome, em ordem alfabética. */
    List<Usuario> listarTodos();

    /** Quantos usuários têm este perfil. */
    long contarPorPerfil(UUID perfilId);
}
