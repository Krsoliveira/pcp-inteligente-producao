package com.krsoliveira.pcp.application.auth;

import com.krsoliveira.pcp.domain.usuario.CodificadorDeSenha;
import com.krsoliveira.pcp.domain.usuario.Perfil;
import com.krsoliveira.pcp.domain.usuario.Usuario;
import com.krsoliveira.pcp.domain.usuario.UsuarioRepository;

/**
 * Caso de uso: registra um novo usuário na plataforma.
 * <p>
 * Sequência:
 * 1. Verifica unicidade do e-mail (falha rápido com 409 se já existe).
 * 2. Codifica a senha via port {@link CodificadorDeSenha} (BCrypt na infra).
 * 3. Cria a entidade de domínio e persiste via port {@link UsuarioRepository}.
 * <p>
 * Nenhum detalhe de framework aqui.
 */
public class RegistrarUsuario {

    private final UsuarioRepository usuarioRepository;
    private final CodificadorDeSenha codificadorDeSenha;

    public RegistrarUsuario(UsuarioRepository usuarioRepository,
                            CodificadorDeSenha codificadorDeSenha) {
        this.usuarioRepository = usuarioRepository;
        this.codificadorDeSenha = codificadorDeSenha;
    }

    public Usuario executar(String nome, String email, String senhaPlana, Perfil perfil) {
        String emailNormalizado = email.trim().toLowerCase();
        if (usuarioRepository.porEmail(emailNormalizado).isPresent()) {
            throw new EmailJaUtilizadoException(email);
        }
        String senhaHash = codificadorDeSenha.codificar(senhaPlana);
        Usuario usuario = Usuario.criar(nome, emailNormalizado, senhaHash, perfil);
        return usuarioRepository.salvar(usuario);
    }
}
