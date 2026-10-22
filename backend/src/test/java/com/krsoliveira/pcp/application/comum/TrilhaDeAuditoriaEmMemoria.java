package com.krsoliveira.pcp.application.comum;

import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.EventoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.FiltroEventos;
import com.krsoliveira.pcp.domain.auditoria.Pagina;
import com.krsoliveira.pcp.domain.auditoria.TrilhaDeAuditoria;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Trilha em memória para testes — mesma semântica de filtro do adaptador JPA
 * (usuário sem diferenciar maiúsculas, {@code de} inclusivo, {@code ate} exclusivo).
 */
public class TrilhaDeAuditoriaEmMemoria implements TrilhaDeAuditoria {

    public static final String USUARIO_TESTE = "teste@pcp";

    private final List<EventoAuditoria> eventos = new ArrayList<>();

    /** Execução auditada com usuário fixo, sem transação real, gravando nesta trilha. */
    public ExecucaoAuditada execucao() {
        return new ExecucaoAuditada(() -> USUARIO_TESTE, Transacao.direta(), this);
    }

    @Override
    public void registrar(List<EventoAuditoria> novos) {
        eventos.addAll(novos);
    }

    @Override
    public Pagina<EventoAuditoria> consultar(FiltroEventos f, int pagina, int tamanho) {
        List<EventoAuditoria> filtrados = eventos.stream()
                .filter(e -> f.tipoEntidade() == null || e.getTipoEntidade() == f.tipoEntidade())
                .filter(e -> f.entidadeId() == null || e.getEntidadeId().equals(f.entidadeId()))
                .filter(e -> f.usuario() == null || e.getUsuario().equalsIgnoreCase(f.usuario().trim()))
                .filter(e -> f.acao() == null || e.getAcao() == f.acao())
                .filter(e -> f.de() == null || !e.getOcorridoEm().isBefore(f.de()))
                .filter(e -> f.ate() == null || e.getOcorridoEm().isBefore(f.ate()))
                .sorted(Comparator.comparing(EventoAuditoria::getOcorridoEm).reversed())
                .toList();
        List<EventoAuditoria> fatia = filtrados.stream()
                .skip((long) pagina * tamanho).limit(tamanho).toList();
        return new Pagina<>(fatia, pagina, tamanho, filtrados.size());
    }

    public List<EventoAuditoria> eventos() {
        return List.copyOf(eventos);
    }

    public List<EventoAuditoria> eventos(AcaoAuditoria acao) {
        return eventos.stream().filter(e -> e.getAcao() == acao).toList();
    }
}
