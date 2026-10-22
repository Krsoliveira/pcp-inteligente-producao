import { useCallback, useEffect, useState } from 'react'

/**
 * Contagem regressiva em segundos, atualizada a cada segundo.
 * `iniciar(n)` começa (ou reinicia) a contagem; `restante` chega a 0 sozinho.
 */
export function useContagemRegressiva() {
  const [restante, setRestante] = useState(0)

  useEffect(() => {
    if (restante <= 0) return
    const id = setTimeout(() => setRestante((atual) => atual - 1), 1000)
    return () => clearTimeout(id)
  }, [restante])

  const iniciar = useCallback((segundos: number) => setRestante(Math.max(0, Math.ceil(segundos))), [])

  return { restante, iniciar }
}
