package com.krsoliveira.pcp.domain.estoque;

import java.time.LocalDate;
import java.util.List;

/** Porta de leitura: posição de estoque por material, agregada a partir dos lotes. */
public interface ConsultaEstoque {

    /** Uma posição por material que tem ao menos um lote; {@code hoje} define o que está vencido. */
    List<PosicaoEstoque> posicoes(LocalDate hoje);
}
