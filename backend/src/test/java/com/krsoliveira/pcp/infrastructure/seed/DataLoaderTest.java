package com.krsoliveira.pcp.infrastructure.seed;

import com.krsoliveira.pcp.application.ordem.OrdemProducaoRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa o DataLoader sem Spring e sem banco: usa o repositório em memória
 * e verifica que o CSV é parseado corretamente.
 */
class DataLoaderTest {

    private final OrdemProducaoRepositoryEmMemoria repositorio =
            new OrdemProducaoRepositoryEmMemoria();
    private final DataLoader loader = new DataLoader(repositorio);

    @Test
    @DisplayName("carrega todas as 105 ordens do CSV")
    void carregaTodasAsOrdens() throws Exception {
        loader.run();

        assertThat(repositorio.contarTodas()).isEqualTo(105);
    }

    @Test
    @DisplayName("preserva dados de domínio — primeira ordem do CSV")
    void preservaDadosDaDominio() throws Exception {
        loader.run();

        List<OrdemProducao> ordens = repositorio.listarTodas();
        OrdemProducao primeiraOrdem = ordens.stream()
                .filter(o -> o.getCodigo().equals("OP-2024-001"))
                .findFirst()
                .orElseThrow();

        assertThat(primeiraOrdem.getProduto()).isEqualTo("Carcaça Motor A200");
        assertThat(primeiraOrdem.getCentroDeTrabalho()).isEqualTo("Usinagem CNC");
        assertThat(primeiraOrdem.getQuantidade()).isEqualTo(250);
        assertThat(primeiraOrdem.getStatus()).isEqualTo(StatusOrdemProducao.CONCLUIDA);
    }

    @Test
    @DisplayName("distribui status corretamente — há ordens PLANEJADAS e EM_PRODUCAO")
    void distribuiStatusCorretamente() throws Exception {
        loader.run();

        List<OrdemProducao> ordens = repositorio.listarTodas();

        long planejadas   = ordens.stream().filter(o -> o.getStatus() == StatusOrdemProducao.PLANEJADA).count();
        long emProducao   = ordens.stream().filter(o -> o.getStatus() == StatusOrdemProducao.EM_PRODUCAO).count();
        long liberadas    = ordens.stream().filter(o -> o.getStatus() == StatusOrdemProducao.LIBERADA).count();
        long concluidas   = ordens.stream().filter(o -> o.getStatus() == StatusOrdemProducao.CONCLUIDA).count();
        long canceladas   = ordens.stream().filter(o -> o.getStatus() == StatusOrdemProducao.CANCELADA).count();

        assertThat(planejadas).isGreaterThan(0);
        assertThat(emProducao).isGreaterThan(0);
        assertThat(liberadas).isGreaterThan(0);
        assertThat(concluidas).isGreaterThan(0);
        assertThat(canceladas).isGreaterThan(0);
        assertThat(planejadas + emProducao + liberadas + concluidas + canceladas).isEqualTo(105);
    }

    @Test
    @DisplayName("é idempotente — segunda execução não duplica dados")
    void idempotente() throws Exception {
        loader.run();
        loader.run();

        assertThat(repositorio.contarTodas()).isEqualTo(105);
    }
}
