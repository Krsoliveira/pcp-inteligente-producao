package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.estoque.ConsultaEstoque;
import com.krsoliveira.pcp.domain.estoque.PosicaoEstoque;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * ADAPTADOR de leitura: posição de estoque agregada no banco a partir dos lotes, uma
 * linha por material (GROUP BY) — sem carregar os lotes na memória.
 */
@Repository
public class EstoqueConsultaAdapter implements ConsultaEstoque {

    private final LoteSpringDataRepository lotes;

    public EstoqueConsultaAdapter(LoteSpringDataRepository lotes) {
        this.lotes = lotes;
    }

    @Override
    public List<PosicaoEstoque> posicoes(LocalDate hoje) {
        return lotes.posicoesDeEstoque(hoje).stream()
                .map(l -> new PosicaoEstoque((UUID) l[0], decimal(l[1]), ((Number) l[2]).intValue(),
                        decimal(l[3]), (LocalDate) l[4], (Instant) l[5]))
                .toList();
    }

    private static BigDecimal decimal(Object valor) {
        return valor == null ? BigDecimal.ZERO : new BigDecimal(valor.toString());
    }
}
