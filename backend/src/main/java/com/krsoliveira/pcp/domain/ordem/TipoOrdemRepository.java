package com.krsoliveira.pcp.domain.ordem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de persistência para a entidade {@link TipoOrdem}.
 * A implementação fica na camada de infraestrutura.
 */
public interface TipoOrdemRepository {

    TipoOrdem salvar(TipoOrdem tipoOrdem);

    Optional<TipoOrdem> buscarPorId(UUID id);

    List<TipoOrdem> listarTodos();

    boolean existePorNome(String nome);
}
