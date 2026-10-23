package com.krsoliveira.pcp.infrastructure.web;

import org.springframework.security.access.prepost.PreAuthorize;
import com.krsoliveira.pcp.application.dashboard.ConsultarResumoProducao;
import com.krsoliveira.pcp.infrastructure.web.dto.ResumoProducaoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Totais do painel principal, calculados no banco. */
@RestController
@PreAuthorize("hasAuthority('DASHBOARD_VISUALIZAR')")
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Totais do painel principal")
public class DashboardController {

    private final ConsultarResumoProducao consultarResumoProducao;

    public DashboardController(ConsultarResumoProducao consultarResumoProducao) {
        this.consultarResumoProducao = consultarResumoProducao;
    }

    @GetMapping("/resumo")
    @Operation(summary = "Resumo da produção",
            description = "Total de ordens, ordens por status, ordens atrasadas e lotes disponíveis. "
                    + "Substitui baixar todas as ordens e lotes só para contá-los.")
    public ResumoProducaoResponse resumo() {
        return ResumoProducaoResponse.de(consultarResumoProducao.executar(LocalDate.now()));
    }
}
