package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.application.lote.ConsultarLotes;
import com.krsoliveira.pcp.infrastructure.web.dto.LoteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Lotes são gerados automaticamente ao concluir ordens de produção.
 * Este endpoint permite consultar e rastrear os lotes existentes.
 */
@RestController
@RequestMapping("/api/v1/lotes")
@Tag(name = "Lotes", description = "Consulta de lotes gerados a partir de ordens concluídas")
public class LoteController {

    private final ConsultarLotes consultarLotes;

    public LoteController(ConsultarLotes consultarLotes) {
        this.consultarLotes = consultarLotes;
    }

    @GetMapping
    @Operation(summary = "Listar lotes",
            description = "Retorna todos os lotes. Use o parâmetro ordemProducaoId para filtrar por ordem.")
    public ResponseEntity<List<LoteResponse>> listar(
            @RequestParam(required = false) UUID ordemProducaoId) {
        List<LoteResponse> lotes = (ordemProducaoId != null)
                ? consultarLotes.listarPorOrdem(ordemProducaoId).stream()
                        .map(LoteResponse::de).toList()
                : consultarLotes.listarTodos().stream()
                        .map(LoteResponse::de).toList();
        return ResponseEntity.ok(lotes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar lote por ID")
    public ResponseEntity<LoteResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(LoteResponse.de(consultarLotes.porId(id)));
    }
}
