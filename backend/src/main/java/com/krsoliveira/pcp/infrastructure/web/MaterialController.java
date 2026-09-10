package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.application.material.CadastrarMaterial;
import com.krsoliveira.pcp.application.material.ConsultarMateriais;
import com.krsoliveira.pcp.infrastructure.web.dto.CadastrarMaterialRequest;
import com.krsoliveira.pcp.infrastructure.web.dto.MaterialResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/materiais")
@Tag(name = "Materiais", description = "Cadastro e consulta de materiais (PA, SA, MP)")
public class MaterialController {

    private final CadastrarMaterial cadastrarMaterial;
    private final ConsultarMateriais consultarMateriais;

    public MaterialController(CadastrarMaterial cadastrarMaterial,
                              ConsultarMateriais consultarMateriais) {
        this.cadastrarMaterial = cadastrarMaterial;
        this.consultarMateriais = consultarMateriais;
    }

    @PostMapping
    @Operation(summary = "Cadastrar material", description = "Registra um novo material (PA, SA ou MP) no sistema.")
    public ResponseEntity<Void> cadastrar(@RequestBody @Valid CadastrarMaterialRequest request) {
        UUID id = cadastrarMaterial.executar(request.paraComando());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).build();
    }

    @GetMapping
    @Operation(summary = "Listar materiais", description = "Retorna todos os materiais cadastrados.")
    public ResponseEntity<List<MaterialResponse>> listar() {
        List<MaterialResponse> resposta = consultarMateriais.listarTodos()
                .stream().map(MaterialResponse::de).toList();
        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar material por ID")
    public ResponseEntity<MaterialResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(MaterialResponse.de(consultarMateriais.buscarPorId(id)));
    }
}
