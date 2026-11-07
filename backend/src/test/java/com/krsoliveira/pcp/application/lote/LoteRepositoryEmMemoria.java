package com.krsoliveira.pcp.application.lote;

import com.krsoliveira.pcp.domain.lote.FiltroLotes;
import com.krsoliveira.pcp.domain.comum.PedidoDePagina;
import com.krsoliveira.pcp.domain.auditoria.Pagina;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.LoteRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.krsoliveira.pcp.domain.lote.StatusLote;
import java.util.Objects;
import java.util.Comparator;
import java.util.Collection;

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
    public Optional<Lote> buscarPorIdParaAtualizar(UUID id) {
        return buscarPorId(id);
    }

    @Override
    public List<Lote> buscarPorIds(Collection<UUID> ids) {
        return ids.stream().map(dados::get).filter(Objects::nonNull).toList();
    }

    @Override
    public List<Lote> listarPorOrdens(Collection<UUID> ordemProducaoIds) {
        return dados.values().stream()
                .filter(l -> l.getOrdemProducaoId() != null && ordemProducaoIds.contains(l.getOrdemProducaoId()))
                .toList();
    }

    @Override
    public List<Lote> listarDisponiveisPorMaterial(UUID materialId) {
        return dados.values().stream()
                .filter(l -> materialId.equals(l.getMaterialId()) && l.getStatus() == StatusLote.DISPONIVEL
                        && l.getSaldo().signum() > 0)
                .sorted(Comparator.comparing(Lote::getDataValidade).thenComparing(Lote::getDataFabricacao))
                .toList();
    }

    @Override
    public long contarPorStatus(StatusLote status) {
        return dados.values().stream().filter(l -> l.getStatus() == status).count();
    }

    @Override
    public Pagina<Lote> pesquisar(FiltroLotes filtro, PedidoDePagina pedido) {
        List<Lote> filtrados = listarTodos().stream()
                .filter(l -> filtro.status() == null || l.getStatus() == filtro.status())
                .filter(l -> filtro.materialId() == null || filtro.materialId().equals(l.getMaterialId()))
                .filter(l -> filtro.busca() == null || l.getNumeroLote().toLowerCase().contains(filtro.busca().toLowerCase()))
                .toList();
        int de = (int) Math.min(pedido.deslocamento(), filtrados.size());
        int ate = Math.min(de + pedido.tamanho(), filtrados.size());
        return new Pagina<>(filtrados.subList(de, ate), pedido.pagina(), pedido.tamanho(), filtrados.size());
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
    public List<Lote> listarPorMaterial(UUID materialId) {
        return dados.values().stream().filter(l -> materialId.equals(l.getMaterialId())).toList();
    }

    @Override
    public List<Lote> listarPorNotasFiscais(Collection<UUID> notaFiscalIds) {
        return dados.values().stream()
                .filter(l -> l.getNotaFiscalId() != null && notaFiscalIds.contains(l.getNotaFiscalId()))
                .toList();
    }

    @Override
    public boolean existeLote(UUID materialId, String numeroLote, String fornecedor) {
        return dados.values().stream().anyMatch(l -> materialId.equals(l.getMaterialId())
                && numeroLote.equals(l.getNumeroLote())
                && Objects.equals(fornecedor == null ? null : fornecedor.toLowerCase(),
                        l.getFornecedor() == null ? null : l.getFornecedor().toLowerCase()));
    }

    @Override
    public int proximoSequencialProducao(LocalDate data) {
        String prefixo = Lote.prefixoLoteProducao(data);
        return (int) dados.values().stream()
                .filter(l -> l.getOrdemProducaoId() != null && l.getNumeroLote().startsWith(prefixo))
                .count() + 1;
    }
}
