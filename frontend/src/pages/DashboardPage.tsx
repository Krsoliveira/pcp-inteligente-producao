import { useState } from 'react'
import Grid from '@mui/material/Grid2'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Typography from '@mui/material/Typography'
import Skeleton from '@mui/material/Skeleton'
import Alert from '@mui/material/Alert'
import ToggleButton from '@mui/material/ToggleButton'
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { pesquisarOrdens } from '../api/ordens'
import { consultarAtrasosPorCentro, consultarEntregasPorMes, consultarResumoProducao } from '../api/dashboard'
import { KpiCard } from '../components/KpiCard'
import { FilaDeProducaoChart } from '../components/FilaDeProducaoChart'
import { EntregasPorMesChart } from '../components/EntregasPorMesChart'
import { AtrasosPorCentroChart } from '../components/AtrasosPorCentroChart'
import { OrdemTable } from '../components/OrdemTable'
import { AtualizarStatusDialog } from '../components/AtualizarStatusDialog'
import { PageHeader } from '../components/PageHeader'
import { usePermissoes } from '../hooks/usePermissoes'
import { mensagemDeErroAoCarregar } from '../utils/mensagensDeErro'
import { intervaloDoMes } from '../utils/periodos'
import { montarBusca } from '../utils/url'
import type { EntregasDoMes, OrdemProducao, StatusOrdem } from '../types'

/** Cada indicador abre a listagem já filtrada, mas só para quem pode ver a tela de destino. */
const PARA_ORDENS = '/ordens/resultados'
const PERIODOS_EM_MESES = [3, 6, 12] as const
const ATUALIZACAO_EM_MS = 30_000

/** Das concluídas no período, quantas terminaram até o fim planejado; `null` quando não houve conclusões. */
export function aderenciaAoPrazo(entregas: EntregasDoMes[]) {
  const noPrazo = entregas.reduce((soma, m) => soma + m.noPrazo, 0)
  const concluidas = noPrazo + entregas.reduce((soma, m) => soma + m.comAtraso, 0)
  return concluidas === 0 ? null : { noPrazo, concluidas, percentual: Math.round((noPrazo / concluidas) * 100) }
}

export function DashboardPage() {
  const navigate = useNavigate()
  const { tem } = usePermissoes()
  const podeVerOrdens = tem('ORDEM_VISUALIZAR')
  const podeVerEstoque = tem('ESTOQUE_VISUALIZAR')
  const linkDeOrdens = (filtros: Record<string, string> = {}) =>
    podeVerOrdens ? { para: `${PARA_ORDENS}${montarBusca(filtros)}`, rotulo: 'Ver ordens' } : undefined
  const [ordemSelecionada, setOrdemSelecionada] = useState<OrdemProducao | null>(null)
  const [meses, setMeses] = useState<number>(12)

  // Os totais vêm calculados do servidor; só as 5 ordens mais recentes são baixadas.
  const resumoQuery = useQuery({ queryKey: ['ordens', 'resumo'], queryFn: consultarResumoProducao, refetchInterval: ATUALIZACAO_EM_MS })
  const recentesQuery = useQuery({
    queryKey: ['ordens', 'recentes'],
    queryFn: () => pesquisarOrdens({ pagina: 0, tamanho: 5 }),
    refetchInterval: ATUALIZACAO_EM_MS,
  })
  const entregasQuery = useQuery({
    queryKey: ['dashboard', 'entregas', meses],
    queryFn: () => consultarEntregasPorMes(meses),
    refetchInterval: ATUALIZACAO_EM_MS,
  })
  const atrasosQuery = useQuery({ queryKey: ['dashboard', 'atrasos'], queryFn: consultarAtrasosPorCentro, refetchInterval: ATUALIZACAO_EM_MS })

  const resumo = resumoQuery.data
  const isLoading = resumoQuery.isLoading
  const erro = resumoQuery.error ?? recentesQuery.error ?? entregasQuery.error ?? atrasosQuery.error
  const total = resumo?.totalOrdens ?? 0
  const atrasadas = resumo?.ordensAtrasadas ?? 0
  const emProducao = resumo?.ordensPorStatus.EM_PRODUCAO ?? 0
  const concluidas = resumo?.ordensPorStatus.CONCLUIDA ?? 0
  const lotesDisponiveis = resumo?.lotesDisponiveis ?? 0
  const recentes = recentesQuery.data?.itens ?? []
  const aderencia = aderenciaAoPrazo(entregasQuery.data ?? [])

  const abrirOrdens = (filtros: Record<string, string>) => navigate(`${PARA_ORDENS}${montarBusca(filtros)}`)

  const seletorDePeriodo = (
    <ToggleButtonGroup size="small" exclusive value={meses} onChange={(_, novo: number | null) => novo && setMeses(novo)} aria-label="Período das entregas">
      {PERIODOS_EM_MESES.map((m) => (
        <ToggleButton key={m} value={m} aria-label={`Últimos ${m} meses`} sx={{ px: 1.5, py: 0.25, textTransform: 'none' }}>
          {m} meses
        </ToggleButton>
      ))}
    </ToggleButtonGroup>
  )

  return (
    <Box>
      <PageHeader
        titulo="Dashboard"
        subtitulo="Visão geral da produção em tempo real"
      />

      {erro && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {mensagemDeErroAoCarregar(erro, 'Não foi possível carregar os dados do painel.')}
        </Alert>
      )}

      {/* KPIs */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        {isLoading ? (
          Array.from({ length: 6 }).map((_, i) => (
            <Grid key={i} size={{ xs: 12, sm: 6, md: 4, lg: 2 }}>
              <Skeleton variant="rounded" height={100} />
            </Grid>
          ))
        ) : (
          <>
            <Grid size={{ xs: 12, sm: 6, md: 4, lg: 2 }}>
              <KpiCard titulo="Total de ordens" valor={total} link={linkDeOrdens()} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 4, lg: 2 }}>
              <KpiCard titulo="Atrasadas" valor={atrasadas} cor={atrasadas > 0 ? '#DC2626' : undefined}
                subtitulo={atrasadas > 0 ? 'Abertas depois do fim planejado' : 'Nenhuma ordem atrasada'}
                link={linkDeOrdens({ atrasadas: 'true' })} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 4, lg: 2 }}>
              <KpiCard titulo="Em produção" valor={emProducao} link={linkDeOrdens({ status: 'EM_PRODUCAO' })} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 4, lg: 2 }}>
              <KpiCard titulo="Concluídas" valor={concluidas} link={linkDeOrdens({ status: 'CONCLUIDA' })} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 4, lg: 2 }}>
              <KpiCard titulo="Aderência ao prazo"
                valor={entregasQuery.isLoading ? '…' : aderencia ? `${aderencia.percentual}%` : '—'}
                subtitulo={aderencia
                  ? `${aderencia.noPrazo} de ${aderencia.concluidas} concluídas no prazo · ${meses} meses`
                  : entregasQuery.isLoading ? undefined : `Sem ordens concluídas nos últimos ${meses} meses`} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 4, lg: 2 }}>
              <KpiCard titulo="Lotes disponíveis" valor={lotesDisponiveis}
                link={podeVerEstoque ? { para: '/estoque', rotulo: 'Ver estoque' } : undefined} />
            </Grid>
          </>
        )}
      </Grid>

      {/* Fila e entregas */}
      <Grid container spacing={2} sx={{ mb: 2 }}>
        <Grid size={{ xs: 12, xl: 5 }}>
          {isLoading ? (
            <Skeleton variant="rounded" height={360} />
          ) : (
            <FilaDeProducaoChart
              contagem={resumo?.ordensPorStatus}
              onSelecionar={podeVerOrdens ? (status: StatusOrdem) => abrirOrdens({ status }) : undefined}
            />
          )}
        </Grid>
        <Grid size={{ xs: 12, xl: 7 }}>
          {entregasQuery.isLoading ? (
            <Skeleton variant="rounded" height={360} />
          ) : (
            <EntregasPorMesChart
              entregas={entregasQuery.data}
              acaoDoCabecalho={seletorDePeriodo}
              onSelecionarMes={podeVerOrdens ? (mes: string) => {
                const { de, ate } = intervaloDoMes(mes)
                abrirOrdens({ fimDe: de, fimAte: ate })
              } : undefined}
            />
          )}
        </Grid>
      </Grid>

      {/* Atrasos e ordens recentes */}
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, xl: 5 }}>
          {atrasosQuery.isLoading ? (
            <Skeleton variant="rounded" height={260} />
          ) : (
            <AtrasosPorCentroChart
              atrasos={atrasosQuery.data}
              onSelecionar={podeVerOrdens ? (centro: string) => abrirOrdens({ centro, atrasadas: 'true' }) : undefined}
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
