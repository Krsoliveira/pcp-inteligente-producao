package com.krsoliveira.pcp.application.ordem;

import com.krsoliveira.pcp.application.comum.EdicaoConcorrenteException;
import com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.ordem.TipoOrdem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Edição do tipo de ordem com a versão lida pela tela (ADR-0034). */
class AtualizarTipoOrdemTest {

    private static final String AZUL = "#1565c0";
    private static final String VERDE = "#2e7d32";

    private TipoOrdemRepositoryEmMemoria tipos;
    private TrilhaDeAuditoriaEmMemoria trilha;
    private AtualizarTipoOrdem atualizar;
    private TipoOrdem manutencao;

    @BeforeEach
    void setUp() {
        tipos = new TipoOrdemRepositoryEmMemoria();
        trilha = new TrilhaDeAuditoriaEmMemoria();
        atualizar = new AtualizarTipoOrdem(tipos, trilha.execucao());
        manutencao = tipos.salvar(TipoOrdem.criar("Manutenção", null, AZUL, "sistema"));
    }

    private TipoOrdem editar(String descricao, String cor, Long versao) {
        return atualizar.executar(new AtualizarTipoOrdem.Comando(manutencao.getId(), "Manutenção", descricao, cor, versao));
    }

    @Test
    @DisplayName("com a versão atual, altera, passa para a versão seguinte e registra ALTERADO")
    void versaoAtualAltera() {
        TipoOrdem salvo = editar("Corretiva", AZUL, 0L);

        assertThat(salvo.getDescricao()).isEqualTo("Corretiva");
        assertThat(salvo.getVersao()).isEqualTo(1);
        assertThat(trilha.eventos(AcaoAuditoria.ALTERADO)).hasSize(1);
    }

    @Test
    @DisplayName("com uma versão que já não é a atual, recusa com 409 e não altera nem registra nada")
    void versaoAntigaRecusada() {
        editar("Corretiva", AZUL, 0L);

        assertThatThrownBy(() -> editar(null, VERDE, 0L))
                .isInstanceOf(EdicaoConcorrenteException.class)
                .hasMessageContaining("\"Manutenção\" foi alterado por outra pessoa");

        TipoOrdem atual = tipos.buscarPorId(manutencao.getId()).orElseThrow();
        assertThat(atual.getDescricao()).isEqualTo("Corretiva");
        assertThat(atual.getCor()).isEqualTo(AZUL);
        assertThat(atual.getVersao()).isEqualTo(1);
        assertThat(trilha.eventos(AcaoAuditoria.ALTERADO)).hasSize(1);
    }

    @Test
    @DisplayName("sem versão informada (cliente que não a envia), altera como antes e ainda soma a versão")
    void semVersaoAlteraComoAntes() {
        editar("Corretiva", AZUL, null);

        TipoOrdem salvo = editar("Preventiva", VERDE, null);

        assertThat(salvo.getDescricao()).isEqualTo("Preventiva");
        assertThat(salvo.getVersao()).isEqualTo(2);
    }
}
