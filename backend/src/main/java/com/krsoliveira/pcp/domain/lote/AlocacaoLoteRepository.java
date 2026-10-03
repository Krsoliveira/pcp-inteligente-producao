package com.krsoliveira.pcp.domain.lote;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Porta de persistência das alocações de lote (genealogia). */
public interface AlocacaoLoteRepository {

    void salvarTodas(List<AlocacaoLote> alocacoes);

    /** Alocações dos consumos informados — de onde veio o material de cada consumo. */
    List<AlocacaoLote> listarPorConsumos(Collection<UUID> consumoMaterialIds);

    /** Alocações de um lote — onde ele foi usado. */
    List<AlocacaoLote> listarPorLote(UUID loteId);
}
