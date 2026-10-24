import type { ConsumoMaterial, LancarConsumoRequest } from '../../types'
import { deUnidades, lerQuantidade, paraUnidades } from './alocacao'

/** O que o usuário digitou numa linha do lançamento. */
export interface Rascunho {
  qtd: string
  justificativa: string
  /** loteId → quantidade (texto, como digitado). */
  alocado: Record<string, string>
}

export const RASCUNHO_VAZIO: Rascunho = { qtd: '', justificativa: '', alocado: {} }

/** Item que o usuário está adicionando e que não está na lista técnica. */
export interface ItemAdicionado extends Rascunho {
  chave: string
  materialId: string
}

const TOLERANCIA = 0.0001

/** Quantidade digitada: `null` se vazia, `NaN` se inválida ou negativa. */
export function lerQuantidadeOpcional(texto: string): number | null {
  if (texto.trim() === '') return null
  const numero = Number(texto.trim().replace(',', '.'))
  return Number.isFinite(numero) && numero >= 0 ? numero : Number.NaN
}

const somaDosLotes = (alocado: Record<string, string>) =>
  Object.values(alocado).reduce((total, v) => total + paraUnidades(lerQuantidade(v)), 0)

const alocacoesDe = (alocado: Record<string, string>) =>
  Object.entries(alocado)
    .map(([loteId, v]) => ({ loteId, quantidade: lerQuantidade(v) }))
    .filter((a) => a.quantidade > 0)

/** Confere os lotes de uma linha com quantidade informada; devolve o motivo ou `null`. */
function erroDosLotes(rotulo: string, quantidade: number, alocado: Record<string, string>): string | null {
  if (quantidade <= 0) return null
  const soma = somaDosLotes(alocado)
  if (soma !== paraUnidades(quantidade)) {
    return `${rotulo}: a soma dos lotes (${deUnidades(soma).toLocaleString('pt-BR')}) deve ser igual à quantidade consumida (${quantidade.toLocaleString('pt-BR')}).`
  }
  return null
}

/**
 * Monta o lançamento a partir do que foi digitado. Linhas da lista com a quantidade em branco ficam de fora (o que ainda
 * não foi consumido); as preenchidas e os itens adicionados seguem as mesmas regras do servidor, que é quem decide
 * (tudo ou nada). Devolve o corpo da requisição, ou o motivo de ainda não poder enviar.
 */
export function montarLancamento(
  pendentes: ConsumoMaterial[],
  rascunhos: Record<string, Rascunho>,
  adicionados: ItemAdicionado[],
  rotulo: (materialId: string) => string,
): { corpo: LancarConsumoRequest } | { erro: string } {
  const corpo: LancarConsumoRequest = { consumos: [], itensAdicionais: [] }

  for (const consumo of pendentes) {
    const r = rascunhos[consumo.id] ?? RASCUNHO_VAZIO
    const quantidade = lerQuantidadeOpcional(r.qtd)
    if (quantidade === null) continue
    const nome = rotulo(consumo.materialId)
    if (Number.isNaN(quantidade)) return { erro: `${nome}: informe uma quantidade válida.` }
    const desvio = quantidade - consumo.quantidadePlanejada
    if (Math.abs(desvio) > TOLERANCIA && !r.justificativa.trim()) {
      return { erro: `${nome}: há desvio, informe a justificativa.` }
    }
    const erroLotes = erroDosLotes(nome, quantidade, r.alocado)
    if (erroLotes) return { erro: erroLotes }
    corpo.consumos.push({
      consumoId: consumo.id,
      quantidadeConsumida: quantidade,
      justificativa: r.justificativa.trim() || undefined,
      alocacoes: quantidade > 0 ? alocacoesDe(r.alocado) : [],
    })
  }

  for (const item of adicionados) {
    if (!item.materialId) return { erro: 'Escolha o material de cada item adicionado.' }
    const nome = rotulo(item.materialId)
    const quantidade = lerQuantidadeOpcional(item.qtd)
    if (quantidade === null || Number.isNaN(quantidade) || quantidade <= 0) {
      return { erro: `${nome}: informe a quantidade consumida do item adicionado.` }
    }
    if (!item.justificativa.trim()) return { erro: `${nome}: item fora da lista exige justificativa.` }
    const erroLotes = erroDosLotes(nome, quantidade, item.alocado)
    if (erroLotes) return { erro: erroLotes }
    corpo.itensAdicionais.push({
      materialId: item.materialId,
      quantidadeConsumida: quantidade,
      justificativa: item.justificativa.trim(),
      alocacoes: alocacoesDe(item.alocado),
    })
  }

  if (corpo.consumos.length === 0 && corpo.itensAdicionais.length === 0) {
    return { erro: 'Informe o consumo de ao menos uma linha.' }
  }
  return { corpo }
}
