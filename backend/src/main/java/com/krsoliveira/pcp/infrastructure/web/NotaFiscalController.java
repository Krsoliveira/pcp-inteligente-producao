package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.application.notafiscal.ConsultarNotasFiscais;
import com.krsoliveira.pcp.application.notafiscal.RegistrarEntradaNotaFiscal;
import com.krsoliveira.pcp.infrastructure.web.dto.NotaFiscalResponse;
import com.krsoliveira.pcp.infrastructure.web.dto.RegistrarNotaFiscalRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
 * Entrada de notas fiscais de compra (ADR-0012): cada item da nota vira um lote de
 * matéria-prima e entra no estoque.
 */
@RestController
@RequestMapping("/api/v1/notas-fiscais")
@Tag(name = "Notas fiscais", description = "Entrada de notas fiscais de compra e seus itens (lotes)")
public class NotaFiscalController {

    private final RegistrarEntradaNotaFiscal registrarEntrada;
    private final ConsultarNotasFiscais consultarNotas;

    public NotaFiscalController(RegistrarEntradaNotaFiscal registrarEntrada,
                                ConsultarNotasFiscais consultarNotas) {
        this.registrarEntrada = registrarEntrada;
        this.consultarNotas = consultarNotas;
    }

    @PostMapping
    @Operation(summary = "Registrar entrada de nota fiscal",
            description = "Cabeçalho (fornecedor, número, emissão, recebimento) e itens: matéria-prima, "
                    + "quantidade, lote do fornecedor (até 20 caracteres), fabricação e validade. "
                    + "Cada item gera um lote DISPONIVEL ligado à nota.")
    public ResponseEntity<NotaFiscalResponse> registrar(@Valid @RequestBody RegistrarNotaFiscalRequest request) {
        RegistrarEntradaNotaFiscal.Resultado resultado = registrarEntrada.executar(request.paraComando());
        URI local = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/notas-fiscais/{id}").buildAndExpand(resultado.nota().getId()).toUri();
        return ResponseEntity.created(local).body(NotaFiscalResponse.de(resultado.nota(), resultado.lotes()));
    }

    @GetMapping
    @Operation(summary = "Listar notas fiscais de entrada",
            description = "Filtro opcional por período de recebimento (datas inclusivas). Mais recentes primeiro.")
    public ResponseEntity<List<NotaFiscalResponse>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return ResponseEntity.ok(consultarNotas.listar(de, ate).stream()
                .map(n -> NotaFiscalResponse.de(n.nota(), n.itens())).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar nota fiscal com seus itens")
    public ResponseEntity<NotaFiscalResponse> buscar(@PathVariable UUID id) {
        ConsultarNotasFiscais.NotaComItens nota = consultarNotas.porId(id);
        return ResponseEntity.ok(NotaFiscalResponse.de(nota.nota(), nota.itens()));
    }
}
