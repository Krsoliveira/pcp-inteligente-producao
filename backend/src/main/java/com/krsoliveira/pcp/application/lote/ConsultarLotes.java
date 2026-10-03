package com.krsoliveira.pcp.application.lote;

import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.LoteRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: consultar lotes (de produção e de compra).
 */
public class ConsultarLotes {

    private final LoteRepository loteRepository;

    public ConsultarLotes(LoteRepository loteRepository) {
        this.loteRepository = loteRepository;
    }

    public List<Lote> listarTodos() {
        return loteRepository.listarTodos();
    }

    public Lote porId(UUID id) {
        return loteRepository.buscarPorId(id)
                .orElseThrow(() -> new LoteNaoEncontradoException(id));
    }

    public List<Lote> listarPorOrdem(UUID ordemProducaoId) {
        return loteRepository.listarPorOrdemProducao(ordemProducaoId);
    }

    /**
     * Lotes que podem ser alocados a um consumo do material: disponíveis, com saldo e
     * dentro da validade, do que vence primeiro para o que vence depois (FEFO).
     */
    public List<Lote> listarDisponiveisParaConsumo(UUID materialId, LocalDate hoje) {
        return loteRepository.listarDisponiveisPorMaterial(materialId).stream()
                .filter(l -> !l.getDataValidade().isBefore(hoje))
                .toList();
    }
}
