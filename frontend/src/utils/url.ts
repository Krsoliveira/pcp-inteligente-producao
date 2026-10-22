/** Monta a query string ("?a=1&b=2") ignorando valores vazios; devolve "" se não sobrar nenhum. */
export function montarBusca(parametros: Record<string, string | number | null | undefined>): string {
  const busca = new URLSearchParams()
  for (const [chave, valor] of Object.entries(parametros)) {
    if (valor !== undefined && valor !== null && String(valor) !== '') busca.set(chave, String(valor))
  }
  const texto = busca.toString()
  return texto ? `?${texto}` : ''
}
