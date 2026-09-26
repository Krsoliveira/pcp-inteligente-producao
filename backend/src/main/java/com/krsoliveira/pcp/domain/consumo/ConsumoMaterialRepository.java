package com.krsoliveira.pcp.domain.consumo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de persistência para a entidade {@link ConsumoMaterial}.
 * A implementação fica na camada de infraestrutura.
 */
public interface ConsumoMaterialRepository {

    ConsumoMaterial salvar(ConsumoMaterial consumo);

    List<ConsumoMaterial> salvarTodos(List<ConsumoMaterial> consumos);

    Optional<ConsumoMaterial> buscarPorId(UUID id);

    List<ConsumoMaterial> listarPorOrdemProducao(UUID ordemProducaoId);
}
