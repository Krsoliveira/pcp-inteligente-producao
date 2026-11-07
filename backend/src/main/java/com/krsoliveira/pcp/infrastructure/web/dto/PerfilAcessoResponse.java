package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.application.acesso.ConsultarAcessos;
import com.krsoliveira.pcp.domain.acesso.PerfilAcesso;
import com.krsoliveira.pcp.domain.acesso.Permissao;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PerfilAcessoResponse(
        UUID id,
        String nome,
        String descricao,
        boolean administrador,
        boolean padraoDoAutocadastro,
        List<Permissao> permissoes,
        long quantidadeDeUsuarios,
        Instant atualizadoEm,
        String atualizadoPor,
        // Devolvida na alteração para recusar a edição feita sobre dados antigos (ADR-0034).
        long versao
) {
    public static PerfilAcessoResponse de(PerfilAcesso perfil, long quantidadeDeUsuarios) {
        return new PerfilAcessoResponse(
                perfil.getId(), perfil.getNome(), perfil.getDescricao(), perfil.isAdministrador(),
                perfil.isPadraoDoAutocadastro(), perfil.permissoesEfetivas().stream().sorted().toList(),
                quantidadeDeUsuarios, perfil.getAssinatura().alteradoEm(), perfil.getAssinatura().alteradoPor(),
                perfil.getVersao());
    }

    public static PerfilAcessoResponse de(ConsultarAcessos.PerfilComUsuarios resultado) {
        return de(resultado.perfil(), resultado.quantidadeDeUsuarios());
    }
}
