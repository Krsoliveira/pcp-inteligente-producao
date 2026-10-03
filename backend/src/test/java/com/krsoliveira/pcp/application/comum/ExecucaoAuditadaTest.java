package com.krsoliveira.pcp.application.comum;

import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecucaoAuditadaTest {

    private final TrilhaDeAuditoriaEmMemoria trilha = new TrilhaDeAuditoriaEmMemoria();

    @Test
    @DisplayName("grava os eventos registrados com o usuário logado e devolve o resultado")
    void gravaEventos() {
        UUID id = UUID.randomUUID();

        String resultado = trilha.execucao().executar(ctx -> {
            ctx.registrar(TipoEntidade.MATERIAL, id, "MAT-1", AcaoAuditoria.CRIADO,
                    Detalhes.com("codigo", "MAT-1").e("ignorado", null));
            return "ok";
        });

        assertThat(resultado).isEqualTo("ok");
        assertThat(trilha.eventos()).singleElement().satisfies(evento -> {
            assertThat(evento.getUsuario()).isEqualTo(TrilhaDeAuditoriaEmMemoria.USUARIO_TESTE);
            assertThat(evento.getDetalhes()).containsOnlyKeys("codigo");
            assertThat(evento.getOcorridoEm()).isNotNull();
        });
    }

    @Test
    @DisplayName("falha na operação: nenhum evento é gravado")
    void falhaNaoGravaEventos() {
        assertThatThrownBy(() -> trilha.execucao().executar(ctx -> {
            ctx.registrar(TipoEntidade.MATERIAL, UUID.randomUUID(), "MAT-1", AcaoAuditoria.CRIADO,
                    Detalhes.vazio());
            throw new IllegalStateException("falhou");
        })).hasMessage("falhou");

        assertThat(trilha.eventos()).isEmpty();
    }

    @Test
    @DisplayName("operação e eventos rodam dentro da mesma transação")
    void mesmaTransacao() {
        List<String> registro = new ArrayList<>();
        Transacao transacao = new Transacao() {
            @Override
            public <T> T executar(Supplier<T> bloco) {
                registro.add("inicio");
                T resultado = bloco.get();
                registro.add("commit");
                return resultado;
            }
        };
        var trilhaQueAnota = new TrilhaDeAuditoriaEmMemoria() {
            @Override
            public void registrar(List<com.krsoliveira.pcp.domain.auditoria.EventoAuditoria> eventos) {
                registro.add("eventos");
                super.registrar(eventos);
            }
        };
        var execucao = new ExecucaoAuditada(() -> "ana@pcp", transacao, trilhaQueAnota);

        execucao.executar(ctx -> {
            registro.add("operacao");
            ctx.registrar(TipoEntidade.LOTE, UUID.randomUUID(), "L-1", AcaoAuditoria.ENTRADA_REGISTRADA, null);
            return null;
        });

        assertThat(registro).containsExactly("inicio", "operacao", "eventos", "commit");
    }

    @Test
    @DisplayName("executarComo usa o responsável explícito em vez do usuário logado")
    void responsavelExplicito() {
        trilha.execucao().executarComo("sistema:teste", ctx -> {
            ctx.registrar(TipoEntidade.USUARIO, UUID.randomUUID(), "x@pcp", AcaoAuditoria.USUARIO_REGISTRADO,
                    Detalhes.vazio());
            return null;
        });

        assertThat(trilha.eventos()).singleElement()
                .satisfies(e -> assertThat(e.getUsuario()).isEqualTo("sistema:teste"));
    }

    @Test
    @DisplayName("Detalhes registra mudança de → para com tipos simples")
    void detalhesMudanca() {
        var mapa = Detalhes.vazio()
                .mudanca("status", AcaoAuditoria.CRIADO, AcaoAuditoria.ALTERADO)
                .e("quantidade", new java.math.BigDecimal("10.5000"))
                .mapa();

        assertThat(mapa).containsEntry("status", java.util.Map.of("de", "CRIADO", "para", "ALTERADO"))
                .containsEntry("quantidade", "10.5");
    }
}
