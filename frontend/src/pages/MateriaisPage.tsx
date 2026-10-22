import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { pesquisarMateriais } from '../api/materiais'
import { ResumoDaTabela, useTabelaInterativa } from '../components/consulta/TabelaInterativa'
import { MaterialTable, colunasDeMateriais } from '../components/materiais/MaterialTable'
import { CamposFiltroMateriais } from '../components/materiais/CamposFiltroMateriais'
import { PainelDeConsulta } from '../components/consulta/PainelDeConsulta'
import { CartaoDeRecentes } from '../components/consulta/CartaoDeRecentes'
import { mensagemDeErroAoCarregar } from '../utils/mensagensDeErro'
import { montarBusca } from '../utils/url'
import Box from '@mui/material/Box'
import TextField from '@mui/material/TextField'
import MenuItem from '@mui/material/MenuItem'
import Button from '@mui/material/Button'
import Skeleton from '@mui/material/Skeleton'
import Alert from '@mui/material/Alert'
import Dialog from '@mui/material/Dialog'
import DialogTitle from '@mui/material/DialogTitle'
import DialogContent from '@mui/material/DialogContent'
import DialogActions from '@mui/material/DialogActions'
import Grid from '@mui/material/Grid2'
import AddIcon from '@mui/icons-material/Add'
import InventoryIcon from '@mui/icons-material/Inventory2Outlined'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { cadastrarMaterial } from '../api/materiais'
import { PageHeader } from '../components/PageHeader'
import { EmptyState } from '../components/EmptyState'
import type { TipoMaterial, CadastrarMaterialRequest } from '../types'
import { TIPO_MATERIAL_LABEL } from '../types'
import { mensagemDeErroDaApi } from '../utils/mensagensDeErro'

/** Primeira etapa: escolher o que consultar (ou abrir um dos 10 materiais mais recentes). */
export function MateriaisPage() {
  const navigate = useNavigate()
  const [dialogAberto, setDialogAberto] = useState(false)
  const [busca, setBusca] = useState('')
  const [tipo, setTipo] = useState<TipoMaterial | 'TODOS'>('TODOS')

  const recentes = useQuery({
    queryKey: ['materiais', 'recentes', 10],
    queryFn: () => pesquisarMateriais({ pagina: 0, tamanho: 10 }),
  })

  const itensRecentes = recentes.data?.itens ?? []
  const tabela = useTabelaInterativa(itensRecentes, colunasDeMateriais)

  const verResultados = (filtros: Record<string, string | undefined> = {}) =>
    navigate({ pathname: '/materiais/resultados', search: montarBusca(filtros) })
  const pesquisar = () => verResultados({ busca: busca.trim(), tipo: tipo === 'TODOS' ? undefined : tipo })
  const limpar = () => { setBusca(''); setTipo('TODOS') }

  return (
    <Box>
      <PageHeader
        titulo="Materiais"
        subtitulo="Pesquise por código, descrição ou tipo, ou veja os mais recentes"
        breadcrumbs={[{ label: 'Cadastros' }, { label: 'Materiais' }]}
        action={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialogAberto(true)}>
            Novo Material
          </Button>
        }
      />

      <PainelDeConsulta onPesquisar={pesquisar} onLimpar={limpar}>
        <CamposFiltroMateriais busca={busca} onBusca={setBusca} tipo={tipo} onTipo={setTipo} />
      </PainelDeConsulta>

      <CartaoDeRecentes titulo="10 materiais mais recentes" onVerTodas={() => verResultados()}>
        {recentes.isLoading ? (
          <Skeleton variant="rounded" height={320} sx={{ m: 2 }} />
        ) : recentes.isError ? (
          <Alert severity="error" sx={{ m: 2 }}>{mensagemDeErroAoCarregar(recentes.error, 'Não foi possível carregar os materiais.')}</Alert>
        ) : (recentes.data?.itens.length ?? 0) === 0 ? (
          <EmptyState Icone={InventoryIcon} titulo="Nenhum material cadastrado" descricao="Cadastre materiais: produtos acabados, semiacabados e matérias-primas." acaoLabel="Novo Material" onAcao={() => setDialogAberto(true)} />
        ) : (
          <>
          <ResumoDaTabela exibidas={tabela.linhas.length} naPagina={itensRecentes.length} filtrando={tabela.controle.filtrando} onLimpar={tabela.controle.limparFiltros} />
          <MaterialTable materiais={tabela.linhas} controle={tabela.controle} />
          </>
        )}
      </CartaoDeRecentes>

      <CriarMaterialDialog aberto={dialogAberto} onFechar={() => setDialogAberto(false)} />
    </Box>
  )
}

// ---- Dialog de cadastro ----

interface CriarMaterialDialogProps { aberto: boolean; onFechar: () => void }

const VAZIO: CadastrarMaterialRequest = { descricao: '', tipo: 'PRODUTO_ACABADO', unidadeDeMedida: '' }

/** Faixa de código por tipo (ADR-0012) — o backend gera o próximo livre. */
const FAIXA_CODIGO: Record<TipoMaterial, string> = {
  PRODUTO_ACABADO: '103.xxx.xxx',
  SEMIACABADO: '105.xxx.xxx',
  MATERIA_PRIMA: '110.xxx.xxx',
}

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
      setErro(mensagemDeErroDaApi(err, 'Não foi possível cadastrar o material. Tente novamente.'))
    },
  })

  const handleSubmit = () => {
    if (!form.descricao.trim() || !form.unidadeDeMedida.trim()) { setErro('Preencha todos os campos.'); return }
    mutate(form)
  }

  const handleFechar = () => { setForm(VAZIO); setErro(null); onFechar() }

  return (
    <Dialog open={aberto} onClose={handleFechar} fullWidth maxWidth="sm">
      <DialogTitle>Novo Material</DialogTitle>
      <DialogContent dividers>
        {erro && <Alert severity="error" sx={{ mb: 2 }}>{erro}</Alert>}
        <Grid container spacing={2} sx={{ mt: 0 }}>
          <Grid size={12}>
            <TextField label="Descrição *" value={form.descricao} onChange={(e) => setForm(p => ({ ...p, descricao: e.target.value }))} fullWidth inputProps={{ maxLength: 200 }} autoFocus />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField select label="Tipo *" value={form.tipo} onChange={(e) => setForm(p => ({ ...p, tipo: e.target.value as TipoMaterial }))} fullWidth helperText={`Código gerado automaticamente: ${FAIXA_CODIGO[form.tipo]}`}>
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
