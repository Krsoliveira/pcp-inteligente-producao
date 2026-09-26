import { useState } from 'react'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import MenuItem from '@mui/material/MenuItem'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Typography from '@mui/material/Typography'
import Skeleton from '@mui/material/Skeleton'
import Alert from '@mui/material/Alert'
import Dialog from '@mui/material/Dialog'
import DialogTitle from '@mui/material/DialogTitle'
import DialogContent from '@mui/material/DialogContent'
import DialogActions from '@mui/material/DialogActions'
import Grid from '@mui/material/Grid2'
import AddIcon from '@mui/icons-material/Add'
import SearchIcon from '@mui/icons-material/Search'
import InventoryIcon from '@mui/icons-material/Inventory2Outlined'
import InputAdornment from '@mui/material/InputAdornment'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { listarMateriais, cadastrarMaterial } from '../api/materiais'
import { PageHeader } from '../components/PageHeader'
import { EmptyState } from '../components/EmptyState'
import type { TipoMaterial, CadastrarMaterialRequest } from '../types'
import { TIPO_MATERIAL_LABEL } from '../types'

const TIPO_COR: Record<TipoMaterial, string> = {
  PRODUTO_ACABADO: '#1565c0',
  SEMIACABADO: '#7b1fa2',
  MATERIA_PRIMA: '#2e7d32',
}

export function MateriaisPage() {
  const [dialogAberto, setDialogAberto] = useState(false)
  const [busca, setBusca] = useState('')
  const [filtroTipo, setFiltroTipo] = useState<TipoMaterial | 'TODOS'>('TODOS')

  const { data: materiais = [], isLoading, isError } = useQuery({
    queryKey: ['materiais-todos'],
    queryFn: listarMateriais,
  })

  const filtrados = materiais.filter((m) => {
    const buscaOk = busca === '' || m.codigo.toLowerCase().includes(busca.toLowerCase()) || m.descricao.toLowerCase().includes(busca.toLowerCase())
    const tipoOk = filtroTipo === 'TODOS' || m.tipo === filtroTipo
    return buscaOk && tipoOk
  })

  return (
    <Box>
      <PageHeader
        titulo="Materiais"
        subtitulo={isLoading ? '…' : `${materiais.length} materiais cadastrados`}
        breadcrumbs={[{ label: 'Cadastros' }, { label: 'Materiais' }]}
        action={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialogAberto(true)}>
            Novo Material
          </Button>
        }
      />

      {isError && <Alert severity="error" sx={{ mb: 2 }}>Não foi possível carregar os materiais.</Alert>}

      <Card>
        <Box sx={{ p: 2, display: 'flex', gap: 2, flexWrap: 'wrap', borderBottom: '1px solid', borderColor: 'divider' }}>
          <TextField
            placeholder="Buscar por código ou descrição…"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            sx={{ flex: 1, minWidth: 220 }}
            slotProps={{ input: { startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" color="action" /></InputAdornment> } }}
          />
          <TextField select label="Tipo" value={filtroTipo} onChange={(e) => setFiltroTipo(e.target.value as TipoMaterial | 'TODOS')} sx={{ minWidth: 180 }}>
            <MenuItem value="TODOS">Todos os tipos</MenuItem>
            {(Object.keys(TIPO_MATERIAL_LABEL) as TipoMaterial[]).map((t) => (
              <MenuItem key={t} value={t}>{TIPO_MATERIAL_LABEL[t]}</MenuItem>
            ))}
          </TextField>
        </Box>

        {isLoading ? (
          <Skeleton variant="rounded" height={320} sx={{ m: 2 }} />
        ) : filtrados.length === 0 ? (
          <EmptyState Icone={InventoryIcon} titulo="Nenhum material encontrado" descricao="Cadastre materiais: produtos acabados, semiacabados e matérias-primas." acaoLabel="Novo Material" onAcao={() => setDialogAberto(true)} />
        ) : (
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Código</TableCell>
                  <TableCell>Descrição</TableCell>
                  <TableCell>Tipo</TableCell>
                  <TableCell>Unidade</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filtrados.map((m) => (
                  <TableRow key={m.id} hover>
                    <TableCell><Typography variant="body2" fontWeight={600}>{m.codigo}</Typography></TableCell>
                    <TableCell>{m.descricao}</TableCell>
                    <TableCell>
                      <Chip label={TIPO_MATERIAL_LABEL[m.tipo]} size="small"
                        sx={{ bgcolor: `${TIPO_COR[m.tipo]}14`, color: TIPO_COR[m.tipo], fontWeight: 600, fontSize: '0.7rem', border: 'none' }} />
                    </TableCell>
                    <TableCell><Typography variant="body2" color="text.secondary">{m.unidadeDeMedida}</Typography></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Card>

      <CriarMaterialDialog aberto={dialogAberto} onFechar={() => setDialogAberto(false)} />
    </Box>
  )
}

// ---- Dialog de cadastro ----

interface CriarMaterialDialogProps { aberto: boolean; onFechar: () => void }

const VAZIO: CadastrarMaterialRequest = { codigo: '', descricao: '', tipo: 'PRODUTO_ACABADO', unidadeDeMedida: '' }

function CriarMaterialDialog({ aberto, onFechar }: CriarMaterialDialogProps) {
  const [form, setForm] = useState<CadastrarMaterialRequest>(VAZIO)
  const [erro, setErro] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const { mutate, isPending } = useMutation({
    mutationFn: cadastrarMaterial,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['materiais-todos'] })
      queryClient.invalidateQueries({ queryKey: ['materiais'] })
      setForm(VAZIO); setErro(null); onFechar()
    },
    onError: (err: unknown) => {
      if (axios.isAxiosError(err)) setErro(err.response?.data?.detail ?? 'Erro ao cadastrar.')
      else setErro('Erro inesperado.')
    },
  })

  const handleSubmit = () => {
    if (!form.codigo || !form.descricao || !form.unidadeDeMedida) { setErro('Preencha todos os campos.'); return }
    mutate(form)
  }

  const handleFechar = () => { setForm(VAZIO); setErro(null); onFechar() }

  return (
    <Dialog open={aberto} onClose={handleFechar} fullWidth maxWidth="sm">
      <DialogTitle>Novo Material</DialogTitle>
      <DialogContent dividers>
        {erro && <Alert severity="error" sx={{ mb: 2 }}>{erro}</Alert>}
        <Grid container spacing={2} sx={{ mt: 0 }}>
          <Grid size={{ xs: 12, sm: 5 }}>
            <TextField label="Código *" value={form.codigo} onChange={(e) => setForm(p => ({ ...p, codigo: e.target.value.toUpperCase() }))} fullWidth inputProps={{ maxLength: 30 }} placeholder="ACO-1020" />
          </Grid>
          <Grid size={{ xs: 12, sm: 7 }}>
            <TextField label="Descrição *" value={form.descricao} onChange={(e) => setForm(p => ({ ...p, descricao: e.target.value }))} fullWidth inputProps={{ maxLength: 200 }} />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField select label="Tipo *" value={form.tipo} onChange={(e) => setForm(p => ({ ...p, tipo: e.target.value as TipoMaterial }))} fullWidth>
              {(Object.keys(TIPO_MATERIAL_LABEL) as TipoMaterial[]).map((t) => (
                <MenuItem key={t} value={t}>{TIPO_MATERIAL_LABEL[t]}</MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField label="Unidade de Medida *" value={form.unidadeDeMedida} onChange={(e) => setForm(p => ({ ...p, unidadeDeMedida: e.target.value }))} fullWidth inputProps={{ maxLength: 10 }} placeholder="un, kg, m…" helperText={form.tipo === 'MATERIA_PRIMA' ? 'Matéria-prima não possui lista técnica.' : ''} />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={handleFechar} disabled={isPending}>Cancelar</Button>
        <Button variant="contained" onClick={handleSubmit} disabled={isPending}>{isPending ? 'Salvando…' : 'Cadastrar'}</Button>
      </DialogActions>
    </Dialog>
  )
}
