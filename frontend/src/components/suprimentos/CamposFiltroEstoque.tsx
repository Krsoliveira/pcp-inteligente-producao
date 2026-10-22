import FormControlLabel from '@mui/material/FormControlLabel'
import InputAdornment from '@mui/material/InputAdornment'
import MenuItem from '@mui/material/MenuItem'
import Switch from '@mui/material/Switch'
import TextField from '@mui/material/TextField'
import SearchIcon from '@mui/icons-material/Search'
import { TIPO_MATERIAL_LABEL, type TipoMaterial } from '../../types'

/** Tipos aceitos na URL; qualquer outro valor é ignorado. */
export const TIPOS_DE_ESTOQUE_VALIDOS = Object.keys(TIPO_MATERIAL_LABEL) as TipoMaterial[]

interface CamposFiltroEstoqueProps {
  busca: string
  onBusca: (valor: string) => void
  tipo: TipoMaterial | 'TODOS'
  onTipo: (valor: TipoMaterial | 'TODOS') => void
  somenteComSaldo: boolean
  onSomenteComSaldo: (valor: boolean) => void
}

export function CamposFiltroEstoque({ busca, onBusca, tipo, onTipo, somenteComSaldo, onSomenteComSaldo }: CamposFiltroEstoqueProps) {
  return (
    <>
      <TextField
        label="Código ou descrição"
        placeholder="Ex.: 110.000.001 ou aço"
        value={busca}
        onChange={(e) => onBusca(e.target.value)}
        sx={{ flex: 1, minWidth: 260 }}
        slotProps={{ input: { startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" color="action" /></InputAdornment> } }}
      />
      <TextField select label="Tipo" value={tipo} onChange={(e) => onTipo(e.target.value as TipoMaterial | 'TODOS')} sx={{ minWidth: 200 }}>
        <MenuItem value="TODOS">Todos os tipos</MenuItem>
        {TIPOS_DE_ESTOQUE_VALIDOS.map((t) => (
          <MenuItem key={t} value={t}>{TIPO_MATERIAL_LABEL[t]}</MenuItem>
        ))}
      </TextField>
      <FormControlLabel
        control={<Switch checked={somenteComSaldo} onChange={(e) => onSomenteComSaldo(e.target.checked)} />}
        label="Só com saldo"
        sx={{ alignSelf: 'center' }}
      />
    </>
  )
}
