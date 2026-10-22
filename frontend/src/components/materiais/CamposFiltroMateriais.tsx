import InputAdornment from '@mui/material/InputAdornment'
import MenuItem from '@mui/material/MenuItem'
import TextField from '@mui/material/TextField'
import SearchIcon from '@mui/icons-material/Search'
import { TIPO_MATERIAL_LABEL, type TipoMaterial } from '../../types'

/** Tipos aceitos na URL; qualquer outro valor é ignorado. */
export const TIPOS_VALIDOS = Object.keys(TIPO_MATERIAL_LABEL) as TipoMaterial[]

interface CamposFiltroMateriaisProps {
  busca: string
  onBusca: (valor: string) => void
  tipo: TipoMaterial | 'TODOS'
  onTipo: (valor: TipoMaterial | 'TODOS') => void
}

export function CamposFiltroMateriais({ busca, onBusca, tipo, onTipo }: CamposFiltroMateriaisProps) {
  return (
    <>
      <TextField
        label="Código ou descrição"
        placeholder="Ex.: 103.000.001 ou viga"
        value={busca}
        onChange={(e) => onBusca(e.target.value)}
        sx={{ flex: 1, minWidth: 260 }}
        slotProps={{ input: { startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" color="action" /></InputAdornment> } }}
      />
      <TextField select label="Tipo" value={tipo} onChange={(e) => onTipo(e.target.value as TipoMaterial | 'TODOS')} sx={{ minWidth: 200 }}>
        <MenuItem value="TODOS">Todos os tipos</MenuItem>
        {TIPOS_VALIDOS.map((t) => (
          <MenuItem key={t} value={t}>{TIPO_MATERIAL_LABEL[t]}</MenuItem>
        ))}
      </TextField>
    </>
  )
}
