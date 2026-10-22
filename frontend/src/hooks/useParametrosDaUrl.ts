import { useSearchParams } from 'react-router-dom'

type Valor = string | number | null | undefined

/**
 * Guarda os filtros e a página na URL (ex.: /ordens/resultados?status=LIBERADA&pagina=2): o
 * botão Voltar do navegador, o F5 e o link copiado reproduzem a mesma consulta.
 */
export function useParametrosDaUrl() {
  const [parametros, setParametros] = useSearchParams()

  const texto = (chave: string) => parametros.get(chave) ?? ''

  const numero = (chave: string, padrao: number) => {
    const valor = Number(parametros.get(chave))
    return parametros.has(chave) && Number.isInteger(valor) && valor >= 0 ? valor : padrao
  }

  /** Altera só as chaves informadas; valor vazio ou indefinido remove a chave. */
  const atualizar = (alteracoes: Record<string, Valor>) => {
    setParametros((atuais) => {
      const novos = new URLSearchParams(atuais)
      for (const [chave, valor] of Object.entries(alteracoes)) {
        if (valor === undefined || valor === null || String(valor) === '') novos.delete(chave)
        else novos.set(chave, String(valor))
      }
      return novos
    })
  }

  return { texto, numero, atualizar }
}
