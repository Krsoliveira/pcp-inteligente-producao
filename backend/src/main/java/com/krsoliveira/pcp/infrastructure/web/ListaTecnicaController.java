package com.krsoliveira.pcp.infrastructure.web;

import org.springframework.security.access.prepost.PreAuthorize;
import com.krsoliveira.pcp.application.lista.AtivarListaTecnica;
import com.krsoliveira.pcp.application.lista.CadastrarListaTecnica;
import com.krsoliveira.pcp.application.lista.CompararListasTecnicas;
import com.krsoliveira.pcp.application.lista.ConsultarListaTecnica;
import com.krsoliveira.pcp.application.lista.RetirarListaTecnicaDeUso;
import com.krsoliveira.pcp.infrastructure.web.dto.CadastrarListaTecnicaRequest;
import com.krsoliveira.pcp.infrastructure.web.dto.ComparacaoDeVersoesResponse;
import com.krsoliveira.pcp.infrastructure.web.dto.ListaTecnicaResponse;
import com.krsoliveira.pcp.domain.comum.PedidoDePagina;
import com.krsoliveira.pcp.domain.lista.FiltroListasTecnicas;
import com.krsoliveira.pcp.domain.lista.StatusListaTecnica;
import com.krsoliveira.pcp.infrastructure.web.dto.PaginaResponse;
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
    private final RetirarListaTecnicaDeUso retirarListaTecnicaDeUso;
    private final CompararListasTecnicas compararListasTecnicas;
    private final ConsultarListaTecnica consultarListaTecnica;

    public ListaTecnicaController(CadastrarListaTecnica cadastrarListaTecnica,
                                  AtivarListaTecnica ativarListaTecnica,
                                  RetirarListaTecnicaDeUso retirarListaTecnicaDeUso,
                                  CompararListasTecnicas compararListasTecnicas,
                                  ConsultarListaTecnica consultarListaTecnica) {
        this.cadastrarListaTecnica = cadastrarListaTecnica;
        this.ativarListaTecnica = ativarListaTecnica;
        this.retirarListaTecnicaDeUso = retirarListaTecnicaDeUso;
        this.compararListasTecnicas = compararListasTecnicas;
        this.consultarListaTecnica = consultarListaTecnica;
    }

    @PreAuthorize("hasAuthority('LISTA_CADASTRAR')")
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

    @PreAuthorize("hasAuthority('LISTA_ATIVAR')")
    @PatchMapping("/{id}/ativar")
    @Operation(summary = "Ativar lista técnica",
               description = "Ativa uma versão em revisão. Não afeta as outras versões do material: várias "
                       + "podem estar ativas ao mesmo tempo (ADR-0019).")
    public ResponseEntity<Void> ativar(@PathVariable UUID id) {
        ativarListaTecnica.executar(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('LISTA_RETIRAR_DE_USO')")
    @PatchMapping("/{id}/retirar-de-uso")
    @Operation(summary = "Retirar lista técnica de uso",
               description = "Torna OBSOLETA uma versão ATIVA. É decisão do usuário: ativar outra versão não "
                       + "retira nenhuma.")
    public ResponseEntity<Void> retirarDeUso(@PathVariable UUID id) {
        retirarListaTecnicaDeUso.executar(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyAuthority('LISTA_VISUALIZAR_ATIVAS', 'LISTA_VISUALIZAR_TODAS')")
    @GetMapping("/pesquisa")
    @Operation(summary = "Pesquisa listas técnicas com filtro e paginação",
            description = "Filtra no servidor por material e status, das criadas mais recentemente para as mais antigas. "
                    + "Tamanho padrão 25, máximo 100.")
    public PaginaResponse<ListaTecnicaResponse> pesquisar(
            @RequestParam(required = false) UUID materialId,
            @RequestParam(required = false) StatusListaTecnica status,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho) {
        var resultado = consultarListaTecnica.pesquisar(
                new FiltroListasTecnicas(materialId, status), PedidoDePagina.de(pagina, tamanho));
        return PaginaResponse.de(resultado, ListaTecnicaResponse::de);
    }

    @PreAuthorize("hasAuthority('LISTA_COMPARAR') and hasAnyAuthority('LISTA_VISUALIZAR_ATIVAS', 'LISTA_VISUALIZAR_TODAS')")
    @GetMapping("/comparar")
    @Operation(summary = "Compara duas versões da mesma lista técnica",
            description = "Devolve os componentes adicionados, removidos, alterados (quantidade ou unidade) e "
                    + "iguais entre as versões a e b. Versões de listas diferentes ou a mesma versão duas vezes "
                    + "devolvem 422. Acesso liberado a qualquer usuário autenticado por ora (ADR-0019).")
    public ComparacaoDeVersoesResponse comparar(@RequestParam UUID a, @RequestParam UUID b) {
        return ComparacaoDeVersoesResponse.de(compararListasTecnicas.executar(a, b));
    }

    @PreAuthorize("hasAnyAuthority('LISTA_VISUALIZAR_ATIVAS', 'LISTA_VISUALIZAR_TODAS')")
    @GetMapping("/{id}")
    @Operation(summary = "Buscar lista técnica por ID")
    public ResponseEntity<ListaTecnicaResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ListaTecnicaResponse.de(consultarListaTecnica.buscarPorId(id)));
    }

    @PreAuthorize("hasAnyAuthority('LISTA_VISUALIZAR_ATIVAS', 'LISTA_VISUALIZAR_TODAS')")
    @GetMapping
    @Operation(summary = "Listar listas técnicas",
               description = "Sem materialId, retorna todas as listas; com materialId, "
                       + "todas as versões de lista técnica daquele material.")
    public ResponseEntity<List<ListaTecnicaResponse>> listar(
            @RequestParam(required = false) UUID materialId) {
        var listas = materialId == null
                ? consultarListaTecnica.listarTodas()
                : consultarListaTecnica.listarPorMaterial(materialId);
        List<ListaTecnicaResponse> resposta = listas.stream().map(ListaTecnicaResponse::de).toList();
        return ResponseEntity.ok(resposta);
    }
}
