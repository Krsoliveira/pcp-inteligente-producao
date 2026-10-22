package com.krsoliveira.pcp.application.material;

import com.krsoliveira.pcp.domain.material.CodigoMaterial;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;
import com.krsoliveira.pcp.domain.material.TipoMaterial;

import com.krsoliveira.pcp.domain.auditoria.Pagina;
import com.krsoliveira.pcp.domain.comum.PedidoDePagina;
import com.krsoliveira.pcp.domain.material.FiltroMateriais;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação em memória do repositório de materiais — usada nos testes de use case
 * para evitar dependência de Spring ou banco de dados.
 */
public class MaterialRepositoryEmMemoria implements MaterialRepository {

    private final List<Material> materiais = new ArrayList<>();

    @Override
    public void salvar(Material material) {
        materiais.removeIf(m -> m.getId().equals(material.getId()));
        materiais.add(material);
    }

    @Override
    public Optional<Material> buscarPorId(UUID id) {
        return materiais.stream().filter(m -> m.getId().equals(id)).findFirst();
    }

    @Override
    public Optional<Material> buscarPorCodigo(String codigo) {
        return materiais.stream().filter(m -> m.getCodigo().equals(codigo)).findFirst();
    }

    @Override
    public List<Material> listarTodos() {
        return List.copyOf(materiais);
    }

    @Override
    public String proximoCodigo(TipoMaterial tipo) {
        return materiais.stream()
                .map(Material::getCodigo)
                .filter(codigo -> codigo.startsWith(tipo.prefixoCodigo()))
                .max(String::compareTo)
                .map(ultimo -> CodigoMaterial.seguinte(ultimo, tipo))
                .orElseGet(() -> CodigoMaterial.primeiro(tipo));
    }

    @Override
    public Pagina<Material> pesquisar(FiltroMateriais filtro, PedidoDePagina pedido) {
        List<Material> filtrados = listarTodos().stream()
                .filter(m -> filtro.tipo() == null || m.getTipo() == filtro.tipo())
                .filter(m -> filtro.busca() == null
                        || m.getDescricao().toLowerCase().contains(filtro.busca().toLowerCase())
                        || (filtro.digitosDoCodigo() != null && m.getCodigo().contains(filtro.digitosDoCodigo())))
                .sorted(Comparator.comparing(Material::getCriadoEm).thenComparing(Material::getCodigo).reversed())
                .toList();
        int de = (int) Math.min(pedido.deslocamento(), filtrados.size());
        int ate = Math.min(de + pedido.tamanho(), filtrados.size());
        return new Pagina<>(filtrados.subList(de, ate), pedido.pagina(), pedido.tamanho(), filtrados.size());
    }
}
