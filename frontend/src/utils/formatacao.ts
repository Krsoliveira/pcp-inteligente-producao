/** "YYYY-MM-DD" → "DD/MM/YYYY" (sem conversão de fuso: é uma data de calendário). */
export function formatarData(data: string | null | undefined): string {
  if (!data) return '—'
  const [ano, mes, dia] = data.split('-')
  return `${dia}/${mes}/${ano}`
}

/** Instante ISO (UTC) → "DD/MM/YYYY HH:mm" no fuso do navegador. */
export function formatarDataHora(instante: string | null | undefined): string {
  if (!instante) return '—'
  return new Date(instante).toLocaleString('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

/** Instante ISO (UTC) → "YYYY-MM-DD" no fuso do navegador — para filtros por dia. */
export function dataLocalIso(instante: string): string {
  const d = new Date(instante)
  const mes = String(d.getMonth() + 1).padStart(2, '0')
  const dia = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${mes}-${dia}`
}

export function formatarQuantidade(valor: number | null | undefined, unidade?: string): string {
  if (valor == null) return '—'
  const numero = Number(valor).toLocaleString('pt-BR', { maximumFractionDigits: 4 })
  return unidade ? `${numero} ${unidade}` : numero
}

/** Dias corridos de hoje até a data ("YYYY-MM-DD"); negativo se já passou. */
export function diasAte(data: string): number {
  const [ano, mes, dia] = data.split('-').map(Number)
  const alvo = new Date(ano, mes - 1, dia)
  const hoje = new Date()
  hoje.setHours(0, 0, 0, 0)
  return Math.round((alvo.getTime() - hoje.getTime()) / 86_400_000)
}

/** Código de material (9 dígitos) → "103.000.001"; outros formatos passam sem mudança. */
export function formatarCodigoMaterial(codigo: string | null | undefined): string {
  if (!codigo) return '—'
  return /^\d{9}$/.test(codigo) ? `${codigo.slice(0, 3)}.${codigo.slice(3, 6)}.${codigo.slice(6)}` : codigo
}

/** Busca de material tolerante à máscara: "103.000.001", "103000001" e "000001" encontram o mesmo. */
export function codigoMaterialCorresponde(codigo: string, termo: string): boolean {
  const digitos = termo.replace(/\D/g, '')
  return codigo.toLowerCase().includes(termo.toLowerCase()) || (digitos !== '' && codigo.includes(digitos))
}
