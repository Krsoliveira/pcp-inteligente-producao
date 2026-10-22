package com.krsoliveira.pcp.application.lista;

import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnicaRepository;
import com.krsoliveira.pcp.domain.lista.StatusListaTecnica;

import java.util.ArrayList;
import java.util.HashMap;
import com.krsoliveira.pcp.domain.auditoria.Pagina;
import com.krsoliveira.pcp.domain.comum.PedidoDePagina;
import com.krsoliveira.pcp.domain.lista.FiltroListasTecnicas;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ListaTecnicaRepositoryEmMemoria implements ListaTecnicaRepository {

    private final Map<UUID, ListaTecnica> dados = new HashMap<>();

    @Override
    public void salvar(ListaTecnica lista) {
        dados.put(lista.getId(), lista);
    }

    @Override
    public Optional<ListaTecnica> buscarPorId(UUID id) {
        return Optional.ofNullable(dados.get(id));
    }

    @Override
    public List<ListaTecnica> listarTodas() {
        return List.copyOf(dados.values());
    }

    @Override
    public List<ListaTecnica> listarPorMaterial(UUID materialId) {
        return dados.values().stream()
                .filter(l -> materialId.equals(l.getMaterialId()))
                .toList();
    }

    @Override
    public Optional<ListaTecnica> buscarAtivaParaMaterial(UUID materialId) {
        return dados.values().stream()
                .filter(l -> materialId.equals(l.getMaterialId())
                        && l.getStatus() == StatusListaTecnica.ATIVA)
                .findFirst();
    }

    @Override
    public boolean existeVersaoParaMaterial(UUID materialId, String versao) {
        return dados.values().stream()
                .anyMatch(l -> materialId.equals(l.getMaterialId())
                        && l.getVersao().equals(versao));
    }

    @Override
    public Pagina<ListaTecnica> pesquisar(FiltroListasTecnicas filtro, PedidoDePagina pedido) {
        List<ListaTecnica> filtradas = listarTodas().stream()
                .filter(l -> filtro.materialId() == null || l.getMaterialId().equals(filtro.materialId()))
                .filter(l -> filtro.status() == null || l.getStatus() == filtro.status())
                .sorted(Comparator.comparing(ListaTecnica::getCriadaEm).thenComparing(ListaTecnica::getVersao).reversed())
                .toList();
        int de = (int) Math.min(pedido.deslocamento(), filtradas.size());
        int ate = Math.min(de + pedido.tamanho(), filtradas.size());
        return new Pagina<>(filtradas.subList(de, ate), pedido.pagina(), pedido.tamanho(), filtradas.size());
    }
}
