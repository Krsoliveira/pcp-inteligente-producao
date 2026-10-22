package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.application.estoque.ConsultarEstoque;
import com.krsoliveira.pcp.infrastructure.web.dto.EstoqueResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Estoque por material (ADR-0012): saldo calculado a partir dos lotes. */
@RestController
@RequestMapping("/api/v1/estoque")
@Tag(name = "Estoque", description = "Posição de estoque por material, a partir dos saldos dos lotes")
public class EstoqueController {

    private final ConsultarEstoque consultarEstoque;

    public EstoqueController(ConsultarEstoque consultarEstoque) {
        this.consultarEstoque = consultarEstoque;
    }

    @GetMapping
    @Operation(summary = "Posição de estoque",
            description = "Todos os materiais com saldo disponível (lotes disponíveis e dentro da validade), "
                    + "saldo indisponível (bloqueado ou vencido), próximo vencimento e última entrada.")
    public ResponseEntity<List<EstoqueResponse>> listar() {
        return ResponseEntity.ok(consultarEstoque.executar(LocalDate.now()).stream()
                .map(EstoqueResponse::de).toList());
    }
}
