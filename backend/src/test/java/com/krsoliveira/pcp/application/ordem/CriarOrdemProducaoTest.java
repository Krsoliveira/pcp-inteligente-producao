package com.krsoliveira.pcp.application.ordem;

import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes do caso de uso com repositório em memória — sem Spring, sem banco.
 */
class CriarOrdemProducaoTest {

    private final OrdemProducaoRepositoryEmMemoria repositorio = new OrdemProducaoRepositoryEmMemoria();
    private final CriarOrdemProducao casoDeUso = new CriarOrdemProducao(repositorio);

    private CriarOrdemProducao.Comando comandoValido(String codigo) {
        return new CriarOrdemProducao.Comando(codigo, "Viga metálica 6m", "Usinagem CNC", 50,
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 20));
    }

    @Test
    @DisplayName("cria e persiste uma ordem válida")
    void criaEPersisteOrdem() {
        OrdemProducao ordem = casoDeUso.executar(comandoValido("OP-0001"));

        assertThat(ordem.getStatus()).isEqualTo(StatusOrdemProducao.PLANEJADA);
        assertThat(ordem.getCentroDeTrabalho()).isEqualTo("Usinagem CNC");
        assertThat(repositorio.buscarPorId(ordem.getId())).isPresent();
    }

    @Test
    @DisplayName("rejeita código duplicado com CodigoJaUtilizadoException")
    void rejeitaCodigoDuplicado() {
        casoDeUso.executar(comandoValido("OP-0001"));

        assertThatThrownBy(() -> casoDeUso.executar(comandoValido("OP-0001")))
                .isInstanceOf(CodigoJaUtilizadoException.class)
                .hasMessageContaining("OP-0001");
    }
}
