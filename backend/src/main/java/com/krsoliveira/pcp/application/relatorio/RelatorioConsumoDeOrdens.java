package com.krsoliveira.pcp.application.relatorio;

import java.time.LocalDate;
import java.util.List;

/** @param totalDeOrdens quantas ordens diferentes aparecem nas linhas */
public record RelatorioConsumoDeOrdens(LocalDate de, LocalDate ate, long totalDeOrdens,
                                       List<LinhaDeConsumoDeOrdem> linhas) {
}
