import { useState } from 'react'
import Grid from '@mui/material/Grid2'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Typography from '@mui/material/Typography'
import Skeleton from '@mui/material/Skeleton'
import Alert from '@mui/material/Alert'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { pesquisarOrdens } from '../api/ordens'
import { consultarResumoProducao } from '../api/dashboard'
import { KpiCard } from '../components/KpiCard'
import { FilaDeProducaoChart } from '../components/FilaDeProducaoChart'
import { OrdemTable } from '../components/OrdemTable'
import { AtualizarStatusDialog } from '../components/AtualizarStatusDialog'
import { PageHeader } from '../components/PageHeader'
import { usePermissoes } from '../hooks/usePermissoes'
import { mensagemDeErroAoCarregar } from '../utils/mensagensDeErro'
import { montarBusca } from '../utils/url'
import type { OrdemProducao, StatusOrdem } from '../types'

/** Cada indicador abre a listagem já filtrada, mas só para quem pode ver a tela de destino. */
const PARA_ORDENS = '/ordens/resultados'

export function DashboardPage() {
  const navigate = useNavigate()
  const { tem } = usePermissoes()
  const podeVerOrdens = tem('ORDEM_VISUALIZAR')
  const podeVerEstoque = tem('ESTOQUE_VISUALIZAR')
  const linkDeOrdens = (filtros: Record<string, string> = {}) =>
    podeVerOrdens ? { para: `${PARA_ORDENS}${montarBusca(filtros)}`, rotulo: 'Ver ordens' } : undefined
  const [ordemSelecionada, setOrdemSelecionada] = useState<OrdemProducao | null>(null)

  // Os totais vêm calculados do servidor; só as 5 ordens mais recentes são baixadas.
  const resumoQuery = useQuery({
    queryKey: ['ordens', 'resumo'],
    queryFn: consultarResumoProducao,
    refetchInterval: 30_000,
  })
  const recentesQuery = useQuery({
    queryKey: ['ordens', 'recentes'],
    queryFn: () => pesquisarOrdens({ pagina: 0, tamanho: 5 }),
    refetchInterval: 30_000,
  })

  const resumo = resumoQuery.data
  const isLoading = resumoQuery.isLoading
  const isError = resumoQuery.isError || recentesQuery.isError
  const total = resumo?.totalOrdens ?? 0
  const atrasadas = resumo?.ordensAtrasadas ?? 0
  const emProducao = resumo?.ordensPorStatus.EM_PRODUCAO ?? 0
  const concluidas = resumo?.ordensPorStatus.CONCLUIDA ?? 0
  const lotesDisponiveis = resumo?.lotesDisponiveis ?? 0
  const recentes = recentesQuery.data?.itens ?? []

  return (
    <Box>
      <PageHeader
        titulo="Dashboard"
        subtitulo="Visão geral da produção em tempo real"
      />

      {isError && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {mensagemDeErroAoCarregar(resumoQuery.error ?? recentesQuery.error, 'Não foi possível carregar os dados do painel.')}
        </Alert>
      )}

      {/* KPIs */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        {isLoading ? (
          Array.from({ length: 5 }).map((_, i) => (
            <Grid key={i} size={{ xs: 12, sm: 6, md: 4, lg: 2.4 }}>
              <Skeleton variant="rounded" height={100} />
            </Grid>
          ))
        ) : (
          <>
            <Grid size={{ xs: 12, sm: 6, md: 4, lg: 2.4 }}>
              <KpiCard titulo="Total de ordens" valor={total} link={linkDeOrdens()} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 4, lg: 2.4 }}>
              <KpiCard titulo="Atrasadas" valor={atrasadas} cor={atrasadas > 0 ? '#DC2626' : undefined}
                subtitulo={atrasadas > 0 ? 'Abertas depois do fim planejado' : 'Nenhuma ordem atrasada'}
                link={linkDeOrdens({ atrasadas: 'true' })} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 4, lg: 2.4 }}>
              <KpiCard titulo="Em produção" valor={emProducao} link={linkDeOrdens({ status: 'EM_PRODUCAO' })} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 4, lg: 2.4 }}>
              <KpiCard titulo="Concluídas" valor={concluidas} link={linkDeOrdens({ status: 'CONCLUIDA' })} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 4, lg: 2.4 }}>
              <KpiCard titulo="Lotes disponíveis" valor={lotesDisponiveis}
                link={podeVerEstoque ? { para: '/estoque', rotulo: 'Ver estoque' } : undefined} />
            </Grid>
          </>
        )}
      </Grid>

      {/* Gráfico + Ordens recentes */}
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, xl: 5 }}>
          {isLoading ? (
            <Skeleton variant="rounded" height={360} />
          ) : (
            <FilaDeProducaoChart
              contagem={resumo?.ordensPorStatus}
              onSelecionar={podeVerOrdens ? (status: StatusOrdem) => navigate(`${PARA_ORDENS}${montarBusca({ status })}`) : undefined}
            />
          )}
        </Grid>
        <Grid size={{ xs: 12, xl: 7 }}>
          <Card sx={{ height: '100%' }}>
            <CardContent sx={{ p: 3 }}>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Ordens Recentes
              </Typography>
              {isLoading ? (
                <Skeleton variant="rounded" height={240} />
              ) : (
                <OrdemTable ordens={recentes} onAtualizarStatus={setOrdemSelecionada} mostrarAcoes={false} />
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <AtualizarStatusDialog ordem={ordemSelecionada} onFechar={() => setOrdemSelecionada(null)} />
    </Box>
  )
}
