package com.krsoliveira.pcp.infrastructure.persistence;

/** Apoio às pesquisas com filtro opcional. */
final class PesquisaJpa {

    private PesquisaJpa() {
    }

    /**
     * Monta o padrão "contém" para LIKE com escape {@code '\\'}: %, _ e \\ digitados pela pessoa
     * são texto comum, não curingas.
     */
    static String contem(String texto) {
        String escapado = texto.toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escapado + "%";
    }

    static final char ESCAPE = '\\';
}
