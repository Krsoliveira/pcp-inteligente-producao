import { useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Divider from '@mui/material/Divider'
import Grid from '@mui/material/Grid2'
import IconButton from '@mui/material/IconButton'
import MenuItem from '@mui/material/MenuItem'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { cadastrarListaTecnica } from '../../api/listasTecnicas'
import { listarMateriais } from '../../api/materiais'
import type { CadastrarListaTecnicaRequest, ListaTecnica, TipoMaterial } from '../../types'
import { formatarCodigoMaterial } from '../../utils/formatacao'
import { mensagemDeErroDaApi } from '../../utils/mensagensDeErro'
import { sugerirProximaVersao } from '../../utils/versoes'

interface ItemForm { materialComponenteId: string; quantidadePlanejada: string; unidadeDeMedida: string }

const ITEM_VAZIO: ItemForm = { materialComponenteId: '', quantidadePlanejada: '', unidadeDeMedida: 'un' }

interface CriarListaTecnicaDialogProps {
  aberto: boolean
  onFechar: () => void
  /**
   * Versão a copiar: o material fica fixo (a nova versão entra na lista dele), os componentes
   * vêm preenchidos para ajustar e o rótulo da versão é sugerido.
   */
  modelo?: ListaTecnica
  /** Rótulos das versões que o material já tem, para sugerir o próximo ("v3"). */
  versoesExistentes?: string[]
  /** Chamado depois de criar, com o material e o rótulo da versão nova. */
  onCriada?: (materialId: string, versao: string) => void
}

/** Cadastro de uma versão de lista técnica, do zero ou copiando outra versão do mesmo material. */
export function CriarListaTecnicaDialog({ aberto, onFechar, modelo, versoesExistentes = [], onCriada }: CriarListaTecnicaDialogProps) {
  const [materialId, setMaterialId] = useState('')
  const [versao, setVersao] = useState('')
  const [itens, setItens] = useState<ItemForm[]>([ITEM_VAZIO])
  const [erro, setErro] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const { data: materiais = [] } = useQuery({ queryKey: ['materiais'], queryFn: listarMateriais, enabled: aberto })
  const produtivos = materiais.filter((m) => (m.tipo as TipoMaterial) !== 'MATERIA_PRIMA')

  // Cada abertura recomeça: vazio, ou preenchido a partir do modelo.
  useEffect(() => {
    if (!aberto) return
    setErro(null)
    if (modelo) {
      setMaterialId(modelo.materialId)
      setVersao(sugerirProximaVersao(versoesExistentes))
      setItens(modelo.itens.map((i) => ({
        materialComponenteId: i.materialComponenteId,
        quantidadePlanejada: String(i.quantidadePlanejada),
        unidadeDeMedida: i.unidadeDeMedida,
      })))
    } else {
      setMaterialId('')
      setVersao('')
      setItens([ITEM_VAZIO])
    }
  }, [aberto, modelo?.id]) // eslint-disable-line react-hooks/exhaustive-deps

  const { mutate, isPending } = useMutation({
    mutationFn: (payload: CadastrarListaTecnicaRequest) => cadastrarListaTecnica(payload),
    onSuccess: (_resultado, payload) => {
      queryClient.invalidateQueries({ queryKey: ['listas-tecnicas-todas'] })
      queryClient.invalidateQueries({ queryKey: ['listas-tecnicas'] })
      onFechar()
      onCriada?.(payload.materialId, payload.versao)
    },
    onError: (err: unknown) => {
      setErro(mensagemDeErroDaApi(err, 'Não foi possível cadastrar a lista técnica. Tente novamente.'))
    },
  })

  const handleSubmit = () => {
    if (!materialId || !versao.trim()) { setErro('Material e versão são obrigatórios.'); return }
    const itensValidos = itens.filter((i) => i.materialComponenteId && i.quantidadePlanejada)
    if (itensValidos.length === 0) { setErro('Adicione ao menos um componente.'); return }
    mutate({
      materialId,
      versao: versao.trim(),
      itens: itensValidos.map((i) => ({
        materialComponenteId: i.materialComponenteId,
        quantidadePlanejada: Number(i.quantidadePlanejada),
        unidadeDeMedida: i.unidadeDeMedida,
      })),
    })
  }

  const addItem = () => setItens((prev) => [...prev, ITEM_VAZIO])
  const removeItem = (idx: number) => setItens((prev) => prev.filter((_, i) => i !== idx))
  const updateItem = (idx: number, campo: keyof ItemForm, valor: string) =>
    setItens((prev) => prev.map((item, i) => (i === idx ? { ...item, [campo]: valor } : item)))

  return (
    <Dialog open={aberto} onClose={onFechar} fullWidth maxWidth="md">
      <DialogTitle>{modelo ? `Nova versão a partir da ${modelo.versao}` : 'Nova Lista Técnica (BOM)'}</DialogTitle>
      <DialogContent dividers>
        {erro && <Alert severity="error" sx={{ mb: 2 }}>{erro}</Alert>}
        {modelo && (
          <Alert severity="info" sx={{ mb: 2 }}>
            Os componentes da versão {modelo.versao} foram copiados. Ajuste o que mudou; a nova versão nasce
            em revisão e só é usada nas ordens depois de ativada.
          </Alert>
        )}
        <Grid container spacing={2} sx={{ mt: 0 }}>
          <Grid size={{ xs: 12, sm: 8 }}>
            <TextField
              select label="Material *" value={materialId} onChange={(e) => setMaterialId(e.target.value)} fullWidth
              disabled={!!modelo}
              helperText={modelo ? 'A nova versão entra na lista deste material.' : 'Produto acabado ou semiacabado que será fabricado.'}
            >
              {produtivos.map((m) => (
                <MenuItem key={m.id} value={m.id}>{formatarCodigoMaterial(m.codigo)} — {m.descricao}</MenuItem>
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
              {materiais.map((m) => (
                <MenuItem key={m.id} value={m.id}>{formatarCodigoMaterial(m.codigo)} — {m.descricao}</MenuItem>
              ))}
            </TextField>
            <TextField label="Qtd" type="number" value={item.quantidadePlanejada} onChange={(e) => updateItem(idx, 'quantidadePlanejada', e.target.value)} sx={{ width: 90 }} inputProps={{ min: 0.0001, step: 0.0001 }} />
            <TextField label="Un." value={item.unidadeDeMedida} onChange={(e) => updateItem(idx, 'unidadeDeMedida', e.target.value)} sx={{ width: 70 }} inputProps={{ maxLength: 10 }} />
            <IconButton aria-label={`Remover componente ${idx + 1}`} onClick={() => removeItem(idx)} disabled={itens.length === 1} sx={{ mt: 0.5 }}>
              <DeleteOutlineIcon fontSize="small" />
            </IconButton>
          </Box>
        ))}
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onFechar} disabled={isPending}>Cancelar</Button>
        <Button variant="contained" onClick={handleSubmit} disabled={isPending}>
          {isPending ? 'Salvando…' : modelo ? 'Criar versão' : 'Criar Lista'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
