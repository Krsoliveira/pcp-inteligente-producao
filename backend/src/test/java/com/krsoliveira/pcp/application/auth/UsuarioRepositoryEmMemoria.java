package com.krsoliveira.pcp.application.auth;

import com.krsoliveira.pcp.domain.usuario.Usuario;
import com.krsoliveira.pcp.domain.usuario.UsuarioRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

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

    @Override
    public Optional<Usuario> porId(UUID id) {
        return usuarios.stream().filter(u -> u.getId().equals(id)).findFirst();
    }

    @Override
    public List<Usuario> listarTodos() {
        return usuarios.stream().sorted(Comparator.comparing(u -> u.getNome().toLowerCase())).toList();
    }

    @Override
    public long contarPorPerfil(UUID perfilId) {
        return usuarios.stream().filter(u -> u.getPerfilId().equals(perfilId)).count();
    }

    public List<Usuario> todos() {
        return List.copyOf(usuarios);
    }
}