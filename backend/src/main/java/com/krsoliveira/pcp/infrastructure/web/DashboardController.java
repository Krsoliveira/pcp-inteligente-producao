package com.krsoliveira.pcp.infrastructure.web;

import org.springframework.security.access.prepost.PreAuthorize;
import com.krsoliveira.pcp.application.dashboard.ConsultarAtrasosPorCentro;
import com.krsoliveira.pcp.application.dashboard.ConsultarEntregasPorMes;
import com.krsoliveira.pcp.application.dashboard.ConsultarResumoProducao;
import com.krsoliveira.pcp.infrastructure.web.dto.AtrasoDoCentroResponse;
import com.krsoliveira.pcp.infrastructure.web.dto.EntregasDoMesResponse;
import com.krsoliveira.pcp.infrastructure.web.dto.ResumoProducaoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** Totais do painel principal, calculados no banco. */
@RestController
@PreAuthorize("hasAuthority('DASHBOARD_VISUALIZAR')")
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Totais do painel principal")
public class DashboardController {

    private final ConsultarResumoProducao consultarResumoProducao;
    private final ConsultarEntregasPorMes consultarEntregasPorMes;
    private final ConsultarAtrasosPorCentro consultarAtrasosPorCentro;

    public DashboardController(ConsultarResumoProducao consultarResumoProducao,
                               ConsultarEntregasPorMes consultarEntregasPorMes,
                               ConsultarAtrasosPorCentro consultarAtrasosPorCentro) {
        this.consultarResumoProducao = consultarResumoProducao;
        this.consultarEntregasPorMes = consultarEntregasPorMes;
        this.consultarAtrasosPorCentro = consultarAtrasosPorCentro;
    }

    @GetMapping("/resumo")
    @Operation(summary = "Resumo da produção",
            description = "Total de ordens, ordens por status, ordens atrasadas e lotes disponíveis. "
                    + "Substitui baixar todas as ordens e lotes só para contá-los.")
    public ResumoProducaoResponse resumo() {
        return ResumoProducaoResponse.de(consultarResumoProducao.executar(LocalDate.now()));
    }

    @GetMapping("/entregas-por-mes")
    @Operation(summary = "Entregas por mês do fim planejado",
            description = "Para cada mês (do mais antigo ao atual, inclusive os sem ordens): concluídas no prazo, "
                    + "concluídas com atraso, abertas atrasadas e abertas no prazo. Canceladas ficam de fora. "
                    + "\"No prazo\" usa o instante em que o sistema registrou a conclusão, não a data de fabricação do lote. "
                    + "meses: 1 a 24 (padrão 12); fora disso é corrigido.")
    public List<EntregasDoMesResponse> entregasPorMes(@RequestParam(required = false) Integer meses) {
        return consultarEntregasPorMes.executar(LocalDate.now(), ZoneId.systemDefault(), meses).stream()
                .map(EntregasDoMesResponse::de)
                .toList();
    }

    @GetMapping("/atrasos-por-centro")
    @Operation(summary = "Ordens atrasadas por centro de trabalho",
            description = "Só centros com ordens abertas depois do fim planejado, do que tem mais atrasadas para o que "
                    + "tem menos, com há quantos dias a mais antiga está atrasada.")
    public List<AtrasoDoCentroResponse> atrasosPorCentro() {
        return consultarAtrasosPorCentro.executar(LocalDate.now()).stream()
                .map(AtrasoDoCentroResponse::de)
                .toList();
    }
}
