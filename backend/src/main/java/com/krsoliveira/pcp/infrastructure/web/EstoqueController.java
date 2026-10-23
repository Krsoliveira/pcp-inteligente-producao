package com.krsoliveira.pcp.infrastructure.web;

import org.springframework.security.access.prepost.PreAuthorize;
import com.krsoliveira.pcp.application.estoque.ConsultarEstoque;
import com.krsoliveira.pcp.domain.comum.PedidoDePagina;
import com.krsoliveira.pcp.domain.estoque.FiltroEstoque;
import com.krsoliveira.pcp.domain.material.FiltroMateriais;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
import com.krsoliveira.pcp.infrastructure.web.dto.EstoqueResponse;
import com.krsoliveira.pcp.infrastructure.web.dto.PaginaResponse;
import com.krsoliveira.pcp.infrastructure.web.dto.ResumoEstoqueResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Estoque por material (ADR-0012): saldo calculado a partir dos lotes. */
@RestController
@PreAuthorize("hasAuthority('ESTOQUE_VISUALIZAR')")
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

    @GetMapping("/pesquisa")
    @Operation(summary = "Pesquisa o estoque com filtro e paginação",
            description = "Filtra por trecho do código (aceita 103.000.001) ou da descrição, por tipo e, opcionalmente, "
                    + "só materiais com saldo. Dos materiais com entrada mais recente para os mais antigos. "
                    + "Tamanho padrão 25, máximo 100.")
    public PaginaResponse<EstoqueResponse> pesquisar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) TipoMaterial tipo,
            @RequestParam(required = false) Boolean somenteComSaldo,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho) {
        var filtro = new FiltroEstoque(new FiltroMateriais(busca, tipo), Boolean.TRUE.equals(somenteComSaldo));
        var resultado = consultarEstoque.pesquisar(filtro, PedidoDePagina.de(pagina, tamanho), LocalDate.now());
        return PaginaResponse.de(resultado, EstoqueResponse::de);
    }

    @GetMapping("/resumo")
    @Operation(summary = "Alertas do estoque",
            description = "Materiais com lote vencendo nos próximos 30 dias e matérias-primas sem saldo disponível.")
    public ResumoEstoqueResponse resumo() {
        return ResumoEstoqueResponse.de(consultarEstoque.resumo(LocalDate.now()));
    }
}
