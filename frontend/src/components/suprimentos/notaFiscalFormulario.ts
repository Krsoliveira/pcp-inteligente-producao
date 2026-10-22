import type { RegistrarNotaFiscalRequest } from '../../types'

/** Estado do formulário de entrada de nota fiscal: tudo como texto, convertido no envio. */
export interface ItemFormulario {
  chave: number
  materialId: string
  numeroLote: string
  quantidade: string
  dataFabricacao: string
  dataValidade: string
}

export interface CabecalhoFormulario {
  fornecedor: string
  numero: string
  dataEmissao: string
  dataRecebimento: string
}

export type ErrosCabecalho = Partial<Record<keyof CabecalhoFormulario, string>>
export type ErrosItem = Partial<Record<Exclude<keyof ItemFormulario, 'chave'>, string>>

export interface ErrosFormulario {
  cabecalho: ErrosCabecalho
  itens: Record<number, ErrosItem>
  geral?: string
}

/** Mesmo padrão do backend (Lote.normalizarNumero): até 20 letras, dígitos e . / - */
export const NUMERO_LOTE_VALIDO = /^[A-Z0-9./-]{1,20}$/

export const hojeIso = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

export const normalizarLote = (valor: string) => valor.trim().toUpperCase()

const paraNumero = (valor: string) => Number(valor.replace(',', '.'))

export const temErros = (erros: ErrosFormulario) =>
  Object.keys(erros.cabecalho).length > 0
  || Object.values(erros.itens).some((e) => Object.keys(e).length > 0)
  || !!erros.geral

/** Mesmas regras do backend (RegistrarNotaFiscalRequest + RegistrarEntradaNotaFiscal + Lote). */
export function validarNotaFiscal(cabecalho: CabecalhoFormulario, itens: ItemFormulario[]): ErrosFormulario {
  const hoje = hojeIso()
  const c: ErrosCabecalho = {}
  if (!cabecalho.fornecedor.trim()) c.fornecedor = 'Informe o fornecedor.'
  if (!cabecalho.numero.trim()) c.numero = 'Informe o número da nota.'
  if (!cabecalho.dataEmissao) c.dataEmissao = 'Informe a emissão.'
  else if (cabecalho.dataEmissao > hoje) c.dataEmissao = 'Não pode estar no futuro.'
  if (!cabecalho.dataRecebimento) c.dataRecebimento = 'Informe o recebimento.'
  else if (cabecalho.dataRecebimento > hoje) c.dataRecebimento = 'Não pode estar no futuro.'
  else if (cabecalho.dataEmissao && cabecalho.dataRecebimento < cabecalho.dataEmissao) {
    c.dataRecebimento = 'Não pode ser anterior à emissão.'
  }

  const porItem: Record<number, ErrosItem> = {}
  const vistos = new Set<string>()
  itens.forEach((item) => {
    const e: ErrosItem = {}
    if (!item.materialId) e.materialId = 'Selecione a matéria-prima.'
    const lote = normalizarLote(item.numeroLote)
    if (!lote) e.numeroLote = 'Informe o lote do fornecedor.'
    else if (!NUMERO_LOTE_VALIDO.test(lote)) e.numeroLote = 'Até 20 caracteres: letras, dígitos e . / -'
    else if (item.materialId) {
      const chave = `${item.materialId}|${lote}`
      if (vistos.has(chave)) e.numeroLote = 'Lote repetido para este material na nota.'
      vistos.add(chave)
    }
    const quantidade = paraNumero(item.quantidade)
    if (!item.quantidade || !Number.isFinite(quantidade) || quantidade <= 0) e.quantidade = 'Maior que zero.'
    if (!item.dataFabricacao) e.dataFabricacao = 'Informe a fabricação.'
    else if (item.dataFabricacao > hoje) e.dataFabricacao = 'Não pode estar no futuro.'
    else if (cabecalho.dataRecebimento && item.dataFabricacao > cabecalho.dataRecebimento) {
      e.dataFabricacao = 'Posterior ao recebimento.'
    }
    if (!item.dataValidade) e.dataValidade = 'Informe a validade.'
    else if (item.dataFabricacao && item.dataValidade < item.dataFabricacao) e.dataValidade = 'Anterior à fabricação.'
    if (Object.keys(e).length > 0) porItem[item.chave] = e
  })

  return {
    cabecalho: c,
    itens: porItem,
    geral: itens.length === 0 ? 'Adicione pelo menos um item.' : undefined,
  }
}

export function paraRequisicao(cabecalho: CabecalhoFormulario, itens: ItemFormulario[]): RegistrarNotaFiscalRequest {
  return {
    fornecedor: cabecalho.fornecedor.trim(),
    numero: cabecalho.numero.trim(),
    dataEmissao: cabecalho.dataEmissao,
    dataRecebimento: cabecalho.dataRecebimento,
    itens: itens.map((i) => ({
      materialId: i.materialId,
      numeroLote: normalizarLote(i.numeroLote),
      quantidade: paraNumero(i.quantidade),
      dataFabricacao: i.dataFabricacao,
      dataValidade: i.dataValidade,
    })),
  }
}

/**
 * Erros de validação do backend ({"itens[0].numeroLote": "..."} ou {"fornecedor": "..."})
 * → mesma estrutura do formulário.
 */
export function errosDoServidor(erros: Record<string, string>, itens: ItemFormulario[]): ErrosFormulario {
  const resultado: ErrosFormulario = { cabecalho: {}, itens: {} }
  Object.entries(erros).forEach(([campo, mensagem]) => {
    const item = /^itens\[(\d+)]\.(\w+)$/.exec(campo)
    if (item) {
      const chave = itens[Number(item[1])]?.chave
      if (chave === undefined) return
      resultado.itens[chave] = { ...resultado.itens[chave], [item[2]]: mensagem }
    } else if (campo === 'itens') {
      resultado.geral = mensagem
    } else {
      resultado.cabecalho[campo as keyof CabecalhoFormulario] = mensagem
    }
  })
  return resultado
}
