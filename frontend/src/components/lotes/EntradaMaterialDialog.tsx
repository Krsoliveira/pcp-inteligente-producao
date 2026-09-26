import { useState } from 'react'
import Dialog from '@mui/material/Dialog'
import DialogTitle from '@mui/material/DialogTitle'
import DialogContent from '@mui/material/DialogContent'
import DialogActions from '@mui/material/DialogActions'
import Button from '@mui/material/Button'
import TextField from '@mui/material/TextField'
import MenuItem from '@mui/material/MenuItem'
import Alert from '@mui/material/Alert'
import Grid from '@mui/material/Grid2'
import InputAdornment from '@mui/material/InputAdornment'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { listarMateriais } from '../../api/materiais'
import { registrarEntradaMaterial } from '../../api/lotes'
import type { ApiError, Lote } from '../../types'

interface EntradaMaterialDialogProps {
  aberto: boolean
  onFechar: () => void
  onRegistrado: (lote: Lote) => void
}

interface Formulario {
  materialId: string
  fornecedor: string
  notaFiscal: string
  quantidade: string
  dataFabricacao: string
  dataValidade: string
}

type Erros = Partial<Record<keyof Formulario, string>>

const hojeIso = () => new Date().toISOString().slice(0, 10)

const VAZIO: Formulario = {
  materialId: '',
  fornecedor: '',
  notaFiscal: '',
  quantidade: '',
  dataFabricacao: '',
  dataValidade: '',
}

/** Mesmas regras do backend (RegistrarEntradaMaterialRequest + Lote.receberCompra). */
function validar(f: Formulario): Erros {
  const erros: Erros = {}
  if (!f.materialId) erros.materialId = 'Selecione a matéria-prima.'
  if (!f.fornecedor.trim()) erros.fornecedor = 'Informe o fornecedor.'
  if (!f.notaFiscal.trim()) erros.notaFiscal = 'Informe a nota fiscal.'
  const quantidade = Number(f.quantidade.replace(',', '.'))
  if (!f.quantidade || !Number.isFinite(quantidade) || quantidade <= 0) {
    erros.quantidade = 'Informe uma quantidade maior que zero.'
  }
  if (!f.dataFabricacao) erros.dataFabricacao = 'Informe a data de fabricação.'
  else if (f.dataFabricacao > hojeIso()) erros.dataFabricacao = 'Não pode estar no futuro.'
  if (!f.dataValidade) erros.dataValidade = 'Informe a data de validade.'
  else if (f.dataFabricacao && f.dataValidade < f.dataFabricacao) {
    erros.dataValidade = 'Não pode ser anterior à fabricação.'
  }
  return erros
}

/**
 * Registro de recebimento de matéria-prima comprada. Gera um lote DISPONIVEL
 * com fornecedor e nota fiscal (POST /api/v1/lotes/entradas).
 */
export function EntradaMaterialDialog({ aberto, onFechar, onRegistrado }: EntradaMaterialDialogProps) {
  const [form, setForm] = useState<Formulario>(VAZIO)
  const [erros, setErros] = useState<Erros>({})
  const [erroGeral, setErroGeral] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const { data: materiais = [], isLoading: carregandoMateriais } = useQuery({
    queryKey: ['materiais'],
    queryFn: listarMateriais,
    enabled: aberto,
  })
  const materiasPrimas = materiais.filter((m) => m.tipo === 'MATERIA_PRIMA')
  const unidade = materiasPrimas.find((m) => m.id === form.materialId)?.unidadeDeMedida

  const { mutate, isPending } = useMutation({
    mutationFn: registrarEntradaMaterial,
    onSuccess: (lote) => {
      queryClient.invalidateQueries({ queryKey: ['lotes'] })
      fechar()
      onRegistrado(lote)
    },
    onError: (err: unknown) => {
      if (!axios.isAxiosError<ApiError>(err) || !err.response) {
        setErroGeral('Erro ao conectar com o servidor.')
        return
      }
      const { status, data } = err.response
      if (status === 400 && data?.erros) setErros(data.erros as Erros)
      else if (status === 409) setErros({ notaFiscal: data?.detail ?? 'Nota fiscal já registrada.' })
      else setErroGeral(data?.detail ?? 'Não foi possível registrar a entrada.')
    },
  })

  const alterar = (campo: keyof Formulario) => (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm((atual) => ({ ...atual, [campo]: e.target.value }))
    if (erros[campo]) setErros((atuais) => ({ ...atuais, [campo]: undefined }))
  }

  const fechar = () => {
    setForm(VAZIO)
    setErros({})
    setErroGeral(null)
    onFechar()
  }

  const registrar = () => {
    setErroGeral(null)
    const novosErros = validar(form)
    setErros(novosErros)
    if (Object.keys(novosErros).length > 0) return
    mutate({
      materialId: form.materialId,
      fornecedor: form.fornecedor.trim(),
      notaFiscal: form.notaFiscal.trim(),
      quantidade: Number(form.quantidade.replace(',', '.')),
      dataFabricacao: form.dataFabricacao,
      dataValidade: form.dataValidade,
    })
  }

  return (
    <Dialog open={aberto} onClose={fechar} fullWidth maxWidth="sm">
      <DialogTitle>Entrada de Material</DialogTitle>
      <DialogContent dividers>
        {erroGeral && <Alert severity="error" sx={{ mb: 2 }}>{erroGeral}</Alert>}
        {!carregandoMateriais && materiasPrimas.length === 0 && (
          <Alert severity="info" sx={{ mb: 2 }}>
            Nenhuma matéria-prima cadastrada. Cadastre em Materiais antes de registrar entradas.
          </Alert>
        )}
        <Grid container spacing={2}>
          <Grid size={12}>
            <TextField select label="Matéria-prima *" value={form.materialId} onChange={alterar('materialId')}
              error={!!erros.materialId} helperText={erros.materialId ?? 'Somente matérias-primas podem dar entrada por compra.'}
              fullWidth disabled={carregandoMateriais}>
              {materiasPrimas.map((m) => (
                <MenuItem key={m.id} value={m.id}>{m.codigo} — {m.descricao}</MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid size={{ xs: 12, sm: 7 }}>
            <TextField label="Fornecedor *" value={form.fornecedor} onChange={alterar('fornecedor')}
              error={!!erros.fornecedor} helperText={erros.fornecedor} fullWidth
              slotProps={{ htmlInput: { maxLength: 150 } }} />
          </Grid>
          <Grid size={{ xs: 12, sm: 5 }}>
            <TextField label="Nota fiscal *" value={form.notaFiscal} onChange={alterar('notaFiscal')}
              error={!!erros.notaFiscal} helperText={erros.notaFiscal ?? 'Número ou chave de acesso.'} fullWidth
              slotProps={{ htmlInput: { maxLength: 44 } }} />
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <TextField label="Quantidade *" value={form.quantidade} onChange={alterar('quantidade')}
              error={!!erros.quantidade} helperText={erros.quantidade} fullWidth
              slotProps={{
                htmlInput: { inputMode: 'decimal' },
                input: unidade ? { endAdornment: <InputAdornment position="end">{unidade}</InputAdornment> } : undefined,
              }} />
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <TextField label="Fabricação *" type="date" value={form.dataFabricacao} onChange={alterar('dataFabricacao')}
              error={!!erros.dataFabricacao} helperText={erros.dataFabricacao} fullWidth
              slotProps={{ inputLabel: { shrink: true }, htmlInput: { max: hojeIso() } }} />
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <TextField label="Validade *" type="date" value={form.dataValidade} onChange={alterar('dataValidade')}
              error={!!erros.dataValidade} helperText={erros.dataValidade} fullWidth
              slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: form.dataFabricacao || undefined } }} />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={fechar} disabled={isPending}>Cancelar</Button>
        <Button variant="contained" onClick={registrar} disabled={isPending}>
          {isPending ? 'Registrando…' : 'Registrar entrada'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
