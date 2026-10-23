package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.application.estoque.ResumoEstoque;

/** Alertas do estoque: materiais com lote vencendo e matérias-primas sem saldo. */
public record ResumoEstoqueResponse(long totalMateriais, long materiaisVencendo, long materiasPrimasSemSaldo) {

    public static ResumoEstoqueResponse de(ResumoEstoque resumo) {
        return new ResumoEstoqueResponse(resumo.totalMateriais(), resumo.materiaisVencendo(),
                resumo.materiasPrimasSemSaldo());
    }
}
