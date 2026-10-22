import { useEffect, useState } from 'react'

/**
 * Devolve `valor` só depois de ele ficar parado por `atrasoMs`. Usado na busca: o
 * servidor é consultado quando a pessoa para de digitar, não a cada tecla.
 */
export function useValorComAtraso<T>(valor: T, atrasoMs = 400): T {
  const [atrasado, setAtrasado] = useState(valor)

  useEffect(() => {
    const id = setTimeout(() => setAtrasado(valor), atrasoMs)
    return () => clearTimeout(id)
  }, [valor, atrasoMs])

  return atrasado
}
