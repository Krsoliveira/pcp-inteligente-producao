package com.krsoliveira.pcp.application.notafiscal;

import com.krsoliveira.pcp.domain.auditoria.Pagina;
import com.krsoliveira.pcp.domain.comum.PedidoDePagina;
import com.krsoliveira.pcp.domain.notafiscal.FiltroNotasFiscais;
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

    /** Pesquisa por número e fornecedor (a busca pelo lote de um item é coberta no teste de integração). */
    @Override
    public Pagina<NotaFiscalEntrada> pesquisar(FiltroNotasFiscais filtro, PedidoDePagina pedido) {
        List<NotaFiscalEntrada> filtradas = dados.values().stream()
                .filter(n -> filtro.de() == null || !n.getDataRecebimento().isBefore(filtro.de()))
                .filter(n -> filtro.ate() == null || !n.getDataRecebimento().isAfter(filtro.ate()))
                .filter(n -> filtro.busca() == null
                        || n.getNumero().toLowerCase().contains(filtro.busca().toLowerCase())
                        || n.getFornecedor().toLowerCase().contains(filtro.busca().toLowerCase()))
                .sorted(Comparator.comparing(NotaFiscalEntrada::getDataRecebimento)
                        .thenComparing(NotaFiscalEntrada::getRegistradaEm).reversed())
                .toList();
        int de = (int) Math.min(pedido.deslocamento(), filtradas.size());
        int ate = Math.min(de + pedido.tamanho(), filtradas.size());
        return new Pagina<>(filtradas.subList(de, ate), pedido.pagina(), pedido.tamanho(), filtradas.size());
    }

    public List<NotaFiscalEntrada> listarTodas() {
        return new ArrayList<>(dados.values());
    }
}
