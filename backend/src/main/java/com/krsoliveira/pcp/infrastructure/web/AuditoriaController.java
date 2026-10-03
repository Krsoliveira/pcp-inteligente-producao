package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.application.auditoria.ConsultarTrilhaDeAuditoria;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.FiltroEventos;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.infrastructure.web.dto.EventoAuditoriaResponse;
import com.krsoliveira.pcp.infrastructure.web.dto.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Trilha de auditoria: quem fez o quê, em qual registro e quando (ADR-0011).
 * Somente leitura — eventos nunca são alterados nem apagados.
 */
@RestController
@RequestMapping("/api/v1/auditoria")
@Tag(name = "Auditoria", description = "Consulta da trilha de auditoria imutável")
public class AuditoriaController {

    private final ConsultarTrilhaDeAuditoria consultarTrilha;
    private final ZoneId fusoHorario;

    public AuditoriaController(ConsultarTrilhaDeAuditoria consultarTrilha,
                               @Value("${pcp.fuso-horario:America/Sao_Paulo}") String fusoHorario) {
        this.consultarTrilha = consultarTrilha;
        this.fusoHorario = ZoneId.of(fusoHorario);
    }

    @GetMapping("/eventos")
    @Operation(summary = "Consultar eventos de auditoria",
            description = "Filtros opcionais por entidade, registro, usuário (e-mail exato, sem diferenciar "
                    + "maiúsculas), ação e período. O período é por dia, com as duas datas inclusivas. "
                    + "Ordenado do mais recente para o mais antigo.")
    public ResponseEntity<PaginaResponse<EventoAuditoriaResponse>> consultar(
            @RequestParam(required = false) TipoEntidade tipoEntidade,
            @RequestParam(required = false) UUID entidadeId,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) AcaoAuditoria acao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho) {
        FiltroEventos filtro = new FiltroEventos(tipoEntidade, entidadeId,
                usuario == null || usuario.isBlank() ? null : usuario.trim(),
                acao, inicioDoDia(de), inicioDoDia(ate == null ? null : ate.plusDays(1)));
        return ResponseEntity.ok(PaginaResponse.de(
                consultarTrilha.executar(filtro, pagina, tamanho), EventoAuditoriaResponse::de));
    }

    private Instant inicioDoDia(LocalDate data) {
        return data == null ? null : data.atStartOfDay(fusoHorario).toInstant();
    }
}
