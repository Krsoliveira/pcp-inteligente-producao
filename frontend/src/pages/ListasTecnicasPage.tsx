import { useState } from 'react'
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
import MenuItem from '@mui/material/MenuItem'
import Grid from '@mui/material/Grid2'
import IconButton from '@mui/material/IconButton'
import Tooltip from '@mui/material/Tooltip'
import Divider from '@mui/material/Divider'
import Chip from '@mui/material/Chip'
import AddIcon from '@mui/icons-material/Add'
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import AccountTreeIcon from '@mui/icons-material/AccountTreeOutlined'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { listarListasTecnicas, cadastrarListaTecnica, ativarListaTecnica } from '../api/listasTecnicas'
import { listarMateriais } from '../api/materiais'
import { PageHeader } from '../components/PageHeader'
import { EmptyState } from '../components/EmptyState'
import { StatusListaBadge } from '../components/StatusBadge'
import type { CadastrarListaTecnicaRequest, TipoMaterial } from '../types'

export function ListasTecnicasPage() {
  const [dialogAberto, setDialogAberto] = useState(false)
  const queryClient = useQueryClient()

  const { data: listas = [], isLoading, isError } = useQuery({
    queryKey: ['listas-tecnicas-todas'],
    queryFn: () => listarListasTecnicas(),
  })

  const { data: materiais = [] } = useQuery({
    queryKey: ['materiais'],
    queryFn: listarMateriais,
  })

  const { mutate: ativar } = useMutation({
    mutationFn: ativarListaTecnica,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['listas-tecnicas-todas'] }),
  })

  const getMaterialNome = (id: string) => {
    const m = materiais.find(m => m.id === id)
    return m ? `${m.codigo} — ${m.descricao}` : id.substring(0, 8) + '…'
  }

  return (
    <Box>
      <PageHeader
        titulo="Listas Técnicas"
        subtitulo={isLoading ? '…' : `${listas.length} versões cadastradas`}
        breadcrumbs={[{ label: 'Cadastros' }, { label: 'Listas Técnicas' }]}
        action={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialogAberto(true)}>
            Nova Lista
          </Button>
        }
      />

      {isError && <Alert severity="error" sx={{ mb: 2 }}>Não foi possível carregar as listas técnicas.</Alert>}

      <Card>
        {isLoading ? (
          <Skeleton variant="rounded" height={320} sx={{ m: 2 }} />
        ) : listas.length === 0 ? (
          <EmptyState
            Icone={AccountTreeIcon}
            titulo="Nenhuma lista técnica cadastrada"
            descricao="As listas técnicas (BOM) definem quais materiais e quantidades são necessários para produzir cada produto."
            acaoLabel="Criar lista técnica"
            onAcao={() => setDialogAberto(true)}
          />
        ) : (
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Material</TableCell>
                  <TableCell>Versão</TableCell>
                  <TableCell align="center">Componentes</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="center" sx={{ width: 80 }}>Ações</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {listas.map((l) => (
                  <TableRow key={l.id} hover>
                    <TableCell>
                      <Typography variant="body2" fontWeight={500}>{getMaterialNome(l.materialId)}</Typography>
                    </TableCell>
                    <TableCell>
                      <Chip label={l.versao} size="small" variant="outlined" sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }} />
                    </TableCell>
                    <TableCell align="center">
                      <Typography variant="body2">{l.itens.length}</Typography>
                    </TableCell>
                    <TableCell><StatusListaBadge status={l.status} /></TableCell>
                    <TableCell align="center">
                      {l.status === 'EM_REVISAO' && (
                        <Tooltip title="Ativar esta versão">
                          <IconButton size="small" color="success" onClick={() => ativar(l.id)}>
                            <CheckCircleOutlineIcon sx={{ fontSize: 16 }} />
                          </IconButton>
                        </Tooltip>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Card>

      <CriarListaTecnicaDialog aberto={dialogAberto} onFechar={() => setDialogAberto(false)} />
    </Box>
  )
}

// ---- Dialog criar ----

interface CriarListaTecnicaDialogProps { aberto: boolean; onFechar: () => void }

interface ItemForm { materialComponenteId: string; quantidadePlanejada: string; unidadeDeMedida: string }

function CriarListaTecnicaDialog({ aberto, onFechar }: CriarListaTecnicaDialogProps) {
  const [materialId, setMaterialId] = useState('')
  const [versao, setVersao] = useState('')
  const [itens, setItens] = useState<ItemForm[]>([{ materialComponenteId: '', quantidadePlanejada: '', unidadeDeMedida: 'un' }])
  const [erro, setErro] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const { data: materiais = [] } = useQuery({ queryKey: ['materiais'], queryFn: listarMateriais, enabled: aberto })
  const produtivos = materiais.filter((m) => (m.tipo as TipoMaterial) !== 'MATERIA_PRIMA')
  const componentes = materiais

  const { mutate, isPending } = useMutation({
    mutationFn: cadastrarListaTecnica,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['listas-tecnicas-todas'] })
      queryClient.invalidateQueries({ queryKey: ['listas-tecnicas'] })
      handleFechar()
    },
    onError: (err: unknown) => {
      if (axios.isAxiosError(err)) setErro(err.response?.data?.detail ?? 'Erro ao cadastrar.')
      else setErro('Erro inesperado.')
    },
  })

  const handleSubmit = () => {
    if (!materialId || !versao) { setErro('Material e versão são obrigatórios.'); return }
    const itensValidos = itens.filter((i) => i.materialComponenteId && i.quantidadePlanejada)
    if (itensValidos.length === 0) { setErro('Adicione ao menos um componente.'); return }
    const payload: CadastrarListaTecnicaRequest = {
      materialId,
      versao,
      itens: itensValidos.map((i) => ({
        materialComponenteId: i.materialComponenteId,
        quantidadePlanejada: Number(i.quantidadePlanejada),
        unidadeDeMedida: i.unidadeDeMedida,
      })),
    }
    mutate(payload)
  }

  const handleFechar = () => {
    setMaterialId(''); setVersao(''); setItens([{ materialComponenteId: '', quantidadePlanejada: '', unidadeDeMedida: 'un' }]); setErro(null); onFechar()
  }

  const addItem = () => setItens(prev => [...prev, { materialComponenteId: '', quantidadePlanejada: '', unidadeDeMedida: 'un' }])
  const removeItem = (idx: number) => setItens(prev => prev.filter((_, i) => i !== idx))
  const updateItem = (idx: number, campo: keyof ItemForm, valor: string) => setItens(prev => prev.map((item, i) => i === idx ? { ...item, [campo]: valor } : item))

  return (
    <Dialog open={aberto} onClose={handleFechar} fullWidth maxWidth="md">
      <DialogTitle>Nova Lista Técnica (BOM)</DialogTitle>
      <DialogContent dividers>
        {erro && <Alert severity="error" sx={{ mb: 2 }}>{erro}</Alert>}
        <Grid container spacing={2} sx={{ mt: 0 }}>
          <Grid size={{ xs: 12, sm: 8 }}>
            <TextField select label="Material *" value={materialId} onChange={(e) => setMaterialId(e.target.value)} fullWidth helperText="Produto acabado ou semiacabado que será fabricado.">
              {produtivos.map((m) => (
                <MenuItem key={m.id} value={m.id}>{m.codigo} — {m.descricao}</MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <TextField label="Versão *" value={versao} onChange={(e) => setVersao(e.target.value)} fullWidth placeholder="v1, v2-rev-a…" inputProps={{ maxLength: 50 }} />
          </Grid>
        </Grid>

        <Divider sx={{ my: 2.5 }} />

        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1.5 }}>
          <Typography variant="subtitle2">Componentes</Typography>
          <Button size="small" startIcon={<AddCircleOutlineIcon />} onClick={addItem}>Adicionar</Button>
        </Box>

        {itens.map((item, idx) => (
          <Box key={idx} sx={{ display: 'flex', gap: 1.5, mb: 1.5, alignItems: 'flex-start' }}>
            <TextField select label="Material componente" value={item.materialComponenteId} onChange={(e) => updateItem(idx, 'materialComponenteId', e.target.value)} sx={{ flex: 2 }}>
              {componentes.map((m) => (
                <MenuItem key={m.id} value={m.id}>{m.codigo} — {m.descricao}</MenuItem>
              ))}
            </TextField>
            <TextField label="Qtd" type="number" value={item.quantidadePlanejada} onChange={(e) => updateItem(idx, 'quantidadePlanejada', e.target.value)} sx={{ width: 90 }} inputProps={{ min: 0.0001, step: 0.0001 }} />
            <TextField label="Un." value={item.unidadeDeMedida} onChange={(e) => updateItem(idx, 'unidadeDeMedida', e.target.value)} sx={{ width: 70 }} inputProps={{ maxLength: 10 }} />
            <IconButton onClick={() => removeItem(idx)} disabled={itens.length === 1} sx={{ mt: 0.5 }}>
              <DeleteOutlineIcon fontSize="small" />
            </IconButton>
          </Box>
        ))}
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={handleFechar} disabled={isPending}>Cancelar</Button>
        <Button variant="contained" onClick={handleSubmit} disabled={isPending}>{isPending ? 'Salvando…' : 'Criar Lista'}</Button>
      </DialogActions>
    </Dialog>
  )
}
