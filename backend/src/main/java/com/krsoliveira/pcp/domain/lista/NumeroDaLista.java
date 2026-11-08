package com.krsoliveira.pcp.domain.lista;

import java.util.Locale;

/**
 * Formato de exibição do número da lista técnica: {@code LT-} e ao menos 4 dígitos ({@code LT-0007}). Passando de
 * 9999 o número simplesmente ganha mais dígitos ({@code LT-10000}); nada é cortado.
 */
public final class NumeroDaLista {

    private static final String PREFIXO = "LT-";
    private static final String FORMATO = PREFIXO + "%04d";

    private NumeroDaLista() {
    }

    public static String formatar(long numero) {
        return String.format(Locale.ROOT, FORMATO, numero);
    }
}
