import { useState } from 'react'
import Box from '@mui/material/Box'
import InputAdornment from '@mui/material/InputAdornment'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import FilterListIcon from '@mui/icons-material/FilterList'

/**
 * Subfiltro: refina, no próprio navegador, as linhas que já estão na tela (a página atual do
 * resultado). Não consulta o servidor. `textoDe` diz em quais textos de cada linha procurar.
 */
export function useSubfiltro<T>(itens: T[], textoDe: (item: T) => string) {
  const [texto, setTexto] = useState('')
  const termo = texto.trim().toLowerCase()
  const filtrados = termo === '' ? itens : itens.filter((item) => textoDe(item).toLowerCase().includes(termo))
  return { texto, setTexto, filtrados }
}

interface SubfiltroTabelaProps {
  valor: string
  onChange: (valor: string) => void
  /** Linhas visíveis depois do subfiltro. */
  exibidas: number
  /** Linhas da página atual, antes do subfiltro. */
  naPagina: number
}

export function SubfiltroTabela({ valor, onChange, exibidas, naPagina }: SubfiltroTabelaProps) {
  return (
    <Box sx={{ px: 2, py: 1.5, display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap', borderBottom: '1px solid', borderColor: 'divider' }}>
      <TextField
        size="small"
        value={valor}
        onChange={(e) => onChange(e.target.value)}
        placeholder="Filtrar nas linhas desta página…"
        sx={{ minWidth: 280 }}
        slotProps={{ input: { startAdornment: <InputAdornment position="start"><FilterListIcon fontSize="small" color="action" /></InputAdornment> } }}
      />
      <Typography variant="caption" color="text.secondary">
        {valor.trim() === '' ? `${naPagina} linhas nesta página` : `Mostrando ${exibidas} de ${naPagina} linhas desta página`}
      </Typography>
    </Box>
  )
}
