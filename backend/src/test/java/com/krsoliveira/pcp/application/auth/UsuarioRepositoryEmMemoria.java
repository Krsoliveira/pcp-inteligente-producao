package com.krsoliveira.pcp.application.auth;

import com.krsoliveira.pcp.domain.usuario.Usuario;
import com.krsoliveira.pcp.domain.usuario.UsuarioRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementação em memória do repositório de usuários — usada nos testes de use case
 * para evitar dependência de Spring ou banco de dados.
 */
public class UsuarioRepositoryEmMemoria implements UsuarioRepository {

    private final List<Usuario> usuarios = new ArrayList<>();

    @Override
    public Usuario salvar(Usuario usuario) {
        usuarios.removeIf(u -> u.getId().equals(usuario.getId()));
        usuarios.add(usuario);
        return usuario;
    }

    @Override
    public Optional<Usuario> porEmail(String email) {
        return usuarios.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    public List<Usuario> todos() {
        return List.copyOf(usuarios);
    }
}