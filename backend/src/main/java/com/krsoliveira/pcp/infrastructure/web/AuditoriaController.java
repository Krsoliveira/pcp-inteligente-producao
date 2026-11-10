package com.krsoliveira.pcp.infrastructure.web;

import org.springframework.security.access.prepost.PreAuthorize;
import com.krsoliveira.pcp.application.auditoria.ConsultarTrilhaDeAuditoria;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.FiltroEventos;
import com.krsoliveira.pcp.domain.auditoria.OrdemDosEventos;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.infrastructure.web.dto.EventoAuditoriaResponse;
import com.krsoliveira.pcp.infrastructure.web.dto.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.time.Clock;

/**
 * Trilha de auditoria: quem fez o quê, em qual registro e quando (ADR-0011).
 * Somente leitura — eventos nunca são alterados nem apagados.
 */
@RestController
@PreAuthorize("hasAuthority('AUDITORIA_VISUALIZAR')")
@RequestMapping("/api/v1/auditoria")
@Tag(name = "Auditoria", description = "Consulta da trilha de auditoria imutável")
public class AuditoriaController {

    private final ConsultarTrilhaDeAuditoria consultarTrilha;
    private final ZoneId fusoHorario;

    public AuditoriaController(ConsultarTrilhaDeAuditoria consultarTrilha,
                               Clock relogio) {
        this.consultarTrilha = consultarTrilha;
        this.fusoHorario = relogio.getZone();
    }

    @GetMapping("/eventos")
    @Operation(summary = "Consultar eventos de auditoria",
            description = "Filtros opcionais por entidade, registro, usuário (e-mail exato, sem diferenciar "
                    + "maiúsculas), ação e período. O período é por dia, com as duas datas inclusivas. "
                    + "Ordenado do mais recente para o mais antigo, ou do mais antigo para o mais recente "
                    + "com ordem=ANTIGOS_PRIMEIRO.")
    public ResponseEntity<PaginaResponse<EventoAuditoriaResponse>> consultar(
            @RequestParam(required = false) TipoEntidade tipoEntidade,
            @RequestParam(required = false) UUID entidadeId,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) AcaoAuditoria acao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(defaultValue = "RECENTES_PRIMEIRO") OrdemPedida ordem,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho) {
        FiltroEventos filtro = new FiltroEventos(tipoEntidade, entidadeId,
                usuario == null || usuario.isBlank() ? null : usuario.trim(),
                acao, inicioDoDia(de), inicioDoDia(ate == null ? null : ate.plusDays(1)));
        return ResponseEntity.ok(PaginaResponse.de(
                consultarTrilha.executar(filtro, ordem.paraDominio(), pagina, tamanho), EventoAuditoriaResponse::de));
    }

    /** Valores aceitos em {@code ordem}; o nome do contrato público não depende do nome no domínio. */
    public enum OrdemPedida {
        RECENTES_PRIMEIRO(OrdemDosEventos.MAIS_RECENTES_PRIMEIRO),
        ANTIGOS_PRIMEIRO(OrdemDosEventos.MAIS_ANTIGOS_PRIMEIRO);

        private final OrdemDosEventos ordem;

        OrdemPedida(OrdemDosEventos ordem) {
            this.ordem = ordem;
        }

        OrdemDosEventos paraDominio() {
            return ordem;
        }
    }

    private Instant inicioDoDia(LocalDate data) {
        return data == null ? null : data.atStartOfDay(fusoHorario).toInstant();
    }
}
