import { useState, useEffect } from 'react'
import Dialog from '@mui/material/Dialog'
import DialogTitle from '@mui/material/DialogTitle'
import DialogContent from '@mui/material/DialogContent'
import DialogActions from '@mui/material/DialogActions'
import Button from '@mui/material/Button'
import TextField from '@mui/material/TextField'
import MenuItem from '@mui/material/MenuItem'
import Grid from '@mui/material/Grid2'
import Alert from '@mui/material/Alert'
import CircularProgress from '@mui/material/CircularProgress'
import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import Divider from '@mui/material/Divider'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { criarOrdem } from '../api/ordens'
import { listarMateriais } from '../api/materiais'
import { listarListasTecnicas } from '../api/listasTecnicas'
import { listarTiposOrdem } from '../api/tiposOrdem'
import type { CriarOrdemRequest } from '../types'

interface CriarOrdemDialogProps {
  aberto: boolean
  onFechar: () => void
}

const CENTROS_DE_TRABALHO = [
  'Usinagem CNC',
  'Montagem',
  'Soldagem MIG/TIG',
  'Pintura Industrial',
  'Inspeção de Qualidade',
  'Fundição Sob Pressão',
  'Estamparia',
  'Tratamento Térmico',
]

const VAZIO: CriarOrdemRequest = {
  codigo: '',
  materialId: '',
  listaTecnicaId: '',
  tipoOrdemId: null,
  centroDeTrabalho: '',
  quantidade: 1,
  inicioPlanejado: '',
  fimPlanejado: '',
}

export function CriarOrdemDialog({ aberto, onFechar }: CriarOrdemDialogProps) {
  const [form, setForm] = useState<CriarOrdemRequest>(VAZIO)
  const [erro, setErro] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const { data: materiais = [], isLoading: carregandoMateriais } = useQuery({
    queryKey: ['materiais'],
    queryFn: listarMateriais,
    enabled: aberto,
    select: (lista) => lista.filter((m) => m.tipo !== 'MATERIA_PRIMA'),
  })

  const { data: tiposOrdem = [] } = useQuery({
    queryKey: ['tipos-ordem'],
    queryFn: listarTiposOrdem,
    enabled: aberto,
  })

  const { data: listasTecnicas = [] } = useQuery({
    queryKey: ['listas-tecnicas', form.materialId],
    queryFn: () => listarListasTecnicas(form.materialId),
    enabled: aberto && !!form.materialId,
  })

  useEffect(() => {
    const ativa = listasTecnicas.find((l) => l.status === 'ATIVA')
    if (ativa) {
      setForm((prev) => ({ ...prev, listaTecnicaId: ativa.id }))
    } else {
      setForm((prev) => ({ ...prev, listaTecnicaId: '' }))
    }
  }, [listasTecnicas])

  const { mutate, isPending } = useMutation({
    mutationFn: criarOrdem,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ordens'] })
      setForm(VAZIO)
      setErro(null)
      onFechar()
    },
    onError: (error: unknown) => {
      if (axios.isAxiosError(error)) {
        const detail = error.response?.data?.detail as string | undefined
        setErro(detail ?? 'Erro ao criar ordem. Verifique os campos.')
      } else {
        setErro('Erro inesperado. Tente novamente.')
      }
    },
  })

  const set = (campo: keyof CriarOrdemRequest) => (e: React.ChangeEvent<HTMLInputElement>) => {
    setErro(null)
    const valor = campo === 'quantidade' ? Number(e.target.value) : e.target.value || null
    if (campo === 'materialId') {
      setForm((prev) => ({ ...prev, materialId: e.target.value, listaTecnicaId: '' }))
    } else {
      setForm((prev) => ({ ...prev, [campo]: valor }))
    }
  }

  const handleSubmit = () => {
    if (!form.codigo || !form.materialId || !form.listaTecnicaId ||
        !form.centroDeTrabalho || !form.inicioPlanejado || !form.fimPlanejado) {
      setErro('Preencha todos os campos obrigatórios.')
      return
    }
    mutate(form)
  }

  const handleFechar = () => {
    setForm(VAZIO)
    setErro(null)
    onFechar()
  }

  const listasDisponiveis = listasTecnicas.filter(
    (l) => l.status === 'ATIVA' || l.status === 'EM_REVISAO',
  )

  return (
    <Dialog open={aberto} onClose={handleFechar} fullWidth maxWidth="sm">
      <DialogTitle sx={{ pb: 1 }}>Nova Ordem de Produção</DialogTitle>
      <DialogContent dividers>
        {erro && <Alert severity="error" sx={{ mb: 2 }}>{erro}</Alert>}

        <Grid container spacing={2} sx={{ mt: 0 }}>
          {/* Identificação */}
          <Grid size={12}>
            <Typography variant="caption" color="text.secondary" fontWeight={600} sx={{ textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Identificação
            </Typography>
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField label="Código *" value={form.codigo} onChange={set('codigo')} fullWidth inputProps={{ maxLength: 30 }} placeholder="OP-0001" />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField label="Quantidade *" type="number" value={form.quantidade} onChange={set('quantidade')} fullWidth inputProps={{ min: 1 }} />
          </Grid>
          <Grid size={12}>
            <TextField
              select
              label="Tipo de Ordem (opcional)"
              value={form.tipoOrdemId ?? ''}
              onChange={(e) => setForm((p) => ({ ...p, tipoOrdemId: e.target.value || null }))}
              fullWidth
              helperText="Categorize a ordem: Produção, Manutenção, Revenda, etc."
            >
              <MenuItem value=""><em>Sem categoria</em></MenuItem>
              {tiposOrdem.map((t) => (
                <MenuItem key={t.id} value={t.id}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Box sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: t.cor, flexShrink: 0 }} />
                    {t.nome}
                  </Box>
                </MenuItem>
              ))}
            </TextField>
          </Grid>

          <Grid size={12}><Divider /></Grid>

          {/* Produto */}
          <Grid size={12}>
            <Typography variant="caption" color="text.secondary" fontWeight={600} sx={{ textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Produto e Lista Técnica
            </Typography>
          </Grid>
          <Grid size={12}>
            {carregandoMateriais ? (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <CircularProgress size={18} />
                <Typography variant="body2" color="text.secondary">Carregando materiais…</Typography>
              </Box>
            ) : (
              <TextField
                select
                label="Material *"
                value={form.materialId}
                onChange={set('materialId')}
                fullWidth
                helperText={materiais.length === 0 ? 'Cadastre um material primeiro.' : ''}
              >
                {materiais.map((m) => (
                  <MenuItem key={m.id} value={m.id}>
                    <Box>
                      <Typography variant="body2" fontWeight={500}>{m.codigo}</Typography>
                      <Typography variant="caption" color="text.secondary">{m.descricao}</Typography>
                    </Box>
                  </MenuItem>
                ))}
              </TextField>
            )}
          </Grid>
          <Grid size={12}>
            <TextField
              select
              label="Lista Técnica (BOM) *"
              value={form.listaTecnicaId}
              onChange={set('listaTecnicaId')}
              fullWidth
              disabled={!form.materialId || listasDisponiveis.length === 0}
              helperText={form.materialId && listasDisponiveis.length === 0 ? 'Nenhuma lista técnica ativa para este material.' : ''}
            >
              {listasDisponiveis.map((l) => (
                <MenuItem key={l.id} value={l.id}>
                  {l.versao}
                  {l.status === 'ATIVA' && <Typography component="span" variant="caption" color="success.main" sx={{ ml: 1 }}>(Ativa)</Typography>}
                </MenuItem>
              ))}
            </TextField>
          </Grid>

          <Grid size={12}><Divider /></Grid>

          {/* Execução */}
          <Grid size={12}>
            <Typography variant="caption" color="text.secondary" fontWeight={600} sx={{ textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Execução
            </Typography>
          </Grid>
          <Grid size={12}>
            <TextField
              select
              label="Centro de Trabalho *"
              value={form.centroDeTrabalho}
              onChange={set('centroDeTrabalho')}
              fullWidth
            >
              {CENTROS_DE_TRABALHO.map((centro) => (
                <MenuItem key={centro} value={centro}>{centro}</MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField label="Início Planejado *" type="date" value={form.inicioPlanejado} onChange={set('inicioPlanejado')} fullWidth slotProps={{ inputLabel: { shrink: true } }} />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField label="Fim Planejado *" type="date" value={form.fimPlanejado} onChange={set('fimPlanejado')} fullWidth slotProps={{ inputLabel: { shrink: true } }} />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={handleFechar} disabled={isPending}>Cancelar</Button>
        <Button variant="contained" onClick={handleSubmit} disabled={isPending}>
          {isPending ? 'Salvando…' : 'Criar Ordem'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
