import { useState } from 'react'

function mudou(anteriores: readonly unknown[] | null, atuais: readonly unknown[]) {
  return anteriores === null
    || anteriores.length !== atuais.length
    || atuais.some((valor, indice) => !Object.is(valor, anteriores[indice]))
}

/**
 * Ajusta o estado do componente quando um valor de fora muda (a URL, o registro ou a abertura do diálogo). Roda na
 * primeira renderização e a cada mudança, durante a renderização: com um efeito, a tela chegaria a ser pintada com o
 * valor antigo e renderizaria de novo. O ajuste só pode alterar estado do próprio componente.
 */
export function useSincronizarAoMudar(valores: readonly unknown[], sincronizar: () => void) {
  const [anteriores, setAnteriores] = useState<readonly unknown[] | null>(null)
  if (mudou(anteriores, valores)) {
    setAnteriores(valores)
    sincronizar()
  }
}
