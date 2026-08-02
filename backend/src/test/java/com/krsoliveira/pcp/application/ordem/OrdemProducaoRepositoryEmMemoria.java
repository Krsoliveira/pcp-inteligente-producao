package com.krsoliveira.pcp.application.ordem;

import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação FAKE da porta para testes de caso de uso: um HashMap no lugar
 * do PostgreSQL. É a recompensa prática da Clean Architecture — testar a
 * orquestração sem subir banco nem Spring.
 */
class OrdemProducaoRepositoryEmMemoria implements OrdemProducaoRepository {

    private final Map<UUID, OrdemProducao> dados = new HashMap<>();

    @Override
    public OrdemProducao salvar(OrdemProducao ordem) {
        dados.put(ordem.getId(), ordem);
        return ordem;
    }

    @Override
    public Optional<OrdemProducao> buscarPorId(UUID id) {
        return Optional.ofNullable(dados.get(id));
    }

    @Override
    public List<OrdemProducao> listarTodas() {
        return new ArrayList<>(dados.values());
    }

    @Override
    public boolean existePorCodigo(String codigo) {
        return dados.values().stream().anyMatch(o -> o.getCodigo().equals(codigo));
    }
}
