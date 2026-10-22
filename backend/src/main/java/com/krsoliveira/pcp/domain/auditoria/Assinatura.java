package com.krsoliveira.pcp.domain.auditoria;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;

import java.time.Instant;

/**
 * Quem criou e quem alterou por último um registro, e quando.
 *
 * Value object imutável: cada alteração gera uma nova assinatura que preserva os dados
 * de criação. Não existe assinatura sem autor — é a garantia de que todo registro do
 * sistema tem um responsável.
 */
public record Assinatura(String criadoPor, Instant criadoEm, String alteradoPor, Instant alteradoEm) {

    public Assinatura {
        exigirUsuario(criadoPor);
        exigirUsuario(alteradoPor);
        if (criadoEm == null || alteradoEm == null) {
            throw new RegraDeNegocioException("As datas da assinatura são obrigatórias.");
        }
    }

    /** Assinatura de um registro recém-criado pelo usuário informado. */
    public static Assinatura nova(String usuario) {
        Instant agora = Instant.now();
        return new Assinatura(usuario, agora, usuario, agora);
    }

    /** Nova assinatura que registra a alteração feita pelo usuário, preservando a criação. */
    public Assinatura alterada(String usuario) {
        return new Assinatura(criadoPor, criadoEm, usuario, Instant.now());
    }

    private static void exigirUsuario(String usuario) {
        if (usuario == null || usuario.isBlank()) {
            throw new RegraDeNegocioException("Toda operação precisa de um usuário responsável.");
        }
    }
}
