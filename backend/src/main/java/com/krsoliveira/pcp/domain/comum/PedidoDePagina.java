package com.krsoliveira.pcp.domain.comum;

/**
 * Qual página de uma listagem se quer: número (a partir de 0) e quantidade de itens.
 *
 * Valores fora do aceitável são corrigidos em vez de recusados: página negativa vira 0,
 * tamanho ausente ou menor que 1 vira o padrão e tamanho acima do teto é limitado — assim
 * nenhuma consulta consegue pedir "todos os registros".
 */
public record PedidoDePagina(int pagina, int tamanho) {

    public static final int TAMANHO_PADRAO = 25;
    public static final int TAMANHO_MAXIMO = 100;

    public PedidoDePagina {
        if (pagina < 0) {
            pagina = 0;
        }
        if (tamanho < 1) {
            tamanho = TAMANHO_PADRAO;
        }
        if (tamanho > TAMANHO_MAXIMO) {
            tamanho = TAMANHO_MAXIMO;
        }
    }

    public static PedidoDePagina de(Integer pagina, Integer tamanho) {
        return new PedidoDePagina(pagina == null ? 0 : pagina, tamanho == null ? TAMANHO_PADRAO : tamanho);
    }

    /** Quantos itens pular para chegar ao início desta página. */
    public long deslocamento() {
        return (long) pagina * tamanho;
    }
}
