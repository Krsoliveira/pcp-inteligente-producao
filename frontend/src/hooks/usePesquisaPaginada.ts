import { useEffect } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import type { PaginaDe } from '../types'
import type { useParametrosDaUrl } from './useParametrosDaUrl'

const TAMANHO_PADRAO = 25

interface OpcoesDaPesquisa<T> {
  url: ReturnType<typeof useParametrosDaUrl>
  /** Identifica a consulta; a página e o tamanho entram sozinhos. */
  chave: readonly unknown[]
  consultar: (pagina: { pagina: number; tamanho: number }) => Promise<PaginaDe<T>>
  /** Falso quando os filtros da URL são inválidos: não consulta o servidor. */
  habilitada?: boolean
}

/**
 * O que toda tela de resultados faz igual: lê a página da URL, consulta mantendo a página anterior na tela enquanto a
 * nova chega, volta para a última página se a pedida deixou de existir e diz se a falha deixou a tela sem dados.
 */
export function usePesquisaPaginada<T>({ url, chave, consultar, habilitada = true }: OpcoesDaPesquisa<T>) {
  const pagina = url.numero('pagina', 0)
  const tamanho = url.numero('tamanho', TAMANHO_PADRAO)

  const { data, isLoading, isError, error, isPlaceholderData } = useQuery({
    queryKey: [...chave, pagina, tamanho],
    queryFn: () => consultar({ pagina, tamanho }),
    enabled: habilitada,
    placeholderData: keepPreviousData,
  })

  useEffect(() => {
    if (data && data.itens.length === 0 && data.total > 0 && pagina > 0) {
      url.atualizar({ pagina: Math.max(0, data.totalPaginas - 1) })
    }
  }, [data, pagina]) // eslint-disable-line react-hooks/exhaustive-deps

  return {
    itens: data?.itens ?? [],
    total: data?.total ?? 0,
    pagina,
    tamanho,
    isLoading,
    isError,
    error,
    isPlaceholderData,
    falhouSemDados: isError && !data,
  }
}
