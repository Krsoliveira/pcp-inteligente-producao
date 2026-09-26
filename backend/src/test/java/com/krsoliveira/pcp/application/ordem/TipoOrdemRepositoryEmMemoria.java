package com.krsoliveira.pcp.application.ordem;

import com.krsoliveira.pcp.domain.ordem.TipoOrdem;
import com.krsoliveira.pcp.domain.ordem.TipoOrdemRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TipoOrdemRepositoryEmMemoria implements TipoOrdemRepository {

    private final Map<UUID, TipoOrdem> dados = new HashMap<>();

    @Override
    public TipoOrdem salvar(TipoOrdem tipoOrdem) {
        dados.put(tipoOrdem.getId(), tipoOrdem);
        return tipoOrdem;
    }

    @Override
    public Optional<TipoOrdem> buscarPorId(UUID id) {
        return Optional.ofNullable(dados.get(id));
    }

    @Override
    public List<TipoOrdem> listarTodos() {
        return new ArrayList<>(dados.values());
    }

    @Override
    public boolean existePorNome(String nome) {
        return dados.values().stream()
                .anyMatch(t -> t.getNome().equalsIgnoreCase(nome));
    }
}
