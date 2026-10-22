import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { pesquisarListasTecnicas } from '../api/listasTecnicas'
import { ListaTecnicaTable } from '../components/listas/ListaTecnicaTable'
import { CamposFiltroListas, nomeDoMaterial } from '../components/listas/CamposFiltroListas'
import { PainelDeConsulta } from '../components/consulta/PainelDeConsulta'
import { CartaoDeRecentes } from '../components/consulta/CartaoDeRecentes'
import { mensagemDeErroAoCarregar } from '../utils/mensagensDeErro'
import { montarBusca } from '../utils/url'
import Box from '@mui/material/Box'
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
import Divider from '@mui/material/Divider'
import AddIcon from '@mui/icons-material/Add'
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import AccountTreeIcon from '@mui/icons-material/AccountTreeOutlined'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { cadastrarListaTecnica, ativarListaTecnica } from '../api/listasTecnicas'
import { listarMateriais } from '../api/materiais'
import { PageHeader } from '../components/PageHeader'
import { EmptyState } from '../components/EmptyState'
import { formatarCodigoMaterial } from '../utils/formatacao'
import type { CadastrarListaTecnicaRequest, StatusListaTecnica, TipoMaterial } from '../types'
import { mensagemDeErroDaApi } from '../utils/mensagensDeErro'

/** Primeira etapa: escolher o que consultar (ou abrir uma das 10 listas mais recentes). */
export function ListasTecnicasPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [dialogAberto, setDialogAberto] = useState(false)
  const [materialId, setMaterialId] = useState('')
  const [status, setStatus] = useState<StatusListaTecnica | 'TODOS'>('TODOS')

  const recentes = useQuery({
    queryKey: ['listas-tecnicas', 'recentes', 10],
    queryFn: () => pesquisarListasTecnicas({ pagina: 0, tamanho: 10 }),
  })
  const { data: materiais = [] } = useQuery({ queryKey: ['materiais'], queryFn: listarMateriais })
  const comLista = materiais.filter((m) => m.tipo !== 'MATERIA_PRIMA')
  const nome = (id: string) => {
    const m = materiais.find((x) => x.id === id)
    return m ? nomeDoMaterial(m) : `${id.substring(0, 8)}…`
  }

  const { mutate: ativar, error: erroAtivar } = useMutation({
    mutationFn: ativarListaTecnica,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['listas-tecnicas'] }),
  })

  const verResultados = (filtros: Record<string, string | undefined> = {}) =>
    navigate({ pathname: '/listas-tecnicas/resultados', search: montarBusca(filtros) })
  const pesquisar = () => verResultados({ materialId: materialId || undefined, status: status === 'TODOS' ? undefined : status })
  const limpar = () => { setMaterialId(''); setStatus('TODOS') }

  return (
    <Box>
      <PageHeader
        titulo="Listas Técnicas"
        subtitulo="Pesquise por material ou status, ou abra uma das mais recentes"
        breadcrumbs={[{ label: 'Cadastros' }, { label: 'Listas Técnicas' }]}
        action={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialogAberto(true)}>
            Nova Lista
          </Button>
        }
      />

      <PainelDeConsulta onPesquisar={pesquisar} onLimpar={limpar}>
        <CamposFiltroListas materiais={comLista} materialId={materialId} onMaterial={setMaterialId} status={status} onStatus={setStatus} />
      </PainelDeConsulta>

      {erroAtivar && <Alert severity="error" sx={{ mb: 2 }}>{mensagemDeErroDaApi(erroAtivar, 'Não foi possível ativar a lista técnica.')}</Alert>}

      <CartaoDeRecentes titulo="10 listas técnicas mais recentes" onVerTodas={() => verResultados()}>
        {recentes.isLoading ? (
          <Skeleton variant="rounded" height={320} sx={{ m: 2 }} />
        ) : recentes.isError ? (
          <Alert severity="error" sx={{ m: 2 }}>{mensagemDeErroAoCarregar(recentes.error, 'Não foi possível carregar as listas técnicas.')}</Alert>
        ) : (recentes.data?.itens.length ?? 0) === 0 ? (
          <EmptyState
            Icone={AccountTreeIcon}
            titulo="Nenhuma lista técnica cadastrada"
            descricao="As listas técnicas (BOM) definem quais materiais e quantidades são necessários para produzir cada produto."
            acaoLabel="Criar lista técnica"
            onAcao={() => setDialogAberto(true)}
          />
        ) : (
          <ListaTecnicaTable listas={recentes.data!.itens} nomeDoMaterial={nome} onAtivar={ativar} />
        )}
      </CartaoDeRecentes>

      <CriarListaTecnicaDialog aberto={dialogAberto} onFechar={() => setDialogAberto(false)} />
    </Box>
  )
}

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
      setErro(mensagemDeErroDaApi(err, 'Não foi possível cadastrar a lista técnica. Tente novamente.'))
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
              {componentes.map((m) => (
                <MenuItem key={m.id} value={m.id}>{formatarCodigoMaterial(m.codigo)} — {m.descricao}</MenuItem>
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
