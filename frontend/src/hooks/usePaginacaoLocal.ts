import { useState } from 'react'

/**
 * Pagina no navegador uma lista que já foi carregada por inteiro.
 * Se os filtros reduzirem a lista, a página atual é ajustada sozinha para a última válida.
 */
export function usePaginacaoLocal<T>(itens: T[], tamanhoInicial = 25) {
  const [pagina, setPagina] = useState(0)
  const [tamanho, setTamanhoBruto] = useState(tamanhoInicial)

  const ultimaPagina = Math.max(0, Math.ceil(itens.length / tamanho) - 1)
  const paginaAtual = Math.min(pagina, ultimaPagina)
  const visiveis = itens.slice(paginaAtual * tamanho, (paginaAtual + 1) * tamanho)

  const setTamanho = (novoTamanho: number) => {
    setTamanhoBruto(novoTamanho)
    setPagina(0)
  }

  return { visiveis, pagina: paginaAtual, tamanho, total: itens.length, setPagina, setTamanho }
}
