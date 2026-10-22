package com.krsoliveira.pcp.application.estoque;

/**
 * Alertas do estoque, calculados sobre todos os materiais.
 *
 * @param materiaisVencendo      materiais com lote disponível vencendo nos próximos 30 dias
 * @param materiasPrimasSemSaldo matérias-primas sem saldo disponível
 */
public record ResumoEstoque(long totalMateriais, long materiaisVencendo, long materiasPrimasSemSaldo) {
}
