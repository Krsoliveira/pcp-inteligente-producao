package com.krsoliveira.pcp.domain.lote;

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

    List<Lote> listarTodos();

    List<Lote> listarPorOrdemProducao(UUID ordemProducaoId);

    Optional<Lote> buscarPorNumeroLote(String numeroLote);

    /**
     * Já existe entrada deste material com a mesma nota fiscal do mesmo fornecedor?
     * Evita registrar o mesmo recebimento duas vezes.
     */
    boolean existeEntrada(UUID materialId, String fornecedor, String notaFiscal);

    /**
     * Retorna o próximo número sequencial para geração do número do lote.
     * Conta quantos lotes já existem para o mesmo material no mesmo ano-mês
     * e retorna o próximo na sequência.
     *
     * @param materialId ID do material
     * @param prefixo prefixo do número do lote (ex.: "MAT-ACO-1020-202609")
     * @return próximo sequencial (1-based)
     */
    int proximoSequencial(UUID materialId, String prefixo);
}
