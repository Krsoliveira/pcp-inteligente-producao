import { useState } from 'react'
import { SePermitido } from '../components/SePermitido'
import { usePermissoes } from '../hooks/usePermissoes'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Button from '@mui/material/Button'
import Typography from '@mui/material/Typography'
import Skeleton from '@mui/material/Skeleton'
import Alert from '@mui/material/Alert'
import Dialog from '@mui/material/Dialog'
import DialogTitle from '@mui/material/DialogTitle'
import DialogContent from '@mui/material/DialogContent'
import DialogActions from '@mui/material/DialogActions'
import TextField from '@mui/material/TextField'
import Grid from '@mui/material/Grid'
import Chip from '@mui/material/Chip'
import IconButton from '@mui/material/IconButton'
import Tooltip from '@mui/material/Tooltip'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import LabelOutlinedIcon from '@mui/icons-material/LabelOutlined'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { listarTiposOrdem, cadastrarTipoOrdem, atualizarTipoOrdem } from '../api/tiposOrdem'
import { PageHeader } from '../components/PageHeader'
import { EmptyState } from '../components/EmptyState'
import { TipoOrdemBadge } from '../components/StatusBadge'
import type { TipoOrdem, CadastrarTipoOrdemRequest } from '../types'
import { mensagemDeErroDaApi } from '../utils/mensagensDeErro'

const CORES_SUGERIDAS = [
  '#4F46E5', '#2563EB', '#0F766E', '#059669',
  '#D97706', '#DC2626', '#9333EA', '#DB2777',
  '#0284C7', '#64748B', '#B45309', '#7C3AED',
]

export function TiposOrdemPage() {
  const { tem } = usePermissoes()
  const [dialogAberto, setDialogAberto] = useState(false)
  const [editando, setEditando] = useState<TipoOrdem | null>(null)

  const { data: tipos = [], isLoading, isError } = useQuery({
    queryKey: ['tipos-ordem'],
    queryFn: listarTiposOrdem,
  })

  return (
    <Box>
      <PageHeader
        titulo="Tipos de Ordem"
        subtitulo="Categorias para classificar ordens de produção"
        breadcrumbs={[{ label: 'Cadastros' }, { label: 'Tipos de Ordem' }]}
        action={
          <SePermitido qualquer={['TIPO_ORDEM_GERENCIAR']}>
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialogAberto(true)}>
              Novo Tipo
            </Button>
          </SePermitido>
        }
      />

      {isError && <Alert severity="error" sx={{ mb: 2 }}>Não foi possível carregar os tipos de ordem.</Alert>}

      <Card>
        {isLoading ? (
          <Skeleton variant="rounded" height={280} sx={{ m: 2 }} />
        ) : tipos.length === 0 ? (
          <EmptyState
            Icone={LabelOutlinedIcon}
            titulo="Nenhum tipo de ordem cadastrado"
            descricao="Crie categorias como Produção Normal, Manutenção, Revenda ou Retrabalho para classificar suas ordens."
            acaoLabel={tem('TIPO_ORDEM_GERENCIAR') ? 'Criar primeiro tipo' : undefined}
            onAcao={() => setDialogAberto(true)}
          />
        ) : (
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Nome</TableCell>
                  <TableCell>Descrição</TableCell>
                  <TableCell>Cor</TableCell>
                  <TableCell align="center" sx={{ width: 60 }}>Ações</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {tipos.map((t) => (
                  <TableRow key={t.id} hover>
                    <TableCell>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <TipoOrdemBadge nome={t.nome} cor={t.cor} />
                        {t.conversao && <Chip size="small" variant="outlined" label="Sistema" />}
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" color="text.secondary">{t.descricao ?? '—'}</Typography>
                    </TableCell>
                    <TableCell>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Box sx={{ width: 16, height: 16, borderRadius: 0.5, bgcolor: t.cor, flexShrink: 0 }} />
                        <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.75rem', color: 'text.secondary' }}>
                          {t.cor}
                        </Typography>
                      </Box>
                    </TableCell>
                    <TableCell align="center">
                      <SePermitido qualquer={['TIPO_ORDEM_GERENCIAR']}>
                        {!t.conversao && (
                          <Tooltip title="Editar">
                            <IconButton size="small" onClick={() => setEditando(t)}>
                              <EditIcon sx={{ fontSize: 16 }} />
                            </IconButton>
                          </Tooltip>
                        )}
                      </SePermitido>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Card>

      <TipoOrdemDialog
        aberto={dialogAberto || editando !== null}
        tipoExistente={editando}
        onFechar={() => { setDialogAberto(false); setEditando(null) }}
      />
    </Box>
  )
}

// ---- Dialog criar/editar ----

interface TipoOrdemDialogProps {
  aberto: boolean
  tipoExistente: TipoOrdem | null
  onFechar: () => void
}

function TipoOrdemDialog({ aberto, tipoExistente, onFechar }: TipoOrdemDialogProps) {
  const [form, setForm] = useState<CadastrarTipoOrdemRequest>({ nome: '', descricao: '', cor: '#4F46E5' })
  const [erro, setErro] = useState<string | null>(null)
  const queryClient = useQueryClient()

  // Preenche o form ao abrir para edição
  useState(() => {
    if (tipoExistente) {
      setForm({ nome: tipoExistente.nome, descricao: tipoExistente.descricao ?? '', cor: tipoExistente.cor })
    } else {
      setForm({ nome: '', descricao: '', cor: '#4F46E5' })
    }
  })

  const { mutate: criar, isPending: criando } = useMutation({
    mutationFn: cadastrarTipoOrdem,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['tipos-ordem'] }); onFechar(); setErro(null) },
    onError: (err: unknown) => {
      setErro(mensagemDeErroDaApi(err, 'Não foi possível cadastrar o tipo de ordem. Tente novamente.'))
    },
  })

  const { mutate: editar, isPending: editando } = useMutation({
    mutationFn: (payload: CadastrarTipoOrdemRequest) => atualizarTipoOrdem(tipoExistente!.id, payload),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['tipos-ordem'] }); onFechar(); setErro(null) },
    onError: (err: unknown) => {
      setErro(mensagemDeErroDaApi(err, 'Não foi possível atualizar o tipo de ordem. Tente novamente.'))
    },
  })

  const isPending = criando || editando

  const handleSubmit = () => {
    if (!form.nome || !form.cor) { setErro('Nome e cor são obrigatórios.'); return }
    if (tipoExistente) editar(form)
    else criar(form)
  }

  return (
    <Dialog open={aberto} onClose={onFechar} fullWidth maxWidth="xs">
      <DialogTitle>{tipoExistente ? 'Editar Tipo de Ordem' : 'Novo Tipo de Ordem'}</DialogTitle>
      <DialogContent dividers>
        {erro && <Alert severity="error" sx={{ mb: 2 }}>{erro}</Alert>}
        <Grid container spacing={2} sx={{ mt: 0 }}>
          <Grid size={12}>
            <TextField label="Nome *" value={form.nome} onChange={(e) => setForm(p => ({ ...p, nome: e.target.value }))} fullWidth inputProps={{ maxLength: 60 }} placeholder="Ex.: Produção Normal" />
          </Grid>
          <Grid size={12}>
            <TextField label="Descrição" value={form.descricao} onChange={(e) => setForm(p => ({ ...p, descricao: e.target.value }))} fullWidth multiline rows={2} inputProps={{ maxLength: 200 }} />
          </Grid>
          <Grid size={12}>
            <Typography variant="caption" color="text.secondary" sx={{ mb: 1, display: 'block' }}>Cor do badge</Typography>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.75 }}>
              {CORES_SUGERIDAS.map((cor) => (
                <Box
                  key={cor}
                  onClick={() => setForm(p => ({ ...p, cor }))}
                  sx={{
                    width: 28, height: 28, borderRadius: 1, bgcolor: cor, cursor: 'pointer',
                    border: form.cor === cor ? '3px solid rgba(0,0,0,0.5)' : '2px solid transparent',
                    transition: 'transform 0.1s', '&:hover': { transform: 'scale(1.15)' },
                  }}
                />
              ))}
            </Box>
            <Box sx={{ mt: 1.5, display: 'flex', alignItems: 'center', gap: 1 }}>
              <Box sx={{ width: 28, height: 28, borderRadius: 1, bgcolor: form.cor, flexShrink: 0 }} />
              <TextField label="Hex" value={form.cor} onChange={(e) => setForm(p => ({ ...p, cor: e.target.value }))} size="small" inputProps={{ maxLength: 7 }} sx={{ width: 110 }} />
              <Box sx={{ ml: 'auto' }}><TipoOrdemBadge nome={form.nome || 'Preview'} cor={form.cor} /></Box>
            </Box>
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onFechar} disabled={isPending}>Cancelar</Button>
        <Button variant="contained" onClick={handleSubmit} disabled={isPending}>{isPending ? 'Salvando…' : tipoExistente ? 'Salvar' : 'Criar'}</Button>
      </DialogActions>
    </Dialog>
  )
}
