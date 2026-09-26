import type { ReactNode } from 'react'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Typography from '@mui/material/Typography'
import FactoryIcon from '@mui/icons-material/Factory'

interface AuthLayoutProps {
  titulo: string
  children: ReactNode
  /** Conteúdo abaixo do cartão (ex.: link para login/cadastro). */
  rodape?: ReactNode
}

/**
 * Moldura comum das telas públicas (login e cadastro): logo, cartão centralizado
 * e rodapé opcional.
 */
export function AuthLayout({ titulo, children, rodape }: AuthLayoutProps) {
  return (
    <Box
      component="main"
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'background.default',
        p: 2,
      }}
    >
      <Box sx={{ width: '100%', maxWidth: 420 }}>
        <Box sx={{ textAlign: 'center', mb: 4 }}>
          <Box
            sx={{
              display: 'inline-flex',
              p: 1.5,
              borderRadius: 3,
              bgcolor: 'primary.main',
              mb: 2,
            }}
          >
            <FactoryIcon sx={{ color: 'white', fontSize: 36 }} aria-hidden />
          </Box>
          <Typography variant="h5" component="p" fontWeight={700}>
            PCP Inteligente
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Plataforma de Produção com IA
          </Typography>
        </Box>

        <Card>
          <CardContent sx={{ p: 4 }}>
            <Typography variant="h6" component="h1" fontWeight={600} gutterBottom>
              {titulo}
            </Typography>
            {children}
          </CardContent>
        </Card>

        {rodape && (
          <Typography
            variant="body2"
            color="text.secondary"
            sx={{ display: 'block', textAlign: 'center', mt: 3 }}
          >
            {rodape}
          </Typography>
        )}
      </Box>
    </Box>
  )
}
