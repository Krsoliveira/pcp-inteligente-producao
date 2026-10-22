import InputAdornment from '@mui/material/InputAdornment'
import MenuItem from '@mui/material/MenuItem'
import TextField from '@mui/material/TextField'
import SearchIcon from '@mui/icons-material/Search'
import type { StatusOrdem } from '../../types'

const STATUS_OPCOES: Array<{ label: string; value: StatusOrdem | 'TODAS' }> = [
  { label: 'Todos os status', value: 'TODAS' },
  { label: 'Planejada', value: 'PLANEJADA' },
  { label: 'Liberada', value: 'LIBERADA' },
  { label: 'Em Produção', value: 'EM_PRODUCAO' },
  { label: 'Concluída', value: 'CONCLUIDA' },
  { label: 'Cancelada', value: 'CANCELADA' },
]

/** Status aceitos na URL; qualquer outro valor é ignorado. */
export const STATUS_VALIDOS = STATUS_OPCOES.map((o) => o.value).filter((v): v is StatusOrdem => v !== 'TODAS')

interface CamposFiltroOrdensProps {
  busca: string
  onBusca: (valor: string) => void
  status: StatusOrdem | 'TODAS'
  onStatus: (valor: StatusOrdem | 'TODAS') => void
}

export function CamposFiltroOrdens({ busca, onBusca, status, onStatus }: CamposFiltroOrdensProps) {
  return (
    <>
      <TextField
        label="Código da ordem"
        placeholder="Ex.: OP-2026-04"
        value={busca}
        onChange={(e) => onBusca(e.target.value)}
        sx={{ flex: 1, minWidth: 240 }}
        slotProps={{ input: { startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" color="action" /></InputAdornment> } }}
      />
      <TextField select label="Status" value={status} onChange={(e) => onStatus(e.target.value as StatusOrdem | 'TODAS')} sx={{ minWidth: 200 }}>
        {STATUS_OPCOES.map((op) => (
          <MenuItem key={op.value} value={op.value}>{op.label}</MenuItem>
        ))}
      </TextField>
    </>
  )
}
