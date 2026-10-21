import ReactECharts from 'echarts-for-react'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Typography from '@mui/material/Typography'
import type { StatusOrdem } from '../types'

const STATUS_CORES: Record<StatusOrdem, string> = {
  PLANEJADA:   '#94A3B8',
  LIBERADA:    '#2563EB',
  EM_PRODUCAO: '#D97706',
  CONCLUIDA:   '#059669',
  CANCELADA:   '#DC2626',
}

const STATUS_LABELS: Record<StatusOrdem, string> = {
  PLANEJADA:   'Planejada',
  LIBERADA:    'Liberada',
  EM_PRODUCAO: 'Em Produção',
  CONCLUIDA:   'Concluída',
  CANCELADA:   'Cancelada',
}

interface StatusPieChartProps {
  /** Quantidade de ordens por status, vinda do resumo do servidor. */
  contagem?: Partial<Record<StatusOrdem, number>>
}

export function StatusPieChart({ contagem = {} }: StatusPieChartProps) {
  const dados = (Object.entries(contagem) as [StatusOrdem, number][])
    .filter(([, value]) => value > 0)
    .map(([status, value]) => ({
      name: STATUS_LABELS[status],
      value,
      itemStyle: { color: STATUS_CORES[status] },
    }))

  const quantidadePorNome = Object.fromEntries(dados.map((d) => [d.name, d.value]))

  const option = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: {
      bottom: 0,
      left: 'center',
      icon: 'circle',
      itemGap: 16,
      formatter: (nome: string) => `${nome} (${quantidadePorNome[nome] ?? 0})`,
    },
    series: [
      {
        type: 'pie',
        center: ['50%', '40%'],
        radius: ['36%', '58%'],
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
        {dados.length === 0 ? (
          <Typography color="text.secondary" sx={{ mt: 4, textAlign: 'center' }}>
            Nenhuma ordem cadastrada
          </Typography>
        ) : (
          <ReactECharts option={option} style={{ height: 340 }} />
        )}
      </CardContent>
    </Card>
  )
}
