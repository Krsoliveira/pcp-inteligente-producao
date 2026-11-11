import { formatarQuantidade } from './formatacao'

/** Quanto vale cada unidade na unidade-base da sua grandeza (peso em gramas, volume em mililitros). */
const GRANDEZAS: Record<string, Record<string, number>> = {
  peso: { MG: 0.001, G: 1, KG: 1000, T: 1_000_000 },
  volume: { ML: 1, L: 1000 },
}

/** Diferença abaixo disto é erro de arredondamento, não de quantidade. */
const TOLERANCIA_RELATIVA = 1e-9

function grandezaDe(unidade: string): string | undefined {
  const normalizada = unidade.trim().toUpperCase()
  return Object.keys(GRANDEZAS).find((g) => normalizada in GRANDEZAS[g])
}

export interface ComponenteDaSoma {
  quantidade: number
  unidadeDeMedida: string
}

export interface DiferencaDaSoma {
  /** Soma dos componentes, já convertida para a unidade do material produzido. */
  soma: number
  unidade: string
}

/**
 * Compara a soma dos componentes com a quantidade base da lista, quando a comparação faz sentido: o material
 * produzido e todos os componentes são da mesma grandeza (peso com peso, volume com volume). Uma lista com "KG" e
 * "UN" não soma. Devolve a soma só quando ela difere da base; igual, ou impossível de comparar, devolve {@code null}.
 * É só um aviso: nada aqui bloqueia a lista (ADR-0037).
 */
export function diferencaDaSoma(
  componentes: ComponenteDaSoma[],
  quantidadeBase: number,
  unidadeDoMaterial: string,
): DiferencaDaSoma | null {
  const grandeza = grandezaDe(unidadeDoMaterial)
  if (!grandeza || !(quantidadeBase > 0) || componentes.length === 0) return null
  const tabela = GRANDEZAS[grandeza]
  const fatorBase = tabela[unidadeDoMaterial.trim().toUpperCase()]

  let somaNaBase = 0
  for (const componente of componentes) {
    if (!(componente.quantidade > 0) || grandezaDe(componente.unidadeDeMedida) !== grandeza) return null
    somaNaBase += componente.quantidade * tabela[componente.unidadeDeMedida.trim().toUpperCase()]
  }

  const soma = somaNaBase / fatorBase
  const diferenca = Math.abs(soma - quantidadeBase)
  if (diferenca <= TOLERANCIA_RELATIVA * Math.max(soma, quantidadeBase)) return null
  return { soma, unidade: unidadeDoMaterial.trim().toUpperCase() }
}

/** "A soma dos componentes (950 G) é diferente da quantidade base (1 KG)." */
export function mensagemDaSoma(diferenca: DiferencaDaSoma, quantidadeBase: number): string {
  return `A soma dos componentes (${formatarQuantidade(diferenca.soma, diferenca.unidade)}) é diferente da quantidade base `
    + `(${formatarQuantidade(quantidadeBase, diferenca.unidade)}).`
}
