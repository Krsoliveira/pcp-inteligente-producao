const MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez']

/** "2026-10" → "out/26". */
export function rotuloDoMes(mes: string): string {
  const [ano, numero] = mes.split('-')
  return `${MESES[Number(numero) - 1] ?? numero}/${ano.slice(2)}`
}

/** "2026-02" → primeiro e último dia do mês, em AAAA-MM-DD (o último respeita o ano bissexto). */
export function intervaloDoMes(mes: string): { de: string; ate: string } {
  const [ano, numero] = mes.split('-').map(Number)
  const ultimoDia = new Date(ano, numero, 0).getDate()
  return { de: `${mes}-01`, ate: `${mes}-${String(ultimoDia).padStart(2, '0')}` }
}
