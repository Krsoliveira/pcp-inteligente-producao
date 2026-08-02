package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.application.ordem.AtualizarStatusOrdemProducao;
import com.krsoliveira.pcp.application.ordem.ConsultarOrdensProducao;
import com.krsoliveira.pcp.application.ordem.CriarOrdemProducao;
import com.krsoliveira.pcp.infrastructure.web.dto.AtualizarStatusRequest;
import com.krsoliveira.pcp.infrastructure.web.dto.CriarOrdemProducaoRequest;
import com.krsoliveira.pcp.infrastructure.web.dto.OrdemProducaoResponse;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Porta de ENTRADA HTTP: traduz requisições em chamadas aos casos de uso e
 * resultados em respostas JSON. Nenhuma regra de negócio vive aqui.
 */
@RestController
@RequestMapping("/api/v1/ordens-producao")
@Tag(name = "Ordens de Produção", description = "Criação e acompanhamento de ordens de produção")
public class OrdemProducaoController {

    private final CriarOrdemProducao criarOrdemProducao;
    private final ConsultarOrdensProducao consultarOrdensProducao;
    private final AtualizarStatusOrdemProducao atualizarStatusOrdemProducao;

    public OrdemProducaoController(CriarOrdemProducao criarOrdemProducao,
                                   ConsultarOrdensProducao consultarOrdensProducao,
                                   AtualizarStatusOrdemProducao atualizarStatusOrdemProducao) {
        this.criarOrdemProducao = criarOrdemProducao;
        this.consultarOrdensProducao = consultarOrdensProducao;
        this.atualizarStatusOrdemProducao = atualizarStatusOrdemProducao;
    }

    @PostMapping
    @Operation(summary = "Cria uma ordem de produção",
            description = "A ordem nasce com status PLANEJADA. O código deve ser único.")
    public ResponseEntity<OrdemProducaoResponse> criar(@Valid @RequestBody CriarOrdemProducaoRequest request,
                                                       UriComponentsBuilder uriBuilder) {
        var ordem = criarOrdemProducao.executar(request.paraComando());
        URI location = uriBuilder.path("/api/v1/ordens-producao/{id}").buildAndExpand(ordem.getId()).toUri();
        return ResponseEntity.created(location).body(OrdemProducaoResponse.de(ordem));
    }

    @GetMapping
    @Operation(summary = "Lista todas as ordens de produção")
    public List<OrdemProducaoResponse> listar() {
        return consultarOrdensProducao.listar().stream()
                .map(OrdemProducaoResponse::de)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma ordem de produção pelo id")
    public OrdemProducaoResponse porId(@PathVariable UUID id) {
        return OrdemProducaoResponse.de(consultarOrdensProducao.porId(id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Avança ou cancela o status de uma ordem",
            description = "Transições válidas: PLANEJADA→LIBERADA→EM_PRODUCAO→CONCLUIDA; "
                    + "CANCELADA é permitida a partir de qualquer estado não terminal.")
    public OrdemProducaoResponse atualizarStatus(@PathVariable UUID id,
                                                 @Valid @RequestBody AtualizarStatusRequest request) {
        return OrdemProducaoResponse.de(atualizarStatusOrdemProducao.executar(id, request.status()));
    }
}
