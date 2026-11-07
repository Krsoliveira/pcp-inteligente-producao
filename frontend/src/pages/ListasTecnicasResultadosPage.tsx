import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import Skeleton from '@mui/material/Skeleton'
import AccountTreeIcon from '@mui/icons-material/AccountTreeOutlined'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ativarListaTecnica, pesquisarListasTecnicas } from '../api/listasTecnicas'
import { listarMateriais } from '../api/materiais'
import { ListaTecnicaTable, colunasDeListas } from '../components/listas/ListaTecnicaTable'
import { RetirarDeUsoDialog } from '../components/listas/RetirarDeUsoDialog'
import { CamposFiltroListas, STATUS_LISTA_VALIDOS, nomeDoMaterial } from '../components/listas/CamposFiltroListas'
import { PageHeader } from '../components/PageHeader'
import { PaginacaoTabela } from '../components/PaginacaoTabela'
import { EmptyState } from '../components/EmptyState'
import { PainelDeConsulta } from '../components/consulta/PainelDeConsulta'
import { ResumoDaTabela, useTabelaInterativa } from '../components/consulta/TabelaInterativa'
import { useParametrosDaUrl } from '../hooks/useParametrosDaUrl'
import { usePesquisaPaginada } from '../hooks/usePesquisaPaginada'
import { mensagemDeErroAoCarregar, mensagemDeErroDaApi } from '../utils/mensagensDeErro'
import type { ListaTecnica, StatusListaTecnica } from '../types'
import { contagem } from '../utils/formatacao'
import { useSincronizarAoMudar } from '../hooks/useSincronizarAoMudar'

/** Segunda etapa: o resultado da consulta de listas técnicas, paginado no servidor, com filtro e ordenação por coluna na página. */
export function ListasTecnicasResultadosPage() {
  const queryClient = useQueryClient()
  const url = useParametrosDaUrl()
  const materialAplicado = url.texto('materialId')
  const statusBruto = url.texto('status') as StatusListaTecnica
  const statusAplicado = STATUS_LISTA_VALIDOS.includes(statusBruto) ? statusBruto : undefined

  const [materialId, setMaterialId] = useState(materialAplicado)
  const [status, setStatus] = useState<StatusListaTecnica | 'TODOS'>(statusAplicado ?? 'TODOS')
  useSincronizarAoMudar([materialAplicado, statusAplicado], () => { setMaterialId(materialAplicado); setStatus(statusAplicado ?? 'TODOS') })

  const { data: materiais = [] } = useQuery({ queryKey: ['materiais'], queryFn: listarMateriais })
  const comLista = materiais.filter((m) => m.tipo !== 'MATERIA_PRIMA')
  const materialPorId = new Map(materiais.map((m) => [m.id, m]))
  const [retirando, setRetirando] = useState<ListaTecnica | null>(null)
  const materialRetirando = retirando ? materialPorId.get(retirando.materialId) : undefined

  const { itens: listas, total, pagina, tamanho, isLoading, isError, error, isPlaceholderData, falhouSemDados } = usePesquisaPaginada({
    url,
    chave: ['listas-tecnicas', 'pesquisa', materialAplicado, statusAplicado],
    consultar: ({ pagina, tamanho }) => pesquisarListasTecnicas({ materialId: materialAplicado || undefined, status: statusAplicado, pagina, tamanho }),
  })
  const tabela = useTabelaInterativa(listas, colunasDeListas(materialPorId))
  const filtrando = materialAplicado !== '' || statusAplicado !== undefined

  const { mutate: ativar, error: erroAtivar } = useMutation({
    mutationFn: ativarListaTecnica,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['listas-tecnicas'] }),
  })

  const pesquisar = () => url.atualizar({ materialId: materialId || undefined, status: status === 'TODOS' ? undefined : status, pagina: undefined })
  const limpar = () => { setMaterialId(''); setStatus('TODOS'); url.atualizar({ materialId: undefined, status: undefined, pagina: undefined }) }

  return (
    <Box>
      <PageHeader
        titulo="Listas técnicas"
        subtitulo={isLoading ? '…' : falhouSemDados ? undefined : filtrando ? contagem(total, 'versão encontrada', 'versões encontradas') : contagem(total, 'versão cadastrada', 'versões cadastradas')}
        breadcrumbs={[{ label: 'Cadastros' }, { label: 'Listas técnicas', href: '/listas-tecnicas' }, { label: 'Resultados' }]}
      />

      <PainelDeConsulta onPesquisar={pesquisar} onLimpar={limpar}>
        <CamposFiltroListas materiais={comLista} materialId={materialId} onMaterial={setMaterialId} status={status} onStatus={setStatus} />
      </PainelDeConsulta>

      {isError && <Alert severity="error" sx={{ mb: 2 }}>{mensagemDeErroAoCarregar(error, 'Não foi possível carregar as listas técnicas.')}</Alert>}
      {erroAtivar && <Alert severity="error" sx={{ mb: 2 }}>{mensagemDeErroDaApi(erroAtivar, 'Não foi possível ativar a lista técnica.')}</Alert>}

      {!falhouSemDados && (
        <Card>
          {isLoading ? (
            <Skeleton variant="rounded" height={320} sx={{ m: 2 }} />
          ) : listas.length === 0 ? (
            <EmptyState
              Icone={AccountTreeIcon}
              titulo={filtrando ? 'Nenhuma lista técnica encontrada com os filtros' : 'Nenhuma lista técnica cadastrada'}
              descricao={filtrando ? 'Tente ajustar os filtros de busca.' : undefined}
            />
          ) : (
            <Box sx={{ opacity: isPlaceholderData ? 0.6 : 1, transition: 'opacity 150ms' }}>
              <ResumoDaTabela exibidas={tabela.linhas.length} naPagina={listas.length} filtrando={tabela.controle.filtrando} onLimpar={tabela.controle.limparFiltros} />
              <ListaTecnicaTable listas={tabela.linhas} materialPorId={materialPorId} onAtivar={ativar} onRetirarDeUso={setRetirando} controle={tabela.controle} />
              <PaginacaoTabela
                total={total}
                pagina={pagina}
                tamanho={tamanho}
                onPagina={(nova) => url.atualizar({ pagina: nova })}
                onTamanho={(novo) => url.atualizar({ tamanho: novo, pagina: undefined })}
              />
            </Box>
          )}
        </Card>
      )}
      <RetirarDeUsoDialog
        lista={retirando}
        nomeDoMaterial={materialRetirando ? nomeDoMaterial(materialRetirando) : 'Material'}
        onFechar={() => setRetirando(null)}
      />
    </Box>
  )
}
