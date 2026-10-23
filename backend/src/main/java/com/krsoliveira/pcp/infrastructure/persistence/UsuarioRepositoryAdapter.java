package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.usuario.Usuario;
import com.krsoliveira.pcp.domain.usuario.UsuarioRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador que implementa o port de domínio {@link UsuarioRepository}
 * usando Spring Data JPA. É a única classe de infraestrutura visível
 * para o domínio/aplicação no contexto de usuários.
 */
@Repository
public class UsuarioRepositoryAdapter implements UsuarioRepository {

    private final UsuarioSpringDataRepository springData;

    public UsuarioRepositoryAdapter(UsuarioSpringDataRepository springData) {
        this.springData = springData;
    }

    @Override
    public Usuario salvar(Usuario usuario) {
        return springData.save(UsuarioJpaEntity.de(usuario)).paraDominio();
    }

    @Override
    public Optional<Usuario> porEmail(String email) {
        return springData.findByEmail(email).map(UsuarioJpaEntity::paraDominio);
    }

    @Override
    public Optional<Usuario> porId(UUID id) {
        return springData.findById(id).map(UsuarioJpaEntity::paraDominio);
    }

    @Override
    public List<Usuario> listarTodos() {
        return springData.findAllByOrderByNomeAsc().stream().map(UsuarioJpaEntity::paraDominio).toList();
    }

    @Override
    public long contarPorPerfil(UUID perfilId) {
        return springData.countByPerfilId(perfilId);
    }
}
