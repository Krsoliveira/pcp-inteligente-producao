package com.krsoliveira.pcp.domain.lote;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de persistência para a entidade {@link Lote}.
 * A implementação fica na camada de infraestrutura.
 */
public interface LoteRepository {

    Lote salvar(Lote lote);

    Optional<Lote> buscarPorId(UUID id);

    /**
     * Busca o lote bloqueando-o até o fim da transação — para alocar saldo sem que dois
     * consumos simultâneos retirem mais do que o lote tem.
     */
    Optional<Lote> buscarPorIdParaAtualizar(UUID id);

    List<Lote> buscarPorIds(Collection<UUID> ids);

    List<Lote> listarPorOrdens(Collection<UUID> ordemProducaoIds);

    /** Lotes DISPONIVEIS do material com saldo, ordenados por validade (FEFO). */
    List<Lote> listarDisponiveisPorMaterial(UUID materialId);

    List<Lote> listarTodos();

    List<Lote> listarPorOrdemProducao(UUID ordemProducaoId);


    /** Todos os lotes do material, de qualquer status. */
    List<Lote> listarPorMaterial(UUID materialId);

    /** Lotes trazidos pelas notas fiscais informadas (os itens das notas). */
    List<Lote> listarPorNotasFiscais(Collection<UUID> notaFiscalIds);

    /**
     * Já existe lote com este número para o material (e o fornecedor, em compras)?
     * Lotes de fornecedores diferentes podem ter o mesmo número.
     */
    boolean existeLote(UUID materialId, String numeroLote, String fornecedor);

    /**
     * Próximo sequencial do dia para lotes de produção ({@code AAMMDD} + sequência). A
     * implementação garante que conclusões simultâneas não recebam o mesmo número —
     * deve ser chamada dentro da transação que grava o lote.
     */
    int proximoSequencialProducao(LocalDate data);
}
