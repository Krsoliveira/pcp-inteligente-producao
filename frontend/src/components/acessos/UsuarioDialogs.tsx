import { useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogContentText from '@mui/material/DialogContentText'
import DialogTitle from '@mui/material/DialogTitle'
import MenuItem from '@mui/material/MenuItem'
import TextField from '@mui/material/TextField'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { atribuirPerfil, criarUsuario, redefinirSenha } from '../../api/acessos'
import type { PerfilAcesso, Permissao, UsuarioAcesso } from '../../types'
import { mensagemDeErroDaApi } from '../../utils/mensagensDeErro'

/** Perfis que quem usa a tela pode conceder: só se concede o que se tem (ADR-0020). */
export function perfisConcediveis(perfis: PerfilAcesso[], permissoesDoUsuario: Set<Permissao>): PerfilAcesso[] {
  return perfis.filter((p) => p.permissoes.every((x) => permissoesDoUsuario.has(x)))
}

interface NovoUsuarioDialogProps {
  aberto: boolean
  onFechar: () => void
  perfis: PerfilAcesso[]
  permissoesDoUsuario: Set<Permissao>
}

/** O gestor cria um usuário já com o perfil e define a senha provisória. */
export function NovoUsuarioDialog({ aberto, onFechar, perfis, permissoesDoUsuario }: NovoUsuarioDialogProps) {
  const queryClient = useQueryClient()
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [perfilId, setPerfilId] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const concediveis = perfisConcediveis(perfis, permissoesDoUsuario)

  useEffect(() => {
    if (!aberto) return
    setNome(''); setEmail(''); setSenha(''); setPerfilId(''); setErro(null)
  }, [aberto])

  const { mutate, isPending } = useMutation({
    mutationFn: () => criarUsuario({ nome: nome.trim(), email: email.trim(), senhaProvisoria: senha, perfilId }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['acessos'] })
      onFechar()
    },
    onError: (e: unknown) => setErro(mensagemDeErroDaApi(e, 'Não foi possível criar o usuário. Confira os dados e tente novamente.')),
  })

  const salvar = () => {
    if (!nome.trim() || !email.trim() || !perfilId) { setErro('Preencha o nome, o e-mail e o perfil.'); return }
    if (senha.length < 8) { setErro('A senha provisória deve ter no mínimo 8 caracteres.'); return }
    if (new TextEncoder().encode(senha).length > 72) { setErro('A senha provisória é longa demais. Use uma mais curta.'); return }
    mutate()
  }

  return (
    <Dialog open={aberto} onClose={isPending ? undefined : onFechar} fullWidth maxWidth="sm">
      <DialogTitle>Novo usuário</DialogTitle>
      <DialogContent dividers>
        {erro && <Alert severity="error" sx={{ mb: 2 }}>{erro}</Alert>}
        <TextField label="Nome *" value={nome} onChange={(e) => setNome(e.target.value)} fullWidth sx={{ mb: 2 }}
          slotProps={{ htmlInput: { maxLength: 100 } }} />
        <TextField label="E-mail *" type="email" value={email} onChange={(e) => setEmail(e.target.value)} fullWidth sx={{ mb: 2 }}
          slotProps={{ htmlInput: { maxLength: 150 } }} />
        <TextField label="Senha provisória *" type="password" value={senha} onChange={(e) => setSenha(e.target.value)} fullWidth sx={{ mb: 2 }}
          autoComplete="new-password"
          helperText="Mínimo de 8 caracteres. Informe-a ao usuário por um canal seguro: ele terá de trocá-la no primeiro acesso." />
        <TextField select label="Perfil *" value={perfilId} onChange={(e) => setPerfilId(e.target.value)} fullWidth
          helperText={concediveis.length < perfis.length ? 'Só aparecem os perfis que você pode conceder (os que não passam do seu acesso).' : undefined}>
          {concediveis.map((p) => <MenuItem key={p.id} value={p.id}>{p.nome}</MenuItem>)}
        </TextField>
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onFechar} disabled={isPending}>Cancelar</Button>
        <Button variant="contained" onClick={salvar} disabled={isPending}>{isPending ? 'Criando…' : 'Criar usuário'}</Button>
      </DialogActions>
    </Dialog>
  )
}

interface TrocarPerfilDialogProps {
  /** Usuário a alterar; `null` mantém o diálogo fechado. */
  usuario: UsuarioAcesso | null
  onFechar: () => void
  perfis: PerfilAcesso[]
  permissoesDoUsuario: Set<Permissao>
}

/** Troca o perfil de um usuário; a troca entra no histórico de alterações. */
export function TrocarPerfilDialog({ usuario, onFechar, perfis, permissoesDoUsuario }: TrocarPerfilDialogProps) {
  const queryClient = useQueryClient()
  const [perfilId, setPerfilId] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const concediveis = perfisConcediveis(perfis, permissoesDoUsuario)

  useEffect(() => {
    setPerfilId(usuario?.perfilId ?? '')
    setErro(null)
  }, [usuario?.id]) // eslint-disable-line react-hooks/exhaustive-deps

  const { mutate, isPending } = useMutation({
    mutationFn: () => atribuirPerfil(usuario!.id, perfilId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['acessos'] })
      onFechar()
    },
    onError: (e: unknown) => setErro(mensagemDeErroDaApi(e, 'Não foi possível trocar o perfil.')),
  })

  return (
    <Dialog open={usuario !== null} onClose={isPending ? undefined : onFechar} fullWidth maxWidth="xs">
      <DialogTitle>Alterar o perfil de {usuario?.nome}</DialogTitle>
      <DialogContent>
        {erro && <Alert severity="error" sx={{ mb: 2 }}>{erro}</Alert>}
        <DialogContentText sx={{ mb: 2 }}>
          {usuario?.email}. A mudança vale na próxima ação do usuário e fica registrada no histórico.
        </DialogContentText>
        <TextField select label="Perfil" value={perfilId} onChange={(e) => setPerfilId(e.target.value)} fullWidth>
          {concediveis.map((p) => <MenuItem key={p.id} value={p.id}>{p.nome}</MenuItem>)}
        </TextField>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onFechar} disabled={isPending}>Cancelar</Button>
        <Button variant="contained" onClick={() => mutate()} disabled={isPending || !perfilId || perfilId === usuario?.perfilId}>
          {isPending ? 'Salvando…' : 'Salvar'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

interface RedefinirSenhaDialogProps {
  /** Usuário cuja senha será redefinida; `null` mantém o diálogo fechado. */
  usuario: UsuarioAcesso | null
  onFechar: () => void
}

/** O gestor define uma senha provisória nova (quem esqueceu a senha); o usuário a troca no próximo acesso. */
export function RedefinirSenhaDialog({ usuario, onFechar }: RedefinirSenhaDialogProps) {
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    setSenha('')
    setErro(null)
  }, [usuario?.id])

  const { mutate, isPending } = useMutation({
    mutationFn: () => redefinirSenha(usuario!.id, senha),
    onSuccess: onFechar,
    onError: (e: unknown) => setErro(mensagemDeErroDaApi(e, 'Não foi possível redefinir a senha.')),
  })

  const salvar = () => {
    if (senha.length < 8) { setErro('A senha provisória deve ter no mínimo 8 caracteres.'); return }
    if (new TextEncoder().encode(senha).length > 72) { setErro('A senha provisória é longa demais. Use uma mais curta.'); return }
    mutate()
  }

  return (
    <Dialog open={usuario !== null} onClose={isPending ? undefined : onFechar} fullWidth maxWidth="xs">
      <DialogTitle>Redefinir a senha de {usuario?.nome}</DialogTitle>
      <DialogContent>
        {erro && <Alert severity="error" sx={{ mb: 2 }}>{erro}</Alert>}
        <DialogContentText sx={{ mb: 2 }}>
          {usuario?.email}. A senha atual deixa de valer agora, e a nova é provisória: o usuário precisa trocá-la ao
          entrar. A redefinição fica registrada no histórico.
        </DialogContentText>
        <TextField label="Senha provisória nova" type="password" value={senha} onChange={(e) => setSenha(e.target.value)} fullWidth
          autoComplete="new-password" helperText="Mínimo de 8 caracteres. Informe-a ao usuário por um canal seguro." />
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onFechar} disabled={isPending}>Cancelar</Button>
        <Button color="warning" variant="contained" onClick={salvar} disabled={isPending}>
          {isPending ? 'Redefinindo…' : 'Redefinir senha'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
