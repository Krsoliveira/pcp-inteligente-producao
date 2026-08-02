package com.krsoliveira.pcp.domain.usuario;

import java.util.Optional;

/**
 * Porta do domínio para persistência de usuários.
 * A implementação concreta usa JPA/PostgreSQL (na camada de infraestrutura).
 */
public interface UsuarioRepository {

    Usuario salvar(Usuario usuario);

    Optional<Usuario> porEmail(String email);
}
