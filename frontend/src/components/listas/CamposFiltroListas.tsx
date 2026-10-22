import Autocomplete from '@mui/material/Autocomplete'
import MenuItem from '@mui/material/MenuItem'
import TextField from '@mui/material/TextField'
import type { Material, StatusListaTecnica } from '../../types'
import { formatarCodigoMaterial } from '../../utils/formatacao'

const STATUS_OPCOES: Array<{ label: string; value: StatusListaTecnica | 'TODOS' }> = [
  { label: 'Todos os status', value: 'TODOS' },
  { label: 'Em revisão', value: 'EM_REVISAO' },
  { label: 'Ativa', value: 'ATIVA' },
  { label: 'Obsoleta', value: 'OBSOLETA' },
]

/** Status aceitos na URL; qualquer outro valor é ignorado. */
export const STATUS_LISTA_VALIDOS = STATUS_OPCOES.map((o) => o.value).filter((v): v is StatusListaTecnica => v !== 'TODOS')

export const nomeDoMaterial = (m: Material) => `${formatarCodigoMaterial(m.codigo)} — ${m.descricao}`

interface CamposFiltroListasProps {
  /** Materiais que podem ter lista técnica (todos, menos matérias-primas). */
  materiais: Material[]
  materialId: string
  onMaterial: (id: string) => void
  status: StatusListaTecnica | 'TODOS'
  onStatus: (valor: StatusListaTecnica | 'TODOS') => void
}

export function CamposFiltroListas({ materiais, materialId, onMaterial, status, onStatus }: CamposFiltroListasProps) {
  return (
    <>
      <Autocomplete
        options={materiais}
        value={materiais.find((m) => m.id === materialId) ?? null}
        onChange={(_, escolhido) => onMaterial(escolhido?.id ?? '')}
        getOptionLabel={nomeDoMaterial}
        isOptionEqualToValue={(a, b) => a.id === b.id}
        noOptionsText="Nenhum material encontrado"
        sx={{ flex: 1, minWidth: 300 }}
        renderInput={(parametros) => <TextField {...parametros} label="Material" placeholder="Digite o código ou a descrição" />}
      />
      <TextField select label="Status" value={status} onChange={(e) => onStatus(e.target.value as StatusListaTecnica | 'TODOS')} sx={{ minWidth: 200 }}>
        {STATUS_OPCOES.map((op) => (
          <MenuItem key={op.value} value={op.value}>{op.label}</MenuItem>
        ))}
      </TextField>
    </>
  )
}
