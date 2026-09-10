package com.krsoliveira.pcp.infrastructure.seed;

import com.krsoliveira.pcp.application.ordem.OrdemProducaoRepositoryEmMemoria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa o DataLoader na Fase 5a.
 *
 * O DataLoader foi desativado porque o dataset CSV (ordens_producao.csv, com campo
 * produto como VARCHAR) é incompatível com o novo modelo de domínio que usa
 * material_id + lista_tecnica_id (ADR-0007). O teste verifica que a execução do
 * loader não carrega dados (comportamento esperado até o dataset sintético estar pronto).
 *
 * TODO (Fase 5b): expandir este teste quando o novo dataset sintético for implementado.
 */
class DataLoaderTest {

    private final OrdemProducaoRepositoryEmMemoria repositorio =
            new OrdemProducaoRepositoryEmMemoria();
    private final DataLoader loader = new DataLoader(repositorio);

    @Test
    @DisplayName("não carrega dados enquanto dataset sintético não está disponível (Fase 5a)")
    void naoCarregaDadosComDatasetAntigo() throws Exception {
        loader.run();

        assertThat(repositorio.contarTodas())
                .as("DataLoader desativado — aguardando dataset sintético (ADR-0007)")
                .isZero();
    }

    @Test
    @DisplayName("é idempotente — múltiplas execuções não lançam exceção")
    void idempotente() throws Exception {
        loader.run();
        loader.run();

        assertThat(repositorio.contarTodas()).isZero();
    }
}
