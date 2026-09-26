import { useState } from 'react'
import { Link as RouterLink, useNavigate } from 'react-router-dom'
import Box from '@mui/material/Box'
import TextField from '@mui/material/TextField'
import Button from '@mui/material/Button'
import Alert from '@mui/material/Alert'
import Link from '@mui/material/Link'
import InputAdornment from '@mui/material/InputAdornment'
import EmailIcon from '@mui/icons-material/Email'
import { useMutation } from '@tanstack/react-query'
import axios from 'axios'
import { login } from '../api/auth'
import { useAuthStore } from '../store/authStore'
import { AuthLayout } from '../components/auth/AuthLayout'
import { CampoSenha } from '../components/auth/CampoSenha'

export function LoginPage() {
  const navigate = useNavigate()
  const loginStore = useAuthStore((s) => s.login)
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
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
    <AuthLayout
      titulo="Entrar na plataforma"
      rodape={
        <>
          Não tem conta?{' '}
          <Link component={RouterLink} to="/cadastro">
            Criar conta
          </Link>
        </>
      }
    >
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
        <CampoSenha
          label="Senha"
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
          autoComplete="current-password"
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
    </AuthLayout>
  )
}
