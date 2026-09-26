package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.application.ordem.AtualizarTipoOrdem;
import com.krsoliveira.pcp.application.ordem.CadastrarTipoOrdem;
import com.krsoliveira.pcp.application.ordem.ConsultarTiposOrdem;
import com.krsoliveira.pcp.infrastructure.web.dto.AtualizarTipoOrdemRequest;
import com.krsoliveira.pcp.infrastructure.web.dto.CadastrarTipoOrdemRequest;
import com.krsoliveira.pcp.infrastructure.web.dto.TipoOrdemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Categorias de ordem de produção definidas pelo usuário.
 * Exemplos: Produção Normal, Manutenção, Revenda, Retrabalho.
 */
@RestController
@RequestMapping("/api/v1/tipos-ordem")
@Tag(name = "Tipos de Ordem", description = "Categorias de ordem de produção definidas pelo usuário")
public class TipoOrdemController {

    private final CadastrarTipoOrdem cadastrarTipoOrdem;
    private final ConsultarTiposOrdem consultarTiposOrdem;
    private final AtualizarTipoOrdem atualizarTipoOrdem;

    public TipoOrdemController(CadastrarTipoOrdem cadastrarTipoOrdem,
                               ConsultarTiposOrdem consultarTiposOrdem,
                               AtualizarTipoOrdem atualizarTipoOrdem) {
        this.cadastrarTipoOrdem = cadastrarTipoOrdem;
        this.consultarTiposOrdem = consultarTiposOrdem;
        this.atualizarTipoOrdem = atualizarTipoOrdem;
    }

    @PostMapping
    @Operation(summary = "Cadastrar tipo de ordem",
            description = "Cria uma nova categoria de ordem (ex.: Produção Normal, Manutenção, Revenda).")
    public ResponseEntity<TipoOrdemResponse> cadastrar(
            @RequestBody @Valid CadastrarTipoOrdemRequest request) {
        var tipo = cadastrarTipoOrdem.executar(request.paraComando());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(tipo.getId()).toUri();
        return ResponseEntity.created(location).body(TipoOrdemResponse.de(tipo));
    }

    @GetMapping
    @Operation(summary = "Listar tipos de ordem")
    public ResponseEntity<List<TipoOrdemResponse>> listar() {
        return ResponseEntity.ok(
                consultarTiposOrdem.listarTodos().stream()
                        .map(TipoOrdemResponse::de)
                        .toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar tipo de ordem por ID")
    public ResponseEntity<TipoOrdemResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(TipoOrdemResponse.de(consultarTiposOrdem.porId(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar tipo de ordem")
    public ResponseEntity<TipoOrdemResponse> atualizar(@PathVariable UUID id,
                                                        @RequestBody @Valid AtualizarTipoOrdemRequest request) {
        return ResponseEntity.ok(
                TipoOrdemResponse.de(atualizarTipoOrdem.executar(request.paraComando(id))));
    }
}
