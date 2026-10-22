import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import Typography from '@mui/material/Typography'
import ArrowForwardIcon from '@mui/icons-material/ArrowForward'
import type { ReactNode } from 'react'

interface CartaoDeRecentesProps {
  titulo: string
  onVerTodas: () => void
  children: ReactNode
}

/** Os registros mais recentes de uma entidade, com atalho para ver todos. */
export function CartaoDeRecentes({ titulo, onVerTodas, children }: CartaoDeRecentesProps) {
  return (
    <Card>
      <Box sx={{ px: 2, py: 1.5, display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderBottom: '1px solid', borderColor: 'divider' }}>
        <Typography variant="subtitle2">{titulo}</Typography>
        <Button size="small" endIcon={<ArrowForwardIcon />} onClick={onVerTodas}>Ver todos</Button>
      </Box>
      {children}
    </Card>
  )
}
