import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Skeleton from '@mui/material/Skeleton'
import AddIcon from '@mui/icons-material/Add'
import ListAltIcon from '@mui/icons-material/ListAlt'
import { useQuery } from '@tanstack/react-query'
import { pesquisarOrdens } from '../api/ordens'
import { OrdemTable } from '../components/OrdemTable'
import { CriarOrdemDialog } from '../components/CriarOrdemDialog'
import { AtualizarStatusDialog } from '../components/AtualizarStatusDialog'
import { PageHeader } from '../components/PageHeader'
import { EmptyState } from '../components/EmptyState'
import { PainelDeConsulta } from '../components/consulta/PainelDeConsulta'
import { CartaoDeRecentes } from '../components/consulta/CartaoDeRecentes'
import { CamposFiltroOrdens } from '../components/ordens/CamposFiltroOrdens'
import { mensagemDeErroAoCarregar } from '../utils/mensagensDeErro'
import { montarBusca } from '../utils/url'
import type { OrdemProducao, StatusOrdem } from '../types'

/** Primeira etapa: escolher o que consultar (ou abrir uma das 10 ordens mais recentes). */
export function OrdensPage() {
  const navigate = useNavigate()
  const [dialogCriarAberto, setDialogCriarAberto] = useState(false)
  const [ordemSelecionada, setOrdemSelecionada] = useState<OrdemProducao | null>(null)
  const [busca, setBusca] = useState('')
  const [status, setStatus] = useState<StatusOrdem | 'TODAS'>('TODAS')

  const recentes = useQuery({
    queryKey: ['ordens', 'recentes', 10],
    queryFn: () => pesquisarOrdens({ pagina: 0, tamanho: 10 }),
  })

  const verResultados = (filtros: Record<string, string | undefined> = {}) =>
    navigate({ pathname: '/ordens/resultados', search: montarBusca(filtros) })
  const pesquisar = () => verResultados({ busca: busca.trim(), status: status === 'TODAS' ? undefined : status })
  const limpar = () => { setBusca(''); setStatus('TODAS') }

  return (
    <Box>
      <PageHeader
        titulo="Ordens de Produção"
        subtitulo="Pesquise por código ou status, ou abra uma das mais recentes"
        breadcrumbs={[{ label: 'Produção' }, { label: 'Ordens' }]}
        action={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialogCriarAberto(true)}>
            Nova Ordem
          </Button>
        }
      />

      <PainelDeConsulta onPesquisar={pesquisar} onLimpar={limpar}>
        <CamposFiltroOrdens busca={busca} onBusca={setBusca} status={status} onStatus={setStatus} />
      </PainelDeConsulta>

      <CartaoDeRecentes titulo="10 ordens mais recentes" onVerTodas={() => verResultados()}>
        {recentes.isLoading ? (
          <Skeleton variant="rounded" height={320} sx={{ m: 2 }} />
        ) : recentes.isError ? (
          <Alert severity="error" sx={{ m: 2 }}>{mensagemDeErroAoCarregar(recentes.error, 'Não foi possível carregar as ordens.')}</Alert>
        ) : (recentes.data?.itens.length ?? 0) === 0 ? (
          <EmptyState Icone={ListAltIcon} titulo="Nenhuma ordem cadastrada" acaoLabel="Criar primeira ordem" onAcao={() => setDialogCriarAberto(true)} />
        ) : (
          <OrdemTable ordens={recentes.data!.itens} onAtualizarStatus={setOrdemSelecionada} />
        )}
      </CartaoDeRecentes>

      <CriarOrdemDialog aberto={dialogCriarAberto} onFechar={() => setDialogCriarAberto(false)} />
      <AtualizarStatusDialog ordem={ordemSelecionada} onFechar={() => setOrdemSelecionada(null)} />
    </Box>
  )
}
