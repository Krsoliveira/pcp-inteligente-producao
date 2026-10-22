package com.krsoliveira.pcp.domain.lista;

import com.krsoliveira.pcp.domain.auditoria.Pagina;
import com.krsoliveira.pcp.domain.comum.PedidoDePagina;

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

    List<ListaTecnica> listarTodas();

    /** Uma página de listas que atendem ao filtro, das criadas mais recentemente para as mais antigas. */
    Pagina<ListaTecnica> pesquisar(FiltroListasTecnicas filtro, PedidoDePagina pedido);

    List<ListaTecnica> listarPorMaterial(UUID materialId);

    Optional<ListaTecnica> buscarAtivaParaMaterial(UUID materialId);

    boolean existeVersaoParaMaterial(UUID materialId, String versao);
}