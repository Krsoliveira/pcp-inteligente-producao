package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.application.relatorio.ConsultarConsumoDeOrdens;
import com.krsoliveira.pcp.infrastructure.web.dto.ConsumoDeOrdensResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Relatórios gerenciais (somente leitura). */
@RestController
@RequestMapping("/api/v1/relatorios")
@Tag(name = "Relatórios", description = "Relatórios gerenciais")
public class RelatorioController {

    private final ConsultarConsumoDeOrdens consultarConsumoDeOrdens;

    public RelatorioController(ConsultarConsumoDeOrdens consultarConsumoDeOrdens) {
        this.consultarConsumoDeOrdens = consultarConsumoDeOrdens;
    }

    @PreAuthorize("hasAuthority('RELATORIO_CONSUMO_VISUALIZAR')")
    @GetMapping("/consumo-de-ordens")
    @Operation(summary = "Consumo de ordens concluídas por componente",
            description = "Para as ordens concluídas com fim planejado entre de e ate (datas inclusivas, obrigatórias, "
                    + "no máximo 366 dias), uma linha por ordem e componente com o consumo total (planejado, consumido "
                    + "e desvio), das ordens que terminam primeiro para as que terminam depois. O detalhe por lote está "
                    + "na ordem. Passando de 5000 linhas devolve 422 pedindo para reduzir o período.")
    public ConsumoDeOrdensResponse consumoDeOrdens(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return ConsumoDeOrdensResponse.de(consultarConsumoDeOrdens.executar(de, ate));
    }
}
