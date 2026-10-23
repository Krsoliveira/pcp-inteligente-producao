import { useNavigate } from 'react-router-dom'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Typography from '@mui/material/Typography'
import LockOutlinedIcon from '@mui/icons-material/LockOutlined'

interface SemAcessoProps {
  /** Para onde o botão leva; sem rota, o botão não aparece (o perfil não dá acesso a nenhuma tela). */
  rotaInicial: string | null
}

/** Tela para quem abriu uma rota que o perfil dele não permite. */
export function SemAcesso({ rotaInicial }: SemAcessoProps) {
  const navigate = useNavigate()
  return (
    <Box sx={{ textAlign: 'center', py: 8 }}>
      <LockOutlinedIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
      <Typography variant="h6" gutterBottom>Você não tem acesso a esta tela</Typography>
      <Typography color="text.secondary" sx={{ maxWidth: 460, mx: 'auto', mb: 3 }}>
        {rotaInicial
          ? 'O seu perfil não inclui esta área. Se precisar dela, peça a um gestor para ajustar o seu acesso.'
          : 'O seu perfil ainda não dá acesso a nenhuma tela. Peça a um gestor para ajustar o seu acesso.'}
      </Typography>
      {rotaInicial && <Button variant="contained" onClick={() => navigate(rotaInicial)}>Ir para o início</Button>}
    </Box>
  )
}
