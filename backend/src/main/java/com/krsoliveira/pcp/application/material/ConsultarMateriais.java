package com.krsoliveira.pcp.application.material;

import com.krsoliveira.pcp.domain.auditoria.Pagina;
import com.krsoliveira.pcp.domain.comum.PedidoDePagina;
import com.krsoliveira.pcp.domain.material.FiltroMateriais;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caso de uso: consultar materiais (individual ou listagem).
 */
public class ConsultarMateriais {

    private final MaterialRepository materialRepository;

    public ConsultarMateriais(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    public Material buscarPorId(UUID id) {
        return materialRepository.buscarPorId(id)
                .orElseThrow(() -> new MaterialNaoEncontradoException(id));
    }

    /** Os materiais destes ids, por id; os que não existem ficam de fora. */
    public Map<UUID, Material> porIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return materialRepository.buscarPorIds(ids).stream()
                .collect(Collectors.toMap(Material::getId, Function.identity()));
    }

    public List<Material> listarTodos() {
        return materialRepository.listarTodos();
    }

    public Pagina<Material> pesquisar(FiltroMateriais filtro, PedidoDePagina pedido) {
        return materialRepository.pesquisar(filtro, pedido);
    }
}