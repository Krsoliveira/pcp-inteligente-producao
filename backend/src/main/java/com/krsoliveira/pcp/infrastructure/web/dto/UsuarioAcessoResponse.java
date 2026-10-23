package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.application.acesso.ConsultarAcessos;
import com.krsoliveira.pcp.domain.usuario.Usuario;

import java.time.Instant;
import java.util.UUID;

/** Usuário como a tela de acessos o mostra: nunca inclui a senha nem o hash. */
public record UsuarioAcessoResponse(UUID id, String nome, String email, UUID perfilId, String perfilNome,
                                    Instant criadoEm) {

    public static UsuarioAcessoResponse de(ConsultarAcessos.UsuarioComPerfil r) {
        Usuario u = r.usuario();
        return new UsuarioAcessoResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfilId(),
                r.perfil() == null ? null : r.perfil().getNome(), u.getCriadoEm());
    }
}
