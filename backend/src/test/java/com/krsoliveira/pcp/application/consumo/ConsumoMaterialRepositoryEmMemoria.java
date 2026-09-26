package com.krsoliveira.pcp.application.consumo;

import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterialRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ConsumoMaterialRepositoryEmMemoria implements ConsumoMaterialRepository {

    private final Map<UUID, ConsumoMaterial> dados = new HashMap<>();

    @Override
    public ConsumoMaterial salvar(ConsumoMaterial consumo) {
        dados.put(consumo.getId(), consumo);
        return consumo;
    }

    @Override
    public List<ConsumoMaterial> salvarTodos(List<ConsumoMaterial> consumos) {
        consumos.forEach(c -> dados.put(c.getId(), c));
        return consumos;
    }

    @Override
    public Optional<ConsumoMaterial> buscarPorId(UUID id) {
        return Optional.ofNullable(dados.get(id));
    }

    @Override
    public List<ConsumoMaterial> listarPorOrdemProducao(UUID ordemProducaoId) {
        return dados.values().stream()
                .filter(c -> ordemProducaoId.equals(c.getOrdemProducaoId()))
                .toList();
    }
}
