import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import TextField from '@mui/material/TextField'
import Button from '@mui/material/Button'
import Typography from '@mui/material/Typography'
import Alert from '@mui/material/Alert'
import InputAdornment from '@mui/material/InputAdornment'
import IconButton from '@mui/material/IconButton'
import EmailIcon from '@mui/icons-material/Email'
import LockIcon from '@mui/icons-material/Lock'
import VisibilityIcon from '@mui/icons-material/Visibility'
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff'
import FactoryIcon from '@mui/icons-material/Factory'
import { useMutation } from '@tanstack/react-query'
import axios from 'axios'
import { login } from '../api/auth'
import { useAuthStore } from '../store/authStore'

export function LoginPage() {
  const navigate = useNavigate()
  const loginStore = useAuthStore((s) => s.login)
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [mostrarSenha, setMostrarSenha] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  const { mutate, isPending } = useMutation({
    mutationFn: login,
    onSuccess: (data) => {
      loginStore(data.token)
      navigate('/dashboard')
    },
    onError: (error: unknown) => {
      if (axios.isAxiosError(error) && error.response?.status === 401) {
        setErro('E-mail ou senha incorretos. Tente novamente.')
      } else {
        setErro('Erro ao conectar com o servidor. Verifique se o backend está rodando.')
      }
    },
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setErro(null)
    if (!email || !senha) {
      setErro('Preencha e-mail e senha.')
      return
    }
    mutate({ email, senha })
  }

  return (
    <Box
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
        {/* Logo */}
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
            <FactoryIcon sx={{ color: 'white', fontSize: 36 }} />
          </Box>
          <Typography variant="h5" fontWeight={700}>
            PCP Inteligente
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Plataforma de Produção com IA
          </Typography>
        </Box>

        <Card>
          <CardContent sx={{ p: 4 }}>
            <Typography variant="h6" fontWeight={600} gutterBottom>
              Entrar na plataforma
            </Typography>

            {erro && (
              <Alert severity="error" sx={{ mb: 2 }} onClose={() => setErro(null)}>
                {erro}
              </Alert>
            )}

            <Box component="form" onSubmit={handleSubmit} noValidate>
              <TextField
                label="E-mail"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                fullWidth
                margin="normal"
                autoComplete="email"
                autoFocus
                slotProps={{
                  input: {
                    startAdornment: (
                      <InputAdornment position="start">
                        <EmailIcon color="action" fontSize="small" />
                      </InputAdornment>
                    ),
                  },
                }}
              />
              <TextField
                label="Senha"
                type={mostrarSenha ? 'text' : 'password'}
                value={senha}
                onChange={(e) => setSenha(e.target.value)}
                fullWidth
                margin="normal"
                autoComplete="current-password"
                slotProps={{
                  input: {
                    startAdornment: (
                      <InputAdornment position="start">
                        <LockIcon color="action" fontSize="small" />
                      </InputAdornment>
                    ),
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton
                          onClick={() => setMostrarSenha(!mostrarSenha)}
                          edge="end"
                          size="small"
                        >
                          {mostrarSenha ? <VisibilityOffIcon /> : <VisibilityIcon />}
                        </IconButton>
                      </InputAdornment>
                    ),
                  },
                }}
              />
              <Button
                type="submit"
                variant="contained"
                fullWidth
                size="large"
                disabled={isPending}
                sx={{ mt: 3, mb: 1 }}
              >
                {isPending ? 'Entrando…' : 'Entrar'}
              </Button>
            </Box>
          </CardContent>
        </Card>

        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', textAlign: 'center', mt: 3 }}>
          Não tem conta? Registre-se via API em{' '}
          <code>/api/v1/auth/registrar</code>
        </Typography>
      </Box>
    </Box>
  )
}
