import InputAdornment from '@mui/material/InputAdornment'
import TextField from '@mui/material/TextField'
import SearchIcon from '@mui/icons-material/Search'

/** Data válida na URL ("AAAA-MM-DD"); qualquer outro valor é ignorado. */
export const ehDataIso = (valor: string) => /^[0-9]{4}-[0-9]{2}-[0-9]{2}$/.test(valor)

/** O fim do período é anterior ao início? (o servidor também recusa esse caso) */
export const periodoInvertido = (de: string, ate: string) => !!de && !!ate && ate < de

interface CamposFiltroNotasProps {
  busca: string
  onBusca: (valor: string) => void
  de: string
  onDe: (valor: string) => void
  ate: string
  onAte: (valor: string) => void
}

export function CamposFiltroNotas({ busca, onBusca, de, onDe, ate, onAte }: CamposFiltroNotasProps) {
  const invertido = periodoInvertido(de, ate)

  return (
    <>
      <TextField
        label="Nota, fornecedor ou lote"
        placeholder="Ex.: NF-1234, Aços Brasil ou LOTE-A1"
        value={busca}
        onChange={(e) => onBusca(e.target.value)}
        sx={{ flex: 1, minWidth: 260 }}
        slotProps={{ input: { startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" color="action" /></InputAdornment> } }}
      />
      <TextField
        label="Recebida de"
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
