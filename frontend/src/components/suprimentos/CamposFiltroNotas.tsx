import { CamposDePeriodo } from '../consulta/CamposDePeriodo'

interface CamposFiltroNotasProps {
  de: string
  onDe: (valor: string) => void
  ate: string
  onAte: (valor: string) => void
}

/** O único critério que vai ao servidor nas notas fiscais: o período de recebimento. */
export function CamposFiltroNotas(props: CamposFiltroNotasProps) {
  return <CamposDePeriodo rotuloDe="Recebida de" {...props} />
}
