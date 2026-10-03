import type { Lote } from '../../types'

/** Quantidades em décimos de milésimo (4 casas, como no backend) para somar sem erro de ponto flutuante. */
export const paraUnidades = (valor: number): number => Math.round(valor * 10_000)
export const deUnidades = (unidades: number): number => unidades / 10_000

/** Converte o texto digitado ("12,5") em número; vazio ou inválido vira 0. */
export function lerQuantidade(texto: string): number {
  const numero = Number(texto.replace(',', '.'))
  return Number.isFinite(numero) && numero > 0 ? numero : 0
}

/**
 * Sugestão FEFO: tira dos lotes na ordem recebida (o backend já devolve por validade)
 * até cobrir a quantidade. Devolve loteId → quantidade (só lotes usados).
 */
export function sugerirFefo(lotes: Lote[], quantidade: number): Record<string, number> {
  let restante = paraUnidades(quantidade)
  const sugestao: Record<string, number> = {}
  for (const lote of lotes) {
    if (restante <= 0) break
    const usar = Math.min(paraUnidades(lote.saldo), restante)
    if (usar > 0) {
      sugestao[lote.id] = deUnidades(usar)
      restante -= usar
    }
  }
  return sugestao
}
