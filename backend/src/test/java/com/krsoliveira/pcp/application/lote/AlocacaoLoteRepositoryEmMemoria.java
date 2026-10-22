package com.krsoliveira.pcp.application.lote;

import com.krsoliveira.pcp.domain.lote.AlocacaoLote;
import com.krsoliveira.pcp.domain.lote.AlocacaoLoteRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class AlocacaoLoteRepositoryEmMemoria implements AlocacaoLoteRepository {

    private final List<AlocacaoLote> dados = new ArrayList<>();

    @Override
    public void salvarTodas(List<AlocacaoLote> alocacoes) {
        dados.addAll(alocacoes);
    }

    @Override
    public List<AlocacaoLote> listarPorConsumos(Collection<UUID> consumoMaterialIds) {
        return dados.stream().filter(a -> consumoMaterialIds.contains(a.getConsumoMaterialId())).toList();
    }

    @Override
    public List<AlocacaoLote> listarPorLote(UUID loteId) {
        return dados.stream().filter(a -> a.getLoteId().equals(loteId))
                .sorted(Comparator.comparing(AlocacaoLote::getCriadoEm).reversed()).toList();
    }

    public List<AlocacaoLote> todas() {
        return List.copyOf(dados);
    }
}
