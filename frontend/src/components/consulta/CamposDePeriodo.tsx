import TextField from '@mui/material/TextField'

/** Data válida na URL ("AAAA-MM-DD"); qualquer outro valor é ignorado. */
export const ehDataIso = (valor: string) => /^[0-9]{4}-[0-9]{2}-[0-9]{2}$/.test(valor)

/** O fim do período é anterior ao início? (o servidor também recusa esse caso) */
export const periodoInvertido = (de: string, ate: string) => !!de && !!ate && ate < de

interface CamposDePeriodoProps {
  /** Rótulo do campo inicial, ex.: "Recebida de". O final é sempre "até". */
  rotuloDe: string
  de: string
  onDe: (valor: string) => void
  ate: string
  onAte: (valor: string) => void
}

/** Dois campos de data (início e fim, inclusivos); avisa quando o fim é anterior ao início. */
export function CamposDePeriodo({ rotuloDe, de, onDe, ate, onAte }: CamposDePeriodoProps) {
  const invertido = periodoInvertido(de, ate)

  return (
    <>
      <TextField
        label={rotuloDe}
        type="date"
        value={de}
        onChange={(e) => onDe(e.target.value)}
        slotProps={{ inputLabel: { shrink: true }, htmlInput: { max: ate || undefined } }}
        sx={{ minWidth: 160 }}
      />
      <TextField
        label="até"
        type="date"
        value={ate}
        onChange={(e) => onAte(e.target.value)}
        error={invertido}
        helperText={invertido ? 'Anterior ao início.' : undefined}
        slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: de || undefined } }}
        sx={{ minWidth: 160 }}
      />
    </>
  )
}
