package com.krsoliveira.pcp.domain.material;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;

import java.util.regex.Pattern;

/**
 * Regras do código de material (ADR-0012): exatamente 9 dígitos numéricos, e os 3
 * primeiros identificam o tipo — 103 produto acabado, 105 semiacabado, 110 matéria-prima.
 * Exibido como {@code 103.000.001}; guardado só com os dígitos.
 */
public final class CodigoMaterial {

    private static final Pattern NOVE_DIGITOS = Pattern.compile("\\d{9}");
    private static final int MAXIMO_POR_FAIXA = 999_999;

    private CodigoMaterial() {
    }

    /** Primeiro código da faixa do tipo (ex.: 110000001). */
    public static String primeiro(TipoMaterial tipo) {
        return tipo.prefixoCodigo() + "000001";
    }

    /** Código seguinte ao informado, na mesma faixa. */
    public static String seguinte(String ultimo, TipoMaterial tipo) {
        validar(ultimo, tipo);
        int sequencial = Integer.parseInt(ultimo.substring(3)) + 1;
        if (sequencial > MAXIMO_POR_FAIXA) {
            throw new RegraDeNegocioException(
                    "A faixa de códigos %s.xxx.xxx está esgotada.".formatted(tipo.prefixoCodigo()));
        }
        return tipo.prefixoCodigo() + "%06d".formatted(sequencial);
    }

    /** Garante 9 dígitos e prefixo compatível com o tipo. */
    public static void validar(String codigo, TipoMaterial tipo) {
        if (codigo == null || !NOVE_DIGITOS.matcher(codigo).matches()) {
            throw new RegraDeNegocioException("O código do material deve ter exatamente 9 dígitos numéricos.");
        }
        if (tipo != null && !codigo.startsWith(tipo.prefixoCodigo())) {
            throw new RegraDeNegocioException("Código %s fora da faixa de %s (%s.xxx.xxx)."
                    .formatted(codigo, tipo, tipo.prefixoCodigo()));
        }
    }

    /** {@code 103000001} → {@code 103.000.001}. */
    public static String formatar(String codigo) {
        return codigo == null || codigo.length() != 9 ? codigo
                : codigo.substring(0, 3) + "." + codigo.substring(3, 6) + "." + codigo.substring(6);
    }
}
