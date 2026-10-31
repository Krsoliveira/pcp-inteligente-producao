import ReactECharts from 'echarts-for-react'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Typography from '@mui/material/Typography'
import type { AtrasoDoCentro } from '../types'

interface AtrasosPorCentroChartProps {
  atrasos?: AtrasoDoCentro[]
  /** Chamado com o nome do centro ao clicar numa barra; ausente quando o perfil não pode ver as ordens. */
  onSelecionar?: (centro: string) => void
}

export function AtrasosPorCentroChart({ atrasos = [], onSelecionar }: AtrasosPorCentroChartProps) {
  const option = {
    grid: { left: 8, right: 48, top: 8, bottom: 8, containLabel: true },
    tooltip: {
      trigger: 'item',
      formatter: (p: { dataIndex: number }) => {
        const a = atrasos[p.dataIndex]
        return `${a.centroDeTrabalho}<br/>${a.ordensAtrasadas} ${a.ordensAtrasadas === 1 ? 'ordem atrasada' : 'ordens atrasadas'}<br/>a mais antiga há ${a.diasDeAtrasoDaMaisAntiga} dias`
      },
    },
    xAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#E2E8F0' } } },
    yAxis: { type: 'category', inverse: true, data: atrasos.map((a) => a.centroDeTrabalho), axisTick: { show: false } },
    series: [
      {
        type: 'bar',
        barMaxWidth: 26,
        cursor: onSelecionar ? 'pointer' : 'default',
        label: { show: true, position: 'right', fontWeight: 600 },
        data: atrasos.map((a) => ({ value: a.ordensAtrasadas, itemStyle: { color: '#DC2626', borderRadius: [0, 4, 4, 0] } })),
      },
    ],
  }

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent sx={{ p: 3, height: '100%' }}>
        <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>Atrasos por centro de trabalho</Typography>
        <Typography
          variant="caption"
          sx={{ color: 'text.secondary', display: 'block', mb: 1 }}>
          Ordens abertas depois do fim planejado{onSelecionar ? ' · clique numa barra para ver as ordens' : ''}
        </Typography>
        {atrasos.length === 0 ? (
          <Typography
            sx={{ color: 'text.secondary', mt: 4, textAlign: 'center' }}>
            Nenhum centro com ordens atrasadas
          </Typography>
        ) : (
          <ReactECharts
            option={option}
            style={{ height: Math.max(160, atrasos.length * 44 + 24) }}
            onEvents={onSelecionar ? { click: (e: { dataIndex: number }) => onSelecionar(atrasos[e.dataIndex].centroDeTrabalho) } : undefined}
          />
        )}
      </CardContent>
    </Card>
  )
}
