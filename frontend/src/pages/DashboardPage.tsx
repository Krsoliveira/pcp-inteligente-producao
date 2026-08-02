import Grid from '@mui/material/Grid2'
import Typography from '@mui/material/Typography'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Skeleton from '@mui/material/Skeleton'
import Alert from '@mui/material/Alert'
import FactoryIcon from '@mui/icons-material/Factory'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import BuildIcon from '@mui/icons-material/Build'
import { useQuery } from '@tanstack/react-query'
import { listarOrdens } from '../api/ordens'
import { KpiCard } from '../components/KpiCard'
import { StatusPieChart } from '../components/StatusPieChart'
import { OrdemTable } from '../components/OrdemTable'
import { AtualizarStatusDialog } from '../components/AtualizarStatusDialog'
import { useState } from 'react'
import type { OrdemProducao } from '../types'

export function DashboardPage() {
  const [ordemSelecionada, setOrdemSelecionada] = useState<OrdemProducao | null>(null)

  const { data: ordens = [], isLoading, isError } = useQuery({
    queryKey: ['ordens'],
    queryFn: listarOrdens,
    refetchInterval: 30_000, // atualiza a cada 30s
  })

  const total = ordens.length
  const atrasadas = ordens.filter((o) => o.atrasada).length
  const emProducao = ordens.filter((o) => o.status === 'EM_PRODUCAO').length
  const concluidas = ordens.filter((o) => o.status === 'CONCLUIDA').length

  // últimas 5 ordens (mais recentes primeiro)
  const recentes = [...ordens]
    .sort((a, b) => new Date(b.criadaEm).getTime() - new Date(a.criadaEm).getTime())
    .slice(0, 5)

  return (
    <Box>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h5">Dashboard</Typography>
        <Typography variant="body2" color="text.secondary">
          Visão geral da produção em tempo real
        </Typography>
      </Box>

      {isError && (
        <Alert severity="error" sx={{ mb: 3 }}>
          Não foi possível carregar os dados. Verifique sua conexão.
        </Alert>
      )}

      {/* KPIs */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        {isLoading
          ? Array.from({ length: 4 }).map((_, i) => (
              <Grid key={i} size={{ xs: 12, sm: 6, lg: 3 }}>
                <Skeleton variant="rounded" height={110} />
              </Grid>
            ))
          : [
              {
                titulo: 'Total de Ordens',
                valor: total,
                Icone: FactoryIcon,
                cor: '#1565c0',
              },
              {
                titulo: 'Atrasadas',
                valor: atrasadas,
                Icone: WarningAmberIcon,
                cor: atrasadas > 0 ? '#d32f2f' : '#2e7d32',
                subtitulo: atrasadas > 0 ? 'Requer atenção' : 'Sem atrasos',
              },
              {
                titulo: 'Em Produção',
                valor: emProducao,
                Icone: BuildIcon,
                cor: '#9c27b0',
              },
              {
                titulo: 'Concluídas',
                valor: concluidas,
                Icone: CheckCircleOutlineIcon,
                cor: '#2e7d32',
              },
            ].map((kpi) => (
              <Grid key={kpi.titulo} size={{ xs: 12, sm: 6, lg: 3 }}>
                <KpiCard {...kpi} />
              </Grid>
            ))}
      </Grid>

      {/* Gráfico + Ordens recentes */}
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 5 }}>
          {isLoading ? (
            <Skeleton variant="rounded" height={360} />
          ) : (
            <StatusPieChart ordens={ordens} />
          )}
        </Grid>
        <Grid size={{ xs: 12, md: 7 }}>
          <Card sx={{ height: '100%' }}>
            <CardContent sx={{ p: 3 }}>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Ordens Recentes
              </Typography>
              {isLoading ? (
                <Skeleton variant="rounded" height={240} />
              ) : (
                <OrdemTable ordens={recentes} onAtualizarStatus={setOrdemSelecionada} />
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <AtualizarStatusDialog
        ordem={ordemSelecionada}
        onFechar={() => setOrdemSelecionada(null)}
      />
    </Box>
  )
}
