package com.krsoliveira.pcp.application.auditoria;

import com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.EventoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.FiltroEventos;
import com.krsoliveira.pcp.domain.auditoria.Pagina;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultarTrilhaDeAuditoriaTest {

    private static final Instant DIA_1 = Instant.parse("2026-09-01T12:00:00Z");
    private static final Instant DIA_2 = Instant.parse("2026-09-02T12:00:00Z");
    private static final Instant DIA_3 = Instant.parse("2026-09-03T12:00:00Z");
    private static final UUID LOTE = UUID.randomUUID();

    private final TrilhaDeAuditoriaEmMemoria trilha = new TrilhaDeAuditoriaEmMemoria();
    private final ConsultarTrilhaDeAuditoria casoDeUso = new ConsultarTrilhaDeAuditoria(trilha);

    @BeforeEach
    void preparar() {
        trilha.registrar(List.of(
                evento(TipoEntidade.LOTE, LOTE, AcaoAuditoria.ENTRADA_REGISTRADA, "ana@pcp", DIA_1),
                evento(TipoEntidade.LOTE, LOTE, AcaoAuditoria.STATUS_ALTERADO, "bruno@pcp", DIA_2),
                evento(TipoEntidade.MATERIAL, UUID.randomUUID(), AcaoAuditoria.CRIADO, "ana@pcp", DIA_3)));
    }

    @Test
    @DisplayName("histórico de um registro, do mais recente para o mais antigo")
    void historicoDoRegistro() {
        Pagina<EventoAuditoria> pagina = casoDeUso.executar(
                FiltroEventos.daEntidade(TipoEntidade.LOTE, LOTE), 0, 50);

        assertThat(pagina.total()).isEqualTo(2);
        assertThat(pagina.itens()).extracting(EventoAuditoria::getAcao)
                .containsExactly(AcaoAuditoria.STATUS_ALTERADO, AcaoAuditoria.ENTRADA_REGISTRADA);
    }

    @Test
    @DisplayName("filtra por usuário (sem diferenciar maiúsculas) e por período")
    void filtraPorUsuarioEPeriodo() {
        var porUsuario = casoDeUso.executar(
                new FiltroEventos(null, null, "ANA@pcp", null, null, null), 0, 50);
        var porPeriodo = casoDeUso.executar(
                new FiltroEventos(null, null, null, null, DIA_2, DIA_3), 0, 50);

        assertThat(porUsuario.total()).isEqualTo(2);
        assertThat(porPeriodo.itens()).singleElement()
                .satisfies(e -> assertThat(e.getOcorridoEm()).isEqualTo(DIA_2));
    }

    @Test
    @DisplayName("pagina o resultado")
    void pagina() {
        var segunda = casoDeUso.executar(new FiltroEventos(null, null, null, null, null, null), 1, 2);

        assertThat(segunda.itens()).hasSize(1);
        assertThat(segunda.total()).isEqualTo(3);
        assertThat(segunda.totalPaginas()).isEqualTo(2);
    }

    @Test
    @DisplayName("valida página, tamanho e período")
    void validacoes() {
        var semFiltro = new FiltroEventos(null, null, null, null, null, null);

        assertThatThrownBy(() -> casoDeUso.executar(semFiltro, -1, 10))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> casoDeUso.executar(semFiltro, 0, ConsultarTrilhaDeAuditoria.TAMANHO_MAXIMO + 1))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> casoDeUso.executar(
                new FiltroEventos(null, null, null, null, DIA_3, DIA_1), 0, 10))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("período");
    }

    private static EventoAuditoria evento(TipoEntidade tipo, UUID id, AcaoAuditoria acao,
                                          String usuario, Instant quando) {
        return EventoAuditoria.registrar(tipo, id, "REF", acao, usuario, quando, Map.of());
    }
}
