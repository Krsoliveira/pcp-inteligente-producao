package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.application.lote.ConsultarLotes;
import com.krsoliveira.pcp.application.lote.RastrearLote;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.infrastructure.web.dto.LoteResponse;
import com.krsoliveira.pcp.infrastructure.web.dto.RastreabilidadeLoteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Lotes nascem ao concluir ordens de produção ou nos itens das notas fiscais de entrada
 * (ver {@link NotaFiscalController}). Este controller consulta e rastreia os lotes.
 */
@RestController
@RequestMapping("/api/v1/lotes")
@Tag(name = "Lotes", description = "Consulta e rastreabilidade de lotes de produção e de compra")
public class LoteController {

    private final ConsultarLotes consultarLotes;
    private final RastrearLote rastrearLote;

    public LoteController(ConsultarLotes consultarLotes, RastrearLote rastrearLote) {
        this.consultarLotes = consultarLotes;
        this.rastrearLote = rastrearLote;
    }

    @GetMapping
    @Operation(summary = "Listar lotes",
            description = "Retorna todos os lotes. Filtros: ordemProducaoId (lote gerado pela ordem); "
                    + "materialId (todos os lotes do material, do mais novo ao mais antigo); "
                    + "materialId com disponiveis=true (lotes que podem ser alocados a um consumo: "
                    + "disponíveis, com saldo e dentro da validade, em ordem FEFO).")
    public ResponseEntity<List<LoteResponse>> listar(
            @RequestParam(required = false) UUID ordemProducaoId,
            @RequestParam(required = false) UUID materialId,
            @RequestParam(defaultValue = "false") boolean disponiveis) {
        List<Lote> lotes;
        if (ordemProducaoId != null) {
            lotes = consultarLotes.listarPorOrdem(ordemProducaoId);
        } else if (materialId != null && disponiveis) {
            lotes = consultarLotes.listarDisponiveisParaConsumo(materialId, LocalDate.now());
        } else if (materialId != null) {
            lotes = consultarLotes.listarPorMaterial(materialId);
        } else {
            lotes = consultarLotes.listarTodos();
        }
        return ResponseEntity.ok(lotes.stream().map(LoteResponse::de).toList());
    }

    @GetMapping("/{id}/rastreabilidade")
    @Operation(summary = "Genealogia do lote",
            description = "Origens: de quais lotes saiu cada material consumido para produzir o lote. "
                    + "Destinos: em quais ordens o lote foi usado e quais lotes elas geraram.")
    public ResponseEntity<RastreabilidadeLoteResponse> rastrear(@PathVariable UUID id) {
        return ResponseEntity.ok(RastreabilidadeLoteResponse.de(rastrearLote.executar(id)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar lote por ID")
    public ResponseEntity<LoteResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(LoteResponse.de(consultarLotes.porId(id)));
    }
}
