import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import Breadcrumbs from '@mui/material/Breadcrumbs'
import Link from '@mui/material/Link'
import NavigateNextIcon from '@mui/icons-material/NavigateNext'
import { useNavigate } from 'react-router-dom'
import type { ReactNode } from 'react'

interface Breadcrumb {
  label: string
  href?: string
}

interface PageHeaderProps {
  titulo: string
  subtitulo?: string
  breadcrumbs?: Breadcrumb[]
  action?: ReactNode
}

export function PageHeader({ titulo, subtitulo, breadcrumbs, action }: PageHeaderProps) {
  const navigate = useNavigate()

  return (
    <Box
      sx={{
        mb: 3,
        display: 'flex',
        alignItems: 'flex-start',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: 2,
      }}
    >
      <Box>
        {breadcrumbs && breadcrumbs.length > 0 && (
          <Breadcrumbs
            separator={<NavigateNextIcon fontSize="inherit" />}
            sx={{ mb: 0.5, fontSize: '0.75rem' }}
          >
            {breadcrumbs.map((bc, i) =>
              bc.href && i < breadcrumbs.length - 1 ? (
                <Link
                  key={bc.label}
                  underline="hover"
                  color="text.secondary"
                  sx={{ fontSize: '0.75rem', cursor: 'pointer' }}
                  onClick={() => navigate(bc.href!)}
                >
                  {bc.label}
                </Link>
              ) : (
                <Typography key={bc.label} color="text.primary" sx={{ fontSize: '0.75rem' }}>
                  {bc.label}
                </Typography>
              )
            )}
          </Breadcrumbs>
        )}
        <Typography variant="h5">{titulo}</Typography>
        {subtitulo && (
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.25 }}>
            {subtitulo}
          </Typography>
        )}
      </Box>
      {action && <Box sx={{ flexShrink: 0 }}>{action}</Box>}
    </Box>
  )
}
