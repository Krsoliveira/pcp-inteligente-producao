package com.krsoliveira.pcp.domain.lista;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída: contrato de persistência para {@link ListaTecnica}.
 * Implementada pela camada de infraestrutura.
 */
public interface ListaTecnicaRepository {

    void salvar(ListaTecnica lista);

    Optional<ListaTecnica> buscarPorId(UUID id);

    List<ListaTecnica> listarPorMaterial(UUID materialId);

    Optional<ListaTecnica> buscarAtivaParaMaterial(UUID materialId);

    boolean existeVersaoParaMaterial(UUID materialId, String versao);
}