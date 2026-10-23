import { useEffect, useMemo, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Checkbox from '@mui/material/Checkbox'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Divider from '@mui/material/Divider'
import FormControlLabel from '@mui/material/FormControlLabel'
import TextField from '@mui/material/TextField'
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { atualizarPerfil, criarPerfil } from '../../api/acessos'
import { CHAVE_USUARIO_LOGADO } from '../../hooks/usePermissoes'
import type { PerfilAcesso, Permissao, PermissaoDoCatalogo } from '../../types'
import { mensagemDeErroDaApi } from '../../utils/mensagensDeErro'
import { HistoricoAuditoria } from '../HistoricoAuditoria'

interface PerfilDialogProps {
  aberto: boolean
  onFechar: () => void
  /** Perfil a editar; sem ele, cria um novo. */
  perfil?: PerfilAcesso | null
  catalogo: PermissaoDoCatalogo[]
  /** O que quem está usando a tela possui: só se concede o que se tem (ADR-0020). */
  permissoesDoUsuario: Set<Permissao>
  /** Pode criar e alterar perfis. Sem isso o diálogo só mostra. */
  podeGerenciar: boolean
  /** Pode ver o histórico de alterações do perfil. */
  podeVerHistorico: boolean
}

/** Agrupa o catálogo por área, mantendo a ordem em que o servidor o devolve. */
function porArea(catalogo: PermissaoDoCatalogo[]): Array<[string, PermissaoDoCatalogo[]]> {
  const grupos = new Map<string, PermissaoDoCatalogo[]>()
  catalogo.forEach((p) => grupos.set(p.area, [...(grupos.get(p.area) ?? []), p]))
  return [...grupos.entries()]
}

/** Cria ou edita um perfil de acesso: nome, descrição e as caixinhas de permissões por área. */
export function PerfilDialog({ aberto, onFechar, perfil, catalogo, permissoesDoUsuario, podeGerenciar, podeVerHistorico }: PerfilDialogProps) {
  const queryClient = useQueryClient()
  const [nome, setNome] = useState('')
  const [descricao, setDescricao] = useState('')
  const [marcadas, setMarcadas] = useState<Set<Permissao>>(new Set())
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    if (!aberto) return
    setErro(null)
    setNome(perfil?.nome ?? '')
    setDescricao(perfil?.descricao ?? '')
    setMarcadas(new Set(perfil?.permissoes ?? []))
  }, [aberto, perfil?.id]) // eslint-disable-line react-hooks/exhaustive-deps

  const grupos = useMemo(() => porArea(catalogo), [catalogo])

  const deSistemaAdministrador = !!perfil?.administrador
  // Quem não tem todas as permissões do perfil não pode mexer nele (o servidor recusa).
  const acimaDoAlcance = !!perfil && !perfil.administrador && perfil.permissoes.some((p) => !permissoesDoUsuario.has(p))
  const somenteLeitura = !podeGerenciar || deSistemaAdministrador || acimaDoAlcance

  const { mutate, isPending } = useMutation({
    mutationFn: () => {
      const payload = { nome: nome.trim(), descricao: descricao.trim(), permissoes: [...marcadas] }
      return perfil ? atualizarPerfil(perfil.id, payload) : criarPerfil(payload)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['acessos'] })
      queryClient.invalidateQueries({ queryKey: CHAVE_USUARIO_LOGADO })
      onFechar()
    },
    onError: (e: unknown) => setErro(mensagemDeErroDaApi(e, 'Não foi possível salvar o perfil. Tente novamente.')),
  })

  const alternar = (permissao: Permissao) =>
    setMarcadas((atual) => {
      const novo = new Set(atual)
      if (novo.has(permissao)) novo.delete(permissao)
      else novo.add(permissao)
      return novo
    })

  const alternarArea = (itens: PermissaoDoCatalogo[], marcar: boolean) =>
    setMarcadas((atual) => {
      const novo = new Set(atual)
      itens.filter((i) => permissoesDoUsuario.has(i.permissao)).forEach((i) => (marcar ? novo.add(i.permissao) : novo.delete(i.permissao)))
      return novo
    })

  const salvar = () => {
    if (!nome.trim()) { setErro('Informe o nome do perfil.'); return }
    if (marcadas.size === 0) { setErro('Marque ao menos uma permissão.'); return }
    mutate()
  }

  const titulo = !perfil ? 'Novo perfil de acesso' : somenteLeitura ? `Perfil ${perfil.nome}` : `Editar perfil ${perfil.nome}`

  return (
    <Dialog open={aberto} onClose={isPending ? undefined : onFechar} fullWidth maxWidth="md">
      <DialogTitle>{titulo}</DialogTitle>
      <DialogContent dividers>
        {erro && <Alert severity="error" sx={{ mb: 2 }}>{erro}</Alert>}
        {deSistemaAdministrador && (
          <Alert severity="info" sx={{ mb: 2 }}>
            O Administrador é um perfil de sistema: tem todas as permissões, sempre, e não pode ser alterado nem excluído.
          </Alert>
        )}
        {acimaDoAlcance && (
          <Alert severity="warning" sx={{ mb: 2 }}>
            Este perfil tem permissões que você não possui, então você só pode consultá-lo. Só se concede o que se tem.
          </Alert>
        )}
        {perfil?.padraoDoAutocadastro && (
          <Alert severity="info" sx={{ mb: 2 }}>
            É o perfil de quem se cadastra sozinho. Ele não pode ser excluído; as permissões podem ser ajustadas.
          </Alert>
        )}

        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 2fr' }, gap: 2 }}>
          <TextField label="Nome *" value={nome} onChange={(e) => setNome(e.target.value)} disabled={somenteLeitura}
            slotProps={{ htmlInput: { maxLength: 60 } }} />
          <TextField label="Descrição" value={descricao} onChange={(e) => setDescricao(e.target.value)} disabled={somenteLeitura}
            slotProps={{ htmlInput: { maxLength: 200 } }} />
        </Box>

        <Divider sx={{ my: 2.5 }} />
        <Typography variant="subtitle2" sx={{ mb: 0.5 }}>Permissões</Typography>
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1.5 }}>
          Marque o que quem tiver este perfil poderá fazer. Você só pode marcar o que você mesmo possui.
        </Typography>

        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 }}>
          {grupos.map(([area, itens]) => {
            const concediveis = itens.filter((i) => permissoesDoUsuario.has(i.permissao))
            const todasMarcadas = concediveis.length > 0 && concediveis.every((i) => marcadas.has(i.permissao))
            const algumaMarcada = itens.some((i) => marcadas.has(i.permissao))
            return (
              <Box key={area} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 2, p: 1.5 }}>
                <FormControlLabel
                  label={<Typography variant="body2" fontWeight={600}>{area}</Typography>}
                  control={
                    <Checkbox
                      size="small"
                      checked={todasMarcadas}
                      indeterminate={algumaMarcada && !todasMarcadas}
                      disabled={somenteLeitura || concediveis.length === 0}
                      onChange={(_, marcar) => alternarArea(itens, marcar)}
                      slotProps={{ input: { 'aria-label': `Marcar tudo de ${area}` } }}
                    />
                  }
                />
                <Box sx={{ pl: 3, display: 'flex', flexDirection: 'column' }}>
                  {itens.map((item) => {
                    const possui = permissoesDoUsuario.has(item.permissao)
                    const caixa = (
                      <FormControlLabel
                        key={item.permissao}
                        label={<Typography variant="body2">{item.descricao}</Typography>}
                        control={
                          <Checkbox
                            size="small"
                            checked={marcadas.has(item.permissao)}
                            // Quem não tem a permissão não a concede, mas vê (e mantém) o que o perfil já tem.
                            disabled={somenteLeitura || (!possui && !marcadas.has(item.permissao))}
                            onChange={() => alternar(item.permissao)}
                          />
                        }
                      />
                    )
                    return !possui && !somenteLeitura
                      ? <Tooltip key={item.permissao} title="Você não tem esta permissão, então não pode concedê-la."><span>{caixa}</span></Tooltip>
                      : caixa
                  })}
                </Box>
              </Box>
            )
          })}
        </Box>

        {perfil && podeVerHistorico && (
          <Box sx={{ mt: 3 }}>
            <HistoricoAuditoria tipoEntidade="PERFIL_ACESSO" entidadeId={perfil.id} titulo="Histórico de alterações do perfil" />
          </Box>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onFechar} disabled={isPending}>{somenteLeitura ? 'Fechar' : 'Cancelar'}</Button>
        {!somenteLeitura && (
          <Button variant="contained" onClick={salvar} disabled={isPending}>
            {isPending ? 'Salvando…' : perfil ? 'Salvar perfil' : 'Criar perfil'}
          </Button>
        )}
      </DialogActions>
    </Dialog>
  )
}
