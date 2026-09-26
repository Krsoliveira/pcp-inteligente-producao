package com.krsoliveira.pcp.application.lote;

import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.LoteRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class LoteRepositoryEmMemoria implements LoteRepository {

    private final Map<UUID, Lote> dados = new HashMap<>();

    @Override
    public Lote salvar(Lote lote) {
        dados.put(lote.getId(), lote);
        return lote;
    }

    @Override
    public Optional<Lote> buscarPorId(UUID id) {
        return Optional.ofNullable(dados.get(id));
    }

    @Override
    public List<Lote> listarTodos() {
        return new ArrayList<>(dados.values());
    }

    @Override
    public List<Lote> listarPorOrdemProducao(UUID ordemProducaoId) {
        return dados.values().stream()
                .filter(l -> ordemProducaoId.equals(l.getOrdemProducaoId()))
                .toList();
    }

    @Override
    public Optional<Lote> buscarPorNumeroLote(String numeroLote) {
        return dados.values().stream()
                .filter(l -> l.getNumeroLote().equals(numeroLote))
                .findFirst();
    }

    @Override
    public boolean existeEntrada(UUID materialId, String fornecedor, String notaFiscal) {
        return dados.values().stream().anyMatch(l -> materialId.equals(l.getMaterialId())
                && fornecedor.equals(l.getFornecedor()) && notaFiscal.equals(l.getNotaFiscal()));
    }

    @Override
    public int proximoSequencial(UUID materialId, String prefixo) {
        long count = dados.values().stream()
                .filter(l -> l.getNumeroLote().startsWith(prefixo))
                .count();
        return (int) count + 1;
    }
}
