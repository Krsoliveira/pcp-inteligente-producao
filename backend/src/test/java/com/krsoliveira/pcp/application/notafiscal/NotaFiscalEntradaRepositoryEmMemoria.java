package com.krsoliveira.pcp.application.notafiscal;

import com.krsoliveira.pcp.domain.notafiscal.NotaFiscalEntrada;
import com.krsoliveira.pcp.domain.notafiscal.NotaFiscalEntradaRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class NotaFiscalEntradaRepositoryEmMemoria implements NotaFiscalEntradaRepository {

    private final Map<UUID, NotaFiscalEntrada> dados = new LinkedHashMap<>();

    @Override
    public void salvar(NotaFiscalEntrada nota) {
        dados.put(nota.getId(), nota);
    }

    @Override
    public Optional<NotaFiscalEntrada> buscarPorId(UUID id) {
        return Optional.ofNullable(dados.get(id));
    }

    @Override
    public boolean existe(String fornecedor, String numero) {
        return dados.values().stream().anyMatch(n -> n.getFornecedor().equalsIgnoreCase(fornecedor)
                && n.getNumero().equals(numero));
    }

    @Override
    public List<NotaFiscalEntrada> listarPorRecebimento(LocalDate de, LocalDate ate) {
        return dados.values().stream()
                .filter(n -> de == null || !n.getDataRecebimento().isBefore(de))
                .filter(n -> ate == null || !n.getDataRecebimento().isAfter(ate))
                .sorted(Comparator.comparing(NotaFiscalEntrada::getDataRecebimento).reversed())
                .toList();
    }

    public List<NotaFiscalEntrada> listarTodas() {
        return new ArrayList<>(dados.values());
    }
}
