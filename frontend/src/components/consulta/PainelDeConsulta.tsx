import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import Typography from '@mui/material/Typography'
import SearchIcon from '@mui/icons-material/Search'
import type { ReactNode } from 'react'

interface PainelDeConsultaProps {
  titulo?: string
  /** Os campos de filtro de cada tela. */
  children: ReactNode
  onPesquisar: () => void
  onLimpar: () => void
}

/** "Tela de seleção": os campos de filtro e os botões Pesquisar/Limpar. Enter também pesquisa. */
export function PainelDeConsulta({ titulo = 'Consultar', children, onPesquisar, onLimpar }: PainelDeConsultaProps) {
  return (
    <Card sx={{ mb: 2 }}>
      <Box
        component="form"
        onSubmit={(e) => { e.preventDefault(); onPesquisar() }}
        sx={{ p: 2 }}
      >
        <Typography variant="subtitle2" sx={{ mb: 1.5 }}>{titulo}</Typography>
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'flex-start' }}>
          {children}
          <Box sx={{ display: 'flex', gap: 1, alignSelf: 'center' }}>
            <Button type="submit" variant="contained" startIcon={<SearchIcon />}>Pesquisar</Button>
            <Button type="button" color="inherit" onClick={onLimpar}>Limpar</Button>
          </Box>
        </Box>
      </Box>
    </Card>
  )
}
