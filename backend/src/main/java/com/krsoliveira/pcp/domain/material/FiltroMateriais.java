package com.krsoliveira.pcp.domain.material;

/**
 * Critérios de pesquisa de materiais. Ambos são opcionais.
 *
 * @param busca trecho da descrição ou do código (aceita o código formatado, como 103.000.001)
 * @param tipo  somente materiais deste tipo
 */
public record FiltroMateriais(String busca, TipoMaterial tipo) {

    static final int TAMANHO_MAXIMO_DA_BUSCA = 100;

    public FiltroMateriais {
        if (busca == null || busca.isBlank()) {
            busca = null;
        } else {
            busca = busca.trim();
            if (busca.length() > TAMANHO_MAXIMO_DA_BUSCA) {
                busca = busca.substring(0, TAMANHO_MAXIMO_DA_BUSCA);
            }
        }
    }

    public static FiltroMateriais semFiltro() {
        return new FiltroMateriais(null, null);
    }

    /** O material atende ao filtro? (mesma regra da pesquisa no banco, para listas montadas em memória) */
    public boolean aceita(Material material) {
        if (tipo != null && material.getTipo() != tipo) {
            return false;
        }
        if (busca == null) {
            return true;
        }
        String digitos = digitosDoCodigo();
        return material.getDescricao().toLowerCase().contains(busca.toLowerCase())
                || (digitos != null && material.getCodigo().contains(digitos));
    }

    /**
     * A busca sem pontos e espaços, quando ela é só dígitos ("103.000.001" -> "103000001");
     * senão {@code null}. É o trecho a procurar no código do material.
     */
    public String digitosDoCodigo() {
        if (busca == null) {
            return null;
        }
        String digitos = busca.replace(".", "").replace(" ", "");
        return !digitos.isEmpty() && digitos.chars().allMatch(Character::isDigit) ? digitos : null;
    }
}
