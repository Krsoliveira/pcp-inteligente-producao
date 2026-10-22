import { useRef, useState } from 'react'
import Dialog from '@mui/material/Dialog'
import DialogTitle from '@mui/material/DialogTitle'
import DialogContent from '@mui/material/DialogContent'
import DialogActions from '@mui/material/DialogActions'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import IconButton from '@mui/material/IconButton'
import TextField from '@mui/material/TextField'
import MenuItem from '@mui/material/MenuItem'
import Alert from '@mui/material/Alert'
import Grid from '@mui/material/Grid2'
import Divider from '@mui/material/Divider'
import Typography from '@mui/material/Typography'
import Tooltip from '@mui/material/Tooltip'
import InputAdornment from '@mui/material/InputAdornment'
import AddIcon from '@mui/icons-material/Add'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { listarMateriais } from '../../api/materiais'
import { registrarNotaFiscal } from '../../api/notasFiscais'
import { formatarCodigoMaterial } from '../../utils/formatacao'
import type { ApiError, NotaFiscal } from '../../types'
import {
  type CabecalhoFormulario,
  type ErrosFormulario,
  type ItemFormulario,
  errosDoServidor,
  hojeIso,
  paraRequisicao,
  temErros,
  validarNotaFiscal,
} from './notaFiscalFormulario'

interface EntradaNotaFiscalDialogProps {
  aberto: boolean
  onFechar: () => void
  onRegistrada: (nota: NotaFiscal) => void
}

const CABECALHO_VAZIO: CabecalhoFormulario = { fornecedor: '', numero: '', dataEmissao: '', dataRecebimento: '' }
const SEM_ERROS: ErrosFormulario = { cabecalho: {}, itens: {} }

/**
 * Entrada de nota fiscal de compra (POST /api/v1/notas-fiscais): cabeçalho da nota e um
 * ou mais itens. Cada item vira um lote DISPONIVEL de matéria-prima, com o lote do
 * fornecedor, ligado à nota — é o que alimenta o estoque.
 */
export function EntradaNotaFiscalDialog({ aberto, onFechar, onRegistrada }: EntradaNotaFiscalDialogProps) {
  const proximaChave = useRef(1)
  const novoItem = (): ItemFormulario => ({
    chave: proximaChave.current++, materialId: '', numeroLote: '', quantidade: '', dataFabricacao: '', dataValidade: '',
  })
  const [cabecalho, setCabecalho] = useState<CabecalhoFormulario>(CABECALHO_VAZIO)
  const [itens, setItens] = useState<ItemFormulario[]>(() => [novoItem()])
  const [erros, setErros] = useState<ErrosFormulario>(SEM_ERROS)
  const queryClient = useQueryClient()

  const { data: materiais = [], isLoading: carregandoMateriais } = useQuery({
    queryKey: ['materiais'],
    queryFn: listarMateriais,
    enabled: aberto,
  })
  const materiasPrimas = materiais.filter((m) => m.tipo === 'MATERIA_PRIMA')
  const unidadeDe = (materialId: string) => materiasPrimas.find((m) => m.id === materialId)?.unidadeDeMedida

  const { mutate, isPending } = useMutation({
    mutationFn: registrarNotaFiscal,
    onSuccess: (nota) => {
      queryClient.invalidateQueries({ queryKey: ['notas-fiscais'] })
      queryClient.invalidateQueries({ queryKey: ['estoque'] })
      queryClient.invalidateQueries({ queryKey: ['lotes'] })
      fechar()
      onRegistrada(nota)
    },
    onError: (err: unknown) => {
      if (!axios.isAxiosError<ApiError>(err) || !err.response) {
        setErros({ ...SEM_ERROS, geral: 'Erro ao conectar com o servidor.' })
        return
      }
      const { status, data } = err.response
      if (status === 400 && data?.erros) setErros(errosDoServidor(data.erros, itens))
      else setErros({ ...SEM_ERROS, geral: data?.detail ?? 'Não foi possível registrar a nota fiscal.' })
    },
  })

  const alterarCabecalho = (campo: keyof CabecalhoFormulario) => (e: React.ChangeEvent<HTMLInputElement>) => {
    setCabecalho((atual) => ({ ...atual, [campo]: e.target.value }))
    setErros((atuais) => ({ ...atuais, cabecalho: { ...atuais.cabecalho, [campo]: undefined } }))
  }

  const alterarItem = (chave: number, campo: keyof Omit<ItemFormulario, 'chave'>) =>
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const valor = campo === 'numeroLote' ? e.target.value.toUpperCase() : e.target.value
      setItens((atuais) => atuais.map((i) => (i.chave === chave ? { ...i, [campo]: valor } : i)))
      setErros((atuais) => ({
        ...atuais,
        itens: { ...atuais.itens, [chave]: { ...atuais.itens[chave], [campo]: undefined } },
      }))
    }

  const removerItem = (chave: number) => setItens((atuais) => atuais.filter((i) => i.chave !== chave))

  const fechar = () => {
    setCabecalho(CABECALHO_VAZIO)
    setItens([novoItem()])
    setErros(SEM_ERROS)
    onFechar()
  }

  const registrar = () => {
    const encontrados = validarNotaFiscal(cabecalho, itens)
    setErros(encontrados)
    if (!temErros(encontrados)) mutate(paraRequisicao(cabecalho, itens))
  }

  const hoje = hojeIso()

  return (
    <Dialog open={aberto} onClose={fechar} fullWidth maxWidth="md">
      <DialogTitle>Entrada de nota fiscal</DialogTitle>
      <DialogContent dividers>
        {erros.geral && <Alert severity="error" sx={{ mb: 2 }}>{erros.geral}</Alert>}
        {!carregandoMateriais && materiasPrimas.length === 0 && (
          <Alert severity="info" sx={{ mb: 2 }}>
            Nenhuma matéria-prima cadastrada. Cadastre em Materiais antes de registrar notas.
          </Alert>
        )}

        <Typography variant="overline" color="text.secondary">Nota fiscal</Typography>
        <Grid container spacing={2} sx={{ mb: 2, mt: 0.5 }}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField label="Fornecedor *" value={cabecalho.fornecedor} onChange={alterarCabecalho('fornecedor')}
              error={!!erros.cabecalho.fornecedor} helperText={erros.cabecalho.fornecedor} fullWidth
              slotProps={{ htmlInput: { maxLength: 150 } }} />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField label="Número da nota *" value={cabecalho.numero} onChange={alterarCabecalho('numero')}
              error={!!erros.cabecalho.numero} helperText={erros.cabecalho.numero ?? 'Número ou chave de acesso.'}
              fullWidth slotProps={{ htmlInput: { maxLength: 44 } }} />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField label="Emissão *" type="date" value={cabecalho.dataEmissao} onChange={alterarCabecalho('dataEmissao')}
              error={!!erros.cabecalho.dataEmissao} helperText={erros.cabecalho.dataEmissao} fullWidth
              slotProps={{ inputLabel: { shrink: true }, htmlInput: { max: hoje } }} />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField label="Recebimento *" type="date" value={cabecalho.dataRecebimento}
              onChange={alterarCabecalho('dataRecebimento')}
              error={!!erros.cabecalho.dataRecebimento}
              helperText={erros.cabecalho.dataRecebimento ?? 'Dia em que o material chegou.'} fullWidth
              slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: cabecalho.dataEmissao || undefined, max: hoje } }} />
          </Grid>
        </Grid>

        <Divider sx={{ mb: 1.5 }} />
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
          <Typography variant="overline" color="text.secondary">
            Itens ({itens.length}) — cada item vira um lote no estoque
          </Typography>
          <Button size="small" startIcon={<AddIcon />} onClick={() => setItens((atuais) => [...atuais, novoItem()])}>
            Adicionar item
          </Button>
        </Box>

        {itens.map((item, indice) => {
          const e = erros.itens[item.chave] ?? {}
          const unidade = unidadeDe(item.materialId)
          return (
            <Box key={item.chave} sx={{ p: 1.5, mb: 1.5, border: '1px solid', borderColor: 'divider', borderRadius: 1.5 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="caption" fontWeight={600} color="text.secondary">Item {indice + 1}</Typography>
                <Tooltip title={itens.length === 1 ? 'A nota precisa de pelo menos um item' : 'Remover item'}>
                  <span>
                    <IconButton size="small" aria-label={`Remover item ${indice + 1}`}
                      onClick={() => removerItem(item.chave)} disabled={itens.length === 1}>
                      <DeleteOutlineIcon fontSize="small" />
                    </IconButton>
                  </span>
                </Tooltip>
              </Box>
              <Grid container spacing={1.5}>
                <Grid size={{ xs: 12, sm: 7 }}>
                  <TextField select label="Matéria-prima *" value={item.materialId}
                    onChange={alterarItem(item.chave, 'materialId')} error={!!e.materialId} helperText={e.materialId}
                    fullWidth disabled={carregandoMateriais}>
                    {materiasPrimas.map((m) => (
                      <MenuItem key={m.id} value={m.id}>{formatarCodigoMaterial(m.codigo)} — {m.descricao}</MenuItem>
                    ))}
                  </TextField>
                </Grid>
                <Grid size={{ xs: 12, sm: 5 }}>
                  <TextField label="Lote do fornecedor *" value={item.numeroLote}
                    onChange={alterarItem(item.chave, 'numeroLote')} error={!!e.numeroLote}
                    helperText={e.numeroLote ?? 'Como impresso na embalagem (até 20).'} fullWidth
                    slotProps={{ htmlInput: { maxLength: 20, style: { fontFamily: 'monospace' } } }} />
                </Grid>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <TextField label="Quantidade *" value={item.quantidade} onChange={alterarItem(item.chave, 'quantidade')}
                    error={!!e.quantidade} helperText={e.quantidade} fullWidth
                    slotProps={{
                      htmlInput: { inputMode: 'decimal' },
                      input: unidade ? { endAdornment: <InputAdornment position="end">{unidade}</InputAdornment> } : undefined,
                    }} />
                </Grid>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <TextField label="Fabricação *" type="date" value={item.dataFabricacao}
                    onChange={alterarItem(item.chave, 'dataFabricacao')} error={!!e.dataFabricacao}
                    helperText={e.dataFabricacao} fullWidth
                    slotProps={{ inputLabel: { shrink: true }, htmlInput: { max: cabecalho.dataRecebimento || hoje } }} />
                </Grid>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <TextField label="Validade *" type="date" value={item.dataValidade}
                    onChange={alterarItem(item.chave, 'dataValidade')} error={!!e.dataValidade}
                    helperText={e.dataValidade} fullWidth
                    slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: item.dataFabricacao || undefined } }} />
                </Grid>
              </Grid>
            </Box>
          )
        })}
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={fechar} disabled={isPending}>Cancelar</Button>
        <Button variant="contained" onClick={registrar} disabled={isPending}>
          {isPending ? 'Registrando…' : 'Registrar nota'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
