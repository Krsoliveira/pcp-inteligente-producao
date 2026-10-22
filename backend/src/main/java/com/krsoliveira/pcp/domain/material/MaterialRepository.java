package com.krsoliveira.pcp.domain.material;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída: contrato de persistência para {@link Material}.
 * Implementada pela camada de infraestrutura.
 */
public interface MaterialRepository {

    void salvar(Material material);

    Optional<Material> buscarPorId(UUID id);

    Optional<Material> buscarPorCodigo(String codigo);

    List<Material> listarTodos();

    /**
     * Próximo código livre na faixa do tipo ({@link CodigoMaterial}). A implementação
     * garante que dois cadastros simultâneos do mesmo tipo não recebam o mesmo código —
     * deve ser chamada dentro da transação que grava o material.
     */
    String proximoCodigo(TipoMaterial tipo);
}