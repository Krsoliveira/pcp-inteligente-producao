package com.krsoliveira.pcp.application.estoque;

import com.krsoliveira.pcp.domain.estoque.ConsultaEstoque;
import com.krsoliveira.pcp.domain.estoque.PosicaoEstoque;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;

import com.krsoliveira.pcp.domain.auditoria.Pagina;
import com.krsoliveira.pcp.domain.comum.PedidoDePagina;
import com.krsoliveira.pcp.domain.estoque.FiltroEstoque;
import com.krsoliveira.pcp.domain.material.TipoMaterial;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caso de uso: estoque por material — todos os materiais, inclusive os sem saldo, com a
 * posição calculada a partir dos lotes. Ordenado por código.
 */
public class ConsultarEstoque {

    private final ConsultaEstoque consultaEstoque;
    private final MaterialRepository materialRepository;

    public ConsultarEstoque(ConsultaEstoque consultaEstoque, MaterialRepository materialRepository) {
        this.consultaEstoque = consultaEstoque;
        this.materialRepository = materialRepository;
    }

    public record ItemEstoque(Material material, PosicaoEstoque posicao) {}

    public List<ItemEstoque> executar(LocalDate hoje) {
        Map<UUID, PosicaoEstoque> posicoes = consultaEstoque.posicoes(hoje).stream()
                .collect(Collectors.toMap(PosicaoEstoque::materialId, Function.identity()));
        return materialRepository.listarTodos().stream()
                .sorted(Comparator.comparing(Material::getCodigo))
                .map(m -> new ItemEstoque(m, posicoes.getOrDefault(m.getId(), PosicaoEstoque.vazia(m.getId()))))
                .toList();
    }

    /** Materiais com lote disponível vencendo em até este número de dias entram no alerta. */
    public static final int DIAS_ALERTA_DE_VENCIMENTO = 30;

    /**
     * Uma página do estoque que atende ao filtro, dos materiais com entrada mais recente para os
     * mais antigos (quem nunca teve entrada vem por último, por código). A posição de cada material é
     * um agregado dos lotes, então o filtro e a página são aplicados sobre as posições já calculadas.
     */
    public Pagina<ItemEstoque> pesquisar(FiltroEstoque filtro, PedidoDePagina pedido, LocalDate hoje) {
        List<ItemEstoque> filtrados = executar(hoje).stream()
                .filter(item -> filtro.material().aceita(item.material()))
                .filter(item -> !filtro.somenteComSaldo() || item.posicao().saldoDisponivel().signum() > 0)
                .sorted(Comparator
                        .comparing((ItemEstoque item) -> item.posicao().ultimaEntrada(),
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(item -> item.material().getCodigo()))
                .toList();
        int de = (int) Math.min(pedido.deslocamento(), filtrados.size());
        int ate = Math.min(de + pedido.tamanho(), filtrados.size());
        return new Pagina<>(filtrados.subList(de, ate), pedido.pagina(), pedido.tamanho(), filtrados.size());
    }

    public ResumoEstoque resumo(LocalDate hoje) {
        List<ItemEstoque> itens = executar(hoje);
        LocalDate limite = hoje.plusDays(DIAS_ALERTA_DE_VENCIMENTO);
        long vencendo = itens.stream()
                .filter(i -> i.posicao().proximoVencimento() != null && !i.posicao().proximoVencimento().isAfter(limite))
                .count();
        long semSaldo = itens.stream()
                .filter(i -> i.material().getTipo() == TipoMaterial.MATERIA_PRIMA
                        && i.posicao().saldoDisponivel().signum() <= 0)
                .count();
        return new ResumoEstoque(itens.size(), vencendo, semSaldo);
    }
}
