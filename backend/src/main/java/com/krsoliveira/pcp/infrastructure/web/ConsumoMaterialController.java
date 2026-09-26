package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.application.consumo.ConsultarConsumoMaterial;
import com.krsoliveira.pcp.application.consumo.RegistrarConsumoMaterial;
import com.krsoliveira.pcp.infrastructure.web.dto.ConsumoMaterialResponse;
import com.krsoliveira.pcp.infrastructure.web.dto.RegistrarConsumoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Gerencia o consumo de materiais em ordens de produção.
 * Os consumos são projetados automaticamente a partir da lista técnica
 * ao criar a ordem e registrados pelo operador durante a execução.
 */
@RestController
@RequestMapping("/api/v1/ordens-producao/{ordemId}/consumos")
@Tag(name = "Consumo de Materiais", description = "Consulta e registro do consumo real de materiais por ordem")
public class ConsumoMaterialController {

    private final ConsultarConsumoMaterial consultarConsumoMaterial;
    private final RegistrarConsumoMaterial registrarConsumoMaterial;

    public ConsumoMaterialController(ConsultarConsumoMaterial consultarConsumoMaterial,
                                     RegistrarConsumoMaterial registrarConsumoMaterial) {
        this.consultarConsumoMaterial = consultarConsumoMaterial;
        this.registrarConsumoMaterial = registrarConsumoMaterial;
    }

    @GetMapping
    @Operation(summary = "Listar consumos de uma ordem",
            description = "Retorna todos os consumos de materiais projetados e registrados para a ordem.")
    public ResponseEntity<List<ConsumoMaterialResponse>> listar(@PathVariable UUID ordemId) {
        return ResponseEntity.ok(
                consultarConsumoMaterial.listarPorOrdem(ordemId).stream()
                        .map(ConsumoMaterialResponse::de)
                        .toList());
    }

    @PatchMapping("/{consumoId}")
    @Operation(summary = "Registrar consumo real",
            description = "Informa a quantidade efetivamente consumida. "
                    + "Se houver desvio em relação ao planejado, justificativa e responsável são obrigatórios.")
    public ResponseEntity<ConsumoMaterialResponse> registrar(@PathVariable UUID ordemId,
                                                              @PathVariable UUID consumoId,
                                                              @RequestBody @Valid RegistrarConsumoRequest request) {
        return ResponseEntity.ok(
                ConsumoMaterialResponse.de(
                        registrarConsumoMaterial.executar(request.paraComando(consumoId))));
    }
}
