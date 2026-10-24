package com.krsoliveira.pcp.domain.usuario;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;

import java.nio.charset.StandardCharsets;

/**
 * Regra de senha (ADR-0021): no mínimo 8 caracteres e no máximo 72 bytes, que é o limite do BCrypt (acima
 * disso ele recusa ou ignora o resto). Sem exigência de complexidade.
 *
 * Classe de domínio PURA: sem anotações de framework.
 */
public final class PoliticaDeSenha {

    public static final int MINIMO_DE_CARACTERES = 8;
    public static final int MAXIMO_DE_BYTES = 72;

    private PoliticaDeSenha() {}

    public static void validar(String senha) {
        if (senha == null || senha.length() < MINIMO_DE_CARACTERES) {
            throw new RegraDeNegocioException(
                    "A senha deve ter no mínimo %d caracteres.".formatted(MINIMO_DE_CARACTERES));
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > MAXIMO_DE_BYTES) {
            throw new RegraDeNegocioException(
                    "A senha é longa demais: o limite é de %d bytes (cerca de %d caracteres simples)."
                            .formatted(MAXIMO_DE_BYTES, MAXIMO_DE_BYTES));
        }
    }
}
