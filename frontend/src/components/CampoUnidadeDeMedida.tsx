import Autocomplete from '@mui/material/Autocomplete'
import TextField from '@mui/material/TextField'
import { useQuery } from '@tanstack/react-query'
import { listarUnidades } from '../api/materiais'
import { usePermissoes } from '../hooks/usePermissoes'

interface CampoUnidadeDeMedidaProps {
  value: string
  onChange: (unidade: string) => void
  label?: string
  placeholder?: string
  helperText?: string
  /** Largura fixa, para o campo caber numa linha de tabela ou de componentes. */
  largura?: number
}

/**
 * Unidade de medida: o que a pessoa digita já aparece em maiúsculas (o servidor guarda assim, ADR-0037) e as unidades
 * já cadastradas são oferecidas como sugestão, sem repetição. Uma unidade nova continua permitida.
 */
export function CampoUnidadeDeMedida({ value, onChange, label = 'Unidade de medida', placeholder, helperText, largura }: CampoUnidadeDeMedidaProps) {
  const { tem } = usePermissoes()
  // Sem permissão de ver material o servidor recusaria a consulta: o campo segue como texto livre.
  const { data: unidades = [] } = useQuery({
    queryKey: ['unidades'],
    queryFn: listarUnidades,
    enabled: tem('MATERIAL_VISUALIZAR'),
    staleTime: 60_000,
  })

  return (
    <Autocomplete
      freeSolo
      options={unidades}
      inputValue={value}
      onInputChange={(_, texto) => onChange(texto.toUpperCase())}
      sx={largura ? { width: largura } : undefined}
      renderInput={(parametros) => (
        <TextField {...parametros} label={label} placeholder={placeholder} helperText={helperText}
          slotProps={{ ...parametros.slotProps, htmlInput: { ...parametros.slotProps.htmlInput, maxLength: 10 } }} />
      )}
    />
  )
}
