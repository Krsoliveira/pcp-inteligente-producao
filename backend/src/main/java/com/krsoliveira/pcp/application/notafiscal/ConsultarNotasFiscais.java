package com.krsoliveira.pcp.application.notafiscal;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.LoteRepository;
import com.krsoliveira.pcp.domain.notafiscal.NotaFiscalEntrada;
import com.krsoliveira.pcp.domain.notafiscal.NotaFiscalEntradaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Caso de uso: consultar as notas fiscais de entrada com os lotes que cada uma trouxe.
 * Lotes buscados de uma vez para todas as notas (sem N+1).
 */
public class ConsultarNotasFiscais {

    private final NotaFiscalEntradaRepository notaRepository;
    private final LoteRepository loteRepository;

    public ConsultarNotasFiscais(NotaFiscalEntradaRepository notaRepository, LoteRepository loteRepository) {
        this.notaRepository = notaRepository;
        this.loteRepository = loteRepository;
    }

    /** Uma nota e seus itens (lotes). */
    public record NotaComItens(NotaFiscalEntrada nota, List<Lote> itens) {}

    /** Notas recebidas no período (datas inclusivas; nulas = sem limite). */
    public List<NotaComItens> listar(LocalDate de, LocalDate ate) {
        if (de != null && ate != null && ate.isBefore(de)) {
            throw new RegraDeNegocioException("O fim do período não pode ser anterior ao início.");
        }
        List<NotaFiscalEntrada> notas = notaRepository.listarPorRecebimento(de, ate);
        Map<UUID, List<Lote>> lotesPorNota = loteRepository
                .listarPorNotasFiscais(notas.stream().map(NotaFiscalEntrada::getId).toList()).stream()
                .collect(Collectors.groupingBy(Lote::getNotaFiscalId));
        return notas.stream()
                .map(n -> new NotaComItens(n, lotesPorNota.getOrDefault(n.getId(), List.of())))
                .toList();
    }

    public NotaComItens porId(UUID id) {
        NotaFiscalEntrada nota = notaRepository.buscarPorId(id)
                .orElseThrow(() -> new NotaFiscalNaoEncontradaException(id));
        return new NotaComItens(nota, loteRepository.listarPorNotasFiscais(List.of(id)));
    }
}
