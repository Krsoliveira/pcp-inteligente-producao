import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Typography from '@mui/material/Typography'
import Box from '@mui/material/Box'
import type { SvgIconComponent } from '@mui/icons-material'

interface KpiCardProps {
  titulo: string
  valor: number | string
  subtitulo?: string
  Icone: SvgIconComponent
  cor?: string
}

export function KpiCard({ titulo, valor, subtitulo, Icone, cor = '#1565c0' }: KpiCardProps) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
          <Box>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              {titulo}
            </Typography>
            <Typography variant="h4" fontWeight={700} color={cor}>
              {valor}
            </Typography>
            {subtitulo && (
              <Typography variant="caption" color="text.secondary">
                {subtitulo}
              </Typography>
            )}
          </Box>
          <Box
            sx={{
              p: 1.5,
              borderRadius: 2,
              bgcolor: `${cor}18`,
              display: 'flex',
              alignItems: 'center',
            }}
          >
            <Icone sx={{ color: cor, fontSize: 28 }} />
          </Box>
        </Box>
      </CardContent>
    </Card>
  )
}
