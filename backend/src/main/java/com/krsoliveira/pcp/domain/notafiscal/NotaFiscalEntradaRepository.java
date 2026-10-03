package com.krsoliveira.pcp.domain.notafiscal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de persistência das notas fiscais de entrada. */
public interface NotaFiscalEntradaRepository {

    void salvar(NotaFiscalEntrada nota);

    Optional<NotaFiscalEntrada> buscarPorId(UUID id);

    /** A mesma nota (fornecedor + número) não entra duas vezes. Fornecedor sem diferenciar maiúsculas. */
    boolean existe(String fornecedor, String numero);

    /** Notas recebidas no período (datas inclusivas; nulas = sem limite), da mais recente para a mais antiga. */
    List<NotaFiscalEntrada> listarPorRecebimento(LocalDate de, LocalDate ate);
}
