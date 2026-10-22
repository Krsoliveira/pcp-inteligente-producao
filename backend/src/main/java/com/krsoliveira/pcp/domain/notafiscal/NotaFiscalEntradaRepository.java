package com.krsoliveira.pcp.domain.notafiscal;

import com.krsoliveira.pcp.domain.auditoria.Pagina;
import com.krsoliveira.pcp.domain.comum.PedidoDePagina;

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

    /**
     * Uma página de notas que atendem ao filtro, da recebida mais recentemente para a mais antiga
     * (empate pelo registro mais recente).
     */
    Pagina<NotaFiscalEntrada> pesquisar(FiltroNotasFiscais filtro, PedidoDePagina pedido);

    /** Notas recebidas no período (datas inclusivas; nulas = sem limite), da mais recente para a mais antiga. */
    List<NotaFiscalEntrada> listarPorRecebimento(LocalDate de, LocalDate ate);
}
