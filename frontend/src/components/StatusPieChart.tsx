import ReactECharts from 'echarts-for-react'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Typography from '@mui/material/Typography'
import type { OrdemProducao, StatusOrdem } from '../types'

const STATUS_CORES: Record<StatusOrdem, string> = {
  PLANEJADA: '#1565c0',
  LIBERADA: '#ed6c02',
  EM_PRODUCAO: '#9c27b0',
  CONCLUIDA: '#2e7d32',
  CANCELADA: '#d32f2f',
}

const STATUS_LABELS: Record<StatusOrdem, string> = {
  PLANEJADA: 'Planejada',
  LIBERADA: 'Liberada',
  EM_PRODUCAO: 'Em Produção',
  CONCLUIDA: 'Concluída',
  CANCELADA: 'Cancelada',
}

interface StatusPieChartProps {
  ordens: OrdemProducao[]
}

export function StatusPieChart({ ordens }: StatusPieChartProps) {
  const contagem = ordens.reduce<Partial<Record<StatusOrdem, number>>>(
    (acc, o) => ({ ...acc, [o.status]: (acc[o.status] ?? 0) + 1 }),
    {},
  )

  const dados = (Object.entries(contagem) as [StatusOrdem, number][]).map(
    ([status, value]) => ({
      name: STATUS_LABELS[status],
      value,
      itemStyle: { color: STATUS_CORES[status] },
    }),
  )

  const option = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: '2%', left: 'center' },
    series: [
      {
        type: 'pie',
        radius: ['42%', '70%'],
        avoidLabelOverlap: false,
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
        data: dados,
      },
    ],
  }

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent sx={{ p: 3, height: '100%' }}>
        <Typography variant="subtitle1" fontWeight={600} gutterBottom>
          Ordens por Status
        </Typography>
        {ordens.length === 0 ? (
          <Typography color="text.secondary" sx={{ mt: 4, textAlign: 'center' }}>
            Nenhuma ordem cadastrada
          </Typography>
        ) : (
          <ReactECharts option={option} style={{ height: 280 }} />
        )}
      </CardContent>
    </Card>
  )
}
