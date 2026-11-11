package com.krsoliveira.pcp.domain.comum;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Padrão da unidade de medida: maiúsculas e sem espaços nas pontas ("kg", "Kg" e " KG " são a mesma unidade, {@code KG}).
 * Aceita letras, números, {@code %}, {@code ²}, {@code ³}, {@code /}, {@code .} e {@code -}, até 10 caracteres (o
 * tamanho da coluna). Todo ponto de entrada da unidade passa por aqui (ADR-0037).
 */
public final class UnidadeDeMedida {

    private static final int TAMANHO_MAXIMO = 10;
    private static final Pattern FORMATO = Pattern.compile("[\\p{L}\\p{N}%²³/.\\-]{1," + TAMANHO_MAXIMO + "}");
    private static final String MENSAGEM = "A unidade de medida é obrigatória, com até " + TAMANHO_MAXIMO
            + " caracteres (letras, números, %, ², ³, / ou .), sem espaços.";

    private UnidadeDeMedida() {
    }

    public static String normalizar(String unidade) {
        if (unidade == null) {
            throw new RegraDeNegocioException(MENSAGEM);
        }
        String normalizada = unidade.strip().toUpperCase(Locale.ROOT);
        if (!FORMATO.matcher(normalizada).matches()) {
            throw new RegraDeNegocioException(MENSAGEM);
        }
        return normalizada;
    }
}
