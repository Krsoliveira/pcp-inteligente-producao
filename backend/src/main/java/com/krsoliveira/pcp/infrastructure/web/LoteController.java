package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.application.lote.ConsultarLotes;
import com.krsoliveira.pcp.application.lote.RastrearLote;
import com.krsoliveira.pcp.application.lote.RegistrarEntradaMaterial;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.infrastructure.web.dto.LoteResponse;
import com.krsoliveira.pcp.infrastructure.web.dto.RastreabilidadeLoteResponse;
import com.krsoliveira.pcp.infrastructure.web.dto.RegistrarEntradaMaterialRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Lotes nascem ao concluir ordens de produção ou na entrada de matéria-prima comprada.
 * Este controller registra entradas e permite consultar e rastrear os lotes.
 */
@RestController
@RequestMapping("/api/v1/lotes")
@Tag(name = "Lotes", description = "Consulta de lotes gerados a partir de ordens concluídas")
public class LoteController {

    private final ConsultarLotes consultarLotes;
    private final RegistrarEntradaMaterial registrarEntradaMaterial;
    private final RastrearLote rastrearLote;

    public LoteController(ConsultarLotes consultarLotes,
                          RegistrarEntradaMaterial registrarEntradaMaterial,
                          RastrearLote rastrearLote) {
        this.consultarLotes = consultarLotes;
        this.registrarEntradaMaterial = registrarEntradaMaterial;
        this.rastrearLote = rastrearLote;
    }

    @PostMapping("/entradas")
    @Operation(summary = "Registrar entrada de material",
            description = "Recebimento de matéria-prima comprada: gera um lote DISPONIVEL "
                    + "com fornecedor e nota fiscal. Somente materiais do tipo MATERIA_PRIMA.")
    public ResponseEntity<LoteResponse> registrarEntrada(
            @Valid @RequestBody RegistrarEntradaMaterialRequest request) {
        Lote lote = registrarEntradaMaterial.executar(new RegistrarEntradaMaterial.Comando(
                request.materialId(), request.fornecedor(), request.notaFiscal(),
                request.dataEmissaoNf(), request.dataRecebimento(), request.quantidade(), request.dataFabricacao(), request.dataValidade()));
        URI local = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/lotes/{id}").buildAndExpand(lote.getId()).toUri();
        return ResponseEntity.created(local).body(LoteResponse.de(lote));
    }

    @GetMapping
    @Operation(summary = "Listar lotes",
            description = "Retorna todos os lotes. Filtros: ordemProducaoId (lote gerado pela ordem) ou "
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
