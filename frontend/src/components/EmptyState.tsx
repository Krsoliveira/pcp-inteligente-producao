import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import Button from '@mui/material/Button'
import type { SvgIconComponent } from '@mui/icons-material'

interface EmptyStateProps {
  Icone: SvgIconComponent
  titulo: string
  descricao?: string
  acaoLabel?: string
  onAcao?: () => void
}

export function EmptyState({ Icone, titulo, descricao, acaoLabel, onAcao }: EmptyStateProps) {
  return (
    <Box
      sx={{
        py: 8,
        px: 2,
        textAlign: 'center',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 1.5,
      }}
    >
      <Box
        sx={{
          width: 56,
          height: 56,
          borderRadius: '50%',
          bgcolor: 'grey.100',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          mb: 0.5,
        }}
      >
        <Icone sx={{ fontSize: 28, color: 'text.disabled' }} />
      </Box>
      <Typography variant="subtitle1" fontWeight={600} color="text.primary">
        {titulo}
      </Typography>
      {descricao && (
        <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 360 }}>
          {descricao}
        </Typography>
      )}
      {acaoLabel && onAcao && (
        <Button variant="contained" onClick={onAcao} sx={{ mt: 0.5 }}>
          {acaoLabel}
        </Button>
      )}
    </Box>
  )
}
