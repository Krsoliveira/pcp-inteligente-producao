package com.krsoliveira.pcp.infrastructure.web;

import org.springframework.security.access.prepost.PreAuthorize;
import com.krsoliveira.pcp.application.consumo.AdicionarConsumoMaterial;
import com.krsoliveira.pcp.application.consumo.ConsultarConsumoMaterial;
import com.krsoliveira.pcp.application.consumo.RegistrarConsumoMaterial;
import com.krsoliveira.pcp.infrastructure.web.dto.AdicionarConsumoRequest;
import com.krsoliveira.pcp.infrastructure.web.dto.ConsumoMaterialResponse;
import com.krsoliveira.pcp.infrastructure.web.dto.RegistrarConsumoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final AdicionarConsumoMaterial adicionarConsumoMaterial;

    public ConsumoMaterialController(ConsultarConsumoMaterial consultarConsumoMaterial,
                                     RegistrarConsumoMaterial registrarConsumoMaterial,
                                     AdicionarConsumoMaterial adicionarConsumoMaterial) {
        this.consultarConsumoMaterial = consultarConsumoMaterial;
        this.registrarConsumoMaterial = registrarConsumoMaterial;
        this.adicionarConsumoMaterial = adicionarConsumoMaterial;
    }

    @PreAuthorize("hasAuthority('CONSUMO_VISUALIZAR')")
    @GetMapping
    @Operation(summary = "Listar consumos de uma ordem",
            description = "Retorna todos os consumos de materiais projetados e registrados para a ordem.")
    public ResponseEntity<List<ConsumoMaterialResponse>> listar(@PathVariable UUID ordemId) {
        return ResponseEntity.ok(
                consultarConsumoMaterial.listarPorOrdem(ordemId).stream()
                        .map(ConsumoMaterialResponse::de)
                        .toList());
    }

    @PreAuthorize("hasAuthority('CONSUMO_ADICIONAR_ITEM')")
    @PostMapping
    @Operation(summary = "Adicionar item ao consumo",
            description = "Adiciona e já registra o consumo de um material que NÃO está na lista técnica (planejado 0). "
                    + "Só em ordem em produção. A justificativa é obrigatória (o desvio é tudo o que foi consumido) e "
                    + "alocacoes diz de quais lotes saiu. Produto acabado só em ordem do tipo Conversão. Se o material "
                    + "já está na ordem, inclua os lotes na linha dele (422).")
    public ResponseEntity<ConsumoMaterialResponse> adicionar(@PathVariable UUID ordemId,
                                                              @RequestBody @Valid AdicionarConsumoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ConsumoMaterialResponse.de(adicionarConsumoMaterial.executar(request.paraComando(ordemId))));
    }

    @PreAuthorize("hasAuthority('CONSUMO_REGISTRAR')")
    @PatchMapping("/{consumoId}")
    @Operation(summary = "Registrar consumo real",
            description = "Informa a quantidade efetivamente consumida. "
                    + "Se houver desvio em relação ao planejado, a justificativa é obrigatória. "
                    + "Informe em alocacoes de quais lotes saiu o material (a soma deve ser igual à "
                    + "quantidade consumida). O responsável é o usuário logado.")
    public ResponseEntity<ConsumoMaterialResponse> registrar(@PathVariable UUID ordemId,
                                                              @PathVariable UUID consumoId,
                                                              @RequestBody @Valid RegistrarConsumoRequest request) {
        return ResponseEntity.ok(
                ConsumoMaterialResponse.de(
                        registrarConsumoMaterial.executar(request.paraComando(consumoId))));
    }
}
