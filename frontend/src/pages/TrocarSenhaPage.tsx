import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { trocarMinhaSenha } from '../api/acessos'
import { PageHeader } from '../components/PageHeader'
import { CHAVE_USUARIO_LOGADO, usePermissoes } from '../hooks/usePermissoes'
import { mensagemDeErroDaApi } from '../utils/mensagensDeErro'

const MINIMO = 8
const MAXIMO_DE_BYTES = 72

/** Primeira mensagem de erro do formulário, ou `null` se estiver tudo certo. */
export function validarNovaSenha(atual: string, nova: string, confirmacao: string): string | null {
  if (!atual) return 'Informe a senha atual.'
  if (nova.length < MINIMO) return `A nova senha deve ter no mínimo ${MINIMO} caracteres.`
  if (new TextEncoder().encode(nova).length > MAXIMO_DE_BYTES) return 'A nova senha é longa demais. Use uma senha mais curta.'
  if (nova === atual) return 'A nova senha deve ser diferente da atual.'
  if (nova !== confirmacao) return 'A confirmação não é igual à nova senha.'
  return null
}

/**
 * Troca de senha (ADR-0021). No primeiro acesso é obrigatória: o usuário entrou com a senha provisória
 * que um gestor definiu e só pode trocá-la. Depois serve como "Alterar minha senha".
 */
export function TrocarSenhaPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { usuario } = usePermissoes()
  const obrigatoria = !!usuario?.deveTrocarSenha

  const [atual, setAtual] = useState('')
  const [nova, setNova] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [trocou, setTrocou] = useState(false)

  const { mutate, isPending } = useMutation({
    mutationFn: () => trocarMinhaSenha(atual, nova),
    onSuccess: async () => {
      setErro(null)
      await queryClient.invalidateQueries({ queryKey: CHAVE_USUARIO_LOGADO })
      if (obrigatoria) {
        navigate('/', { replace: true })
      } else {
        setTrocou(true)
        setAtual(''); setNova(''); setConfirmacao('')
      }
    },
    onError: (e: unknown) => setErro(mensagemDeErroDaApi(e, 'Não foi possível trocar a senha. Tente novamente.')),
  })

  const enviar = (evento: React.FormEvent) => {
    evento.preventDefault()
    setTrocou(false)
    const problema = validarNovaSenha(atual, nova, confirmacao)
    if (problema) { setErro(problema); return }
    setErro(null)
    mutate()
  }

  return (
    <Box sx={{ maxWidth: 520 }}>
      <PageHeader
        titulo={obrigatoria ? 'Defina a sua senha' : 'Alterar minha senha'}
        subtitulo={obrigatoria ? 'Para continuar, troque a senha provisória por uma que só você conheça' : undefined}
        breadcrumbs={obrigatoria ? undefined : [{ label: 'Minha conta' }, { label: 'Alterar senha' }]}
      />

      {obrigatoria && (
        <Alert severity="info" sx={{ mb: 2 }}>
          A senha que você recebeu é provisória e é conhecida por quem criou a sua conta. Escolha uma nova; depois
          disso o sistema é liberado.
        </Alert>
      )}
      {trocou && <Alert severity="success" sx={{ mb: 2 }}>Senha alterada.</Alert>}
      {erro && <Alert severity="error" sx={{ mb: 2 }}>{erro}</Alert>}

      <Card>
        <CardContent sx={{ p: 3 }}>
          <Box component="form" onSubmit={enviar} noValidate sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField
              label={obrigatoria ? 'Senha provisória (a que você recebeu)' : 'Senha atual'}
              type="password" value={atual} onChange={(e) => setAtual(e.target.value)} autoComplete="current-password" fullWidth
            />
            <TextField
              label="Nova senha" type="password" value={nova} onChange={(e) => setNova(e.target.value)} autoComplete="new-password" fullWidth
              helperText={`No mínimo ${MINIMO} caracteres e diferente da atual.`}
            />
            <TextField
              label="Confirmar nova senha" type="password" value={confirmacao} onChange={(e) => setConfirmacao(e.target.value)}
              autoComplete="new-password" fullWidth
            />
            <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1, mt: 1 }}>
              <Typography
                variant="caption"
                sx={{ color: 'text.secondary', alignSelf: 'center', mr: 'auto' }}>
                Sua senha nunca aparece no histórico do sistema.
              </Typography>
              <Button type="submit" variant="contained" disabled={isPending}>
                {isPending ? 'Salvando…' : obrigatoria ? 'Definir senha' : 'Alterar senha'}
              </Button>
            </Box>
          </Box>
        </CardContent>
      </Card>
    </Box>
  )
}
