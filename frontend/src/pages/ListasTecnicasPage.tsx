import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { pesquisarListasTecnicas } from '../api/listasTecnicas'
import { ResumoDaTabela, useTabelaInterativa } from '../components/consulta/TabelaInterativa'
import { ListaTecnicaTable, colunasDeListas } from '../components/listas/ListaTecnicaTable'
import { RetirarDeUsoDialog } from '../components/listas/RetirarDeUsoDialog'
import { CriarListaTecnicaDialog } from '../components/listas/CriarListaTecnicaDialog'
import { CamposFiltroListas, nomeDoMaterial } from '../components/listas/CamposFiltroListas'
import { PainelDeConsulta } from '../components/consulta/PainelDeConsulta'
import { CartaoDeRecentes } from '../components/consulta/CartaoDeRecentes'
import { mensagemDeErroAoCarregar } from '../utils/mensagensDeErro'
import { montarBusca } from '../utils/url'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Skeleton from '@mui/material/Skeleton'
import Alert from '@mui/material/Alert'
import AddIcon from '@mui/icons-material/Add'
import AccountTreeIcon from '@mui/icons-material/AccountTreeOutlined'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ativarListaTecnica } from '../api/listasTecnicas'
import { listarMateriais } from '../api/materiais'
import { PageHeader } from '../components/PageHeader'
import { EmptyState } from '../components/EmptyState'
import type { ListaTecnica, StatusListaTecnica } from '../types'
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
  const materialPorId = new Map(materiais.map((m) => [m.id, m]))
  const [retirando, setRetirando] = useState<ListaTecnica | null>(null)
  const materialRetirando = retirando ? materialPorId.get(retirando.materialId) : undefined

  const { mutate: ativar, error: erroAtivar } = useMutation({
    mutationFn: ativarListaTecnica,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['listas-tecnicas'] }),
  })

  const itensRecentes = recentes.data?.itens ?? []
  const tabela = useTabelaInterativa(itensRecentes, colunasDeListas(materialPorId))

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
          <>
          <ResumoDaTabela exibidas={tabela.linhas.length} naPagina={itensRecentes.length} filtrando={tabela.controle.filtrando} onLimpar={tabela.controle.limparFiltros} />
          <ListaTecnicaTable listas={tabela.linhas} materialPorId={materialPorId} onAtivar={ativar} onRetirarDeUso={setRetirando} controle={tabela.controle} />
          </>
        )}
      </CartaoDeRecentes>

      <CriarListaTecnicaDialog aberto={dialogAberto} onFechar={() => setDialogAberto(false)} />
      <RetirarDeUsoDialog
        lista={retirando}
        nomeDoMaterial={materialRetirando ? nomeDoMaterial(materialRetirando) : 'Material'}
        onFechar={() => setRetirando(null)}
      />
    </Box>
  )
}
