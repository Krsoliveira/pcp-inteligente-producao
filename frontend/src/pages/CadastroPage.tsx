import { useState } from 'react'
import { Link as RouterLink, useNavigate } from 'react-router-dom'
import Box from '@mui/material/Box'
import TextField from '@mui/material/TextField'
import Button from '@mui/material/Button'
import Alert from '@mui/material/Alert'
import Link from '@mui/material/Link'
import InputAdornment from '@mui/material/InputAdornment'
import EmailIcon from '@mui/icons-material/Email'
import PersonIcon from '@mui/icons-material/Person'
import { useMutation } from '@tanstack/react-query'
import axios from 'axios'
import { login, registrar } from '../api/auth'
import { useAuthStore } from '../store/authStore'
import { AuthLayout } from '../components/auth/AuthLayout'
import { CampoSenha } from '../components/auth/CampoSenha'
import type { ApiError, RegistrarRequest } from '../types'

type Campo = 'nome' | 'email' | 'senha' | 'confirmacao'
type ErrosDeCampo = Partial<Record<Campo, string>>

const SENHA_MINIMA = 8
const EMAIL_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** Mesmas regras do backend (RegistrarRequest), para dar feedback antes do envio. */
function validar(dados: RegistrarRequest & { confirmacao: string }): ErrosDeCampo {
  const erros: ErrosDeCampo = {}
  if (!dados.nome.trim()) erros.nome = 'Informe seu nome.'
  else if (dados.nome.trim().length > 100) erros.nome = 'Máximo de 100 caracteres.'

  if (!dados.email.trim()) erros.email = 'Informe seu e-mail.'
  else if (!EMAIL_VALIDO.test(dados.email.trim())) erros.email = 'E-mail inválido.'
  else if (dados.email.trim().length > 150) erros.email = 'Máximo de 150 caracteres.'

  if (dados.senha.length < SENHA_MINIMA) erros.senha = `Mínimo de ${SENHA_MINIMA} caracteres.`
  if (dados.confirmacao !== dados.senha) erros.confirmacao = 'As senhas não conferem.'
  return erros
}

export function CadastroPage() {
  const navigate = useNavigate()
  const loginStore = useAuthStore((s) => s.login)
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [errosDeCampo, setErrosDeCampo] = useState<ErrosDeCampo>({})
  const [erro, setErro] = useState<string | null>(null)
  const [emailDuplicado, setEmailDuplicado] = useState(false)

  // Cadastro + login em sequência: o usuário cai direto no dashboard.
  const { mutate, isPending } = useMutation({
    mutationFn: async (dados: RegistrarRequest) => {
      await registrar(dados)
      return login({ email: dados.email, senha: dados.senha })
    },
    onSuccess: (data) => {
      loginStore(data.token)
      navigate('/dashboard', { replace: true })
    },
    onError: (error: unknown) => {
      if (!axios.isAxiosError<ApiError>(error) || !error.response) {
        setErro('Erro ao conectar com o servidor. Verifique se o backend está rodando.')
        return
      }
      const { status, data } = error.response
      if (status === 409) {
        setEmailDuplicado(true)
        setErrosDeCampo({ email: 'Este e-mail já está cadastrado.' })
      } else if (status === 400 && data?.erros) {
        setErrosDeCampo(data.erros as ErrosDeCampo)
      } else {
        setErro(data?.detail ?? 'Não foi possível concluir o cadastro. Tente novamente.')
      }
    },
  })

  const limparErro = (campo: Campo) => {
    if (errosDeCampo[campo]) setErrosDeCampo((atuais) => ({ ...atuais, [campo]: undefined }))
    if (campo === 'email') setEmailDuplicado(false)
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setErro(null)
    setEmailDuplicado(false)
    const dados = { nome: nome.trim(), email: email.trim(), senha }
    const erros = validar({ ...dados, confirmacao })
    setErrosDeCampo(erros)
    if (Object.keys(erros).length === 0) mutate(dados)
  }

  return (
    <AuthLayout
      titulo="Criar conta"
      rodape={
        <>
          Já tem conta?{' '}
          <Link component={RouterLink} to="/login">
            Entrar
          </Link>
        </>
      }
    >
      {erro && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setErro(null)}>
          {erro}
        </Alert>
      )}
      {emailDuplicado && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Esse e-mail já tem conta.{' '}
          <Link component={RouterLink} to="/login">
            Entrar com ele
          </Link>
        </Alert>
      )}

      <Box component="form" onSubmit={handleSubmit} noValidate>
        <TextField
          label="Nome"
          value={nome}
          onChange={(e) => {
            setNome(e.target.value)
            limparErro('nome')
          }}
          error={!!errosDeCampo.nome}
          helperText={errosDeCampo.nome}
          fullWidth
          margin="normal"
          autoComplete="name"
          autoFocus
          required
          slotProps={{
            input: {
              startAdornment: (
                <InputAdornment position="start">
                  <PersonIcon color="action" fontSize="small" />
                </InputAdornment>
              ),
            },
          }}
        />
        <TextField
          label="E-mail"
          type="email"
          value={email}
          onChange={(e) => {
            setEmail(e.target.value)
            limparErro('email')
          }}
          error={!!errosDeCampo.email}
          helperText={errosDeCampo.email}
          fullWidth
          margin="normal"
          autoComplete="email"
          required
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
          onChange={(e) => {
            setSenha(e.target.value)
            limparErro('senha')
          }}
          error={!!errosDeCampo.senha}
          helperText={errosDeCampo.senha ?? `Mínimo de ${SENHA_MINIMA} caracteres.`}
          autoComplete="new-password"
          required
        />
        <CampoSenha
          label="Confirmar senha"
          value={confirmacao}
          onChange={(e) => {
            setConfirmacao(e.target.value)
            limparErro('confirmacao')
          }}
          error={!!errosDeCampo.confirmacao}
          helperText={errosDeCampo.confirmacao}
          autoComplete="new-password"
          required
        />

        <Alert severity="info" variant="outlined" sx={{ mt: 2 }}>
          Novas contas são criadas com o perfil <strong>Planejador</strong>.
        </Alert>

        <Button
          type="submit"
          variant="contained"
          fullWidth
          size="large"
          disabled={isPending}
          sx={{ mt: 3, mb: 1 }}
        >
          {isPending ? 'Criando conta…' : 'Criar conta'}
        </Button>
      </Box>
    </AuthLayout>
  )
}
