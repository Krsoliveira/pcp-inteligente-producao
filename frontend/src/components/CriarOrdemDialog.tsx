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
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { criarOrdem } from '../api/ordens'
import { listarMateriais, listarListasTecnicas } from '../api/materiais'
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

  const { data: listasTecnicas = [] } = useQuery({
    queryKey: ['listas-tecnicas', form.materialId],
    queryFn: () => listarListasTecnicas(form.materialId),
    enabled: aberto && !!form.materialId,
  })

  // Ao carregar as listas do material, pré-seleciona a versão ATIVA automaticamente
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

  const handleChange =
    (campo: keyof CriarOrdemRequest) =>
    (e: React.ChangeEvent<HTMLInputElement>) => {
      setErro(null)
      const valor = campo === 'quantidade' ? Number(e.target.value) : e.target.value
      setForm((prev) => ({ ...prev, [campo]: valor }))
      // Ao trocar material, limpar lista técnica (o useEffect acima vai pré-selecionar a ATIVA)
      if (campo === 'materialId') {
        setForm((prev) => ({ ...prev, materialId: e.target.value, listaTecnicaId: '' }))
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
      <DialogTitle>Nova Ordem de Produção</DialogTitle>
      <DialogContent dividers>
        {erro && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {erro}
          </Alert>
        )}
        <Grid container spacing={2} sx={{ mt: 0 }}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Código *"
              value={form.codigo}
              onChange={handleChange('codigo')}
              fullWidth
              inputProps={{ maxLength: 30 }}
              placeholder="OP-0001"
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Quantidade *"
              type="number"
              value={form.quantidade}
              onChange={handleChange('quantidade')}
              fullWidth
              inputProps={{ min: 1 }}
            />
          </Grid>
          <Grid size={12}>
            {carregandoMateriais ? (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <CircularProgress size={20} />
                <span>Carregando materiais…</span>
              </Box>
            ) : (
              <TextField
                select
                label="Material *"
                value={form.materialId}
                onChange={handleChange('materialId')}
                fullWidth
                helperText={materiais.length === 0 ? 'Nenhum material cadastrado. Cadastre um no Swagger.' : ''}
              >
                {materiais.map((m) => (
                  <MenuItem key={m.id} value={m.id}>
                    {m.codigo} — {m.descricao}
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
              onChange={handleChange('listaTecnicaId')}
              fullWidth
              disabled={!form.materialId || listasDisponiveis.length === 0}
              helperText={
                form.materialId && listasDisponiveis.length === 0
                  ? 'Nenhuma lista técnica ativa para este material.'
                  : ''
              }
            >
              {listasDisponiveis.map((l) => (
                <MenuItem key={l.id} value={l.id}>
                  {l.versao} — {l.status}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid size={12}>
            <TextField
              select
              label="Centro de Trabalho *"
              value={form.centroDeTrabalho}
              onChange={handleChange('centroDeTrabalho')}
              fullWidth
            >
              {CENTROS_DE_TRABALHO.map((centro) => (
                <MenuItem key={centro} value={centro}>
                  {centro}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Início Planejado *"
              type="date"
              value={form.inicioPlanejado}
              onChange={handleChange('inicioPlanejado')}
              fullWidth
              slotProps={{ inputLabel: { shrink: true } }}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Fim Planejado *"
              type="date"
              value={form.fimPlanejado}
              onChange={handleChange('fimPlanejado')}
              fullWidth
              slotProps={{ inputLabel: { shrink: true } }}
            />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={handleFechar} disabled={isPending}>
          Cancelar
        </Button>
        <Button variant="contained" onClick={handleSubmit} disabled={isPending}>
          {isPending ? 'Salvando…' : 'Criar Ordem'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
