package com.krsoliveira.pcp.domain.comum;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PedidoDePaginaTest {

    @Test
    @DisplayName("sem informar nada, vale a primeira página com o tamanho padrão")
    void padrao() {
        var pedido = PedidoDePagina.de(null, null);

        assertThat(pedido.pagina()).isZero();
        assertThat(pedido.tamanho()).isEqualTo(PedidoDePagina.TAMANHO_PADRAO);
    }

    @Test
    @DisplayName("página negativa vira 0 e tamanho menor que 1 vira o padrão")
    void corrigeValoresInvalidos() {
        var pedido = PedidoDePagina.de(-3, 0);

        assertThat(pedido.pagina()).isZero();
        assertThat(pedido.tamanho()).isEqualTo(PedidoDePagina.TAMANHO_PADRAO);
    }

    @Test
    @DisplayName("tamanho acima do teto é limitado, para ninguém pedir todos os registros")
    void limitaOTamanho() {
        assertThat(PedidoDePagina.de(2, 100_000).tamanho()).isEqualTo(PedidoDePagina.TAMANHO_MAXIMO);
    }

    @Test
    @DisplayName("valores válidos são mantidos e o deslocamento é página x tamanho")
    void deslocamento() {
        var pedido = PedidoDePagina.de(3, 10);

        assertThat(pedido.pagina()).isEqualTo(3);
        assertThat(pedido.tamanho()).isEqualTo(10);
        assertThat(pedido.deslocamento()).isEqualTo(30);
    }

    @Test
    @DisplayName("o deslocamento não estoura int com páginas muito grandes")
    void deslocamentoGrande() {
        assertThat(PedidoDePagina.de(Integer.MAX_VALUE, 100).deslocamento()).isEqualTo(214_748_364_700L);
    }
}
