package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.application.lista.AtivarListaTecnica;
import com.krsoliveira.pcp.application.lista.CadastrarListaTecnica;
import com.krsoliveira.pcp.application.lista.ConsultarListaTecnica;
import com.krsoliveira.pcp.infrastructure.web.dto.CadastrarListaTecnicaRequest;
import com.krsoliveira.pcp.infrastructure.web.dto.ListaTecnicaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listas-tecnicas")
@Tag(name = "Listas Técnicas", description = "Gestão de BOM (Bill of Materials) versionada")
public class ListaTecnicaController {

    private final CadastrarListaTecnica cadastrarListaTecnica;
    private final AtivarListaTecnica ativarListaTecnica;
    private final ConsultarListaTecnica consultarListaTecnica;

    public ListaTecnicaController(CadastrarListaTecnica cadastrarListaTecnica,
                                  AtivarListaTecnica ativarListaTecnica,
                                  ConsultarListaTecnica consultarListaTecnica) {
        this.cadastrarListaTecnica = cadastrarListaTecnica;
        this.ativarListaTecnica = ativarListaTecnica;
        this.consultarListaTecnica = consultarListaTecnica;
    }

    @PostMapping
    @Operation(summary = "Cadastrar lista técnica",
               description = "Cria uma nova BOM em status EM_REVISAO para o material informado.")
    public ResponseEntity<Void> cadastrar(
            @RequestBody @Valid CadastrarListaTecnicaRequest request) {
        UUID id = cadastrarListaTecnica.executar(request.paraComando());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).build();
    }

    @PatchMapping("/{id}/ativar")
    @Operation(summary = "Ativar lista técnica",
               description = "Ativa uma lista em revisão. A versão ATIVA atual (se houver) é obsoletada.")
    public ResponseEntity<Void> ativar(@PathVariable UUID id) {
        ativarListaTecnica.executar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar lista técnica por ID")
    public ResponseEntity<ListaTecnicaResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ListaTecnicaResponse.de(consultarListaTecnica.buscarPorId(id)));
    }

    @GetMapping
    @Operation(summary = "Listar listas técnicas por material",
               description = "Retorna todas as versões de lista técnica de um material.")
    public ResponseEntity<List<ListaTecnicaResponse>> listarPorMaterial(
            @RequestParam UUID materialId) {
        List<ListaTecnicaResponse> resposta = consultarListaTecnica.listarPorMaterial(materialId)
                .stream().map(ListaTecnicaResponse::de).toList();
        return ResponseEntity.ok(resposta);
    }
}
