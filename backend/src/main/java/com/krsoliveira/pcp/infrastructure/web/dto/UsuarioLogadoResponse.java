package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.application.acesso.ConsultarUsuarioLogado;
import com.krsoliveira.pcp.domain.acesso.Permissao;

import java.util.List;
import java.util.UUID;

/** Quem está logado e o que pode fazer; a tela usa para mostrar ou esconder telas e botões. */
public record UsuarioLogadoResponse(String nome, String email, PerfilResumo perfil, List<Permissao> permissoes,
                                    boolean deveTrocarSenha) {

    public record PerfilResumo(UUID id, String nome) {}

    public static UsuarioLogadoResponse de(ConsultarUsuarioLogado.Resultado r) {
        return new UsuarioLogadoResponse(r.usuario().getNome(), r.usuario().getEmail(),
                new PerfilResumo(r.perfil().getId(), r.perfil().getNome()),
                r.permissoes().stream().sorted().toList(), r.deveTrocarSenha());
    }
}
