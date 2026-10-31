import ReactECharts from 'echarts-for-react'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Typography from '@mui/material/Typography'
import type { StatusOrdem } from '../types'

/** As ordens que ainda estão no chão de fábrica, na ordem em que andam. Concluídas e canceladas ficam de fora. */
export const ETAPAS_DA_FILA: { status: StatusOrdem; rotulo: string; cor: string }[] = [
  { status: 'PLANEJADA', rotulo: 'Planejada', cor: '#94A3B8' },
  { status: 'LIBERADA', rotulo: 'Liberada', cor: '#4F46E5' },
  { status: 'EM_PRODUCAO', rotulo: 'Em produção', cor: '#D97706' },
]

interface FilaDeProducaoChartProps {
  /** Quantidade de ordens por status, vinda do resumo do servidor. */
  contagem?: Partial<Record<StatusOrdem, number>>
  /** Chamado ao clicar numa barra; ausente quando o perfil não pode ver as ordens. */
  onSelecionar?: (status: StatusOrdem) => void
}

export function FilaDeProducaoChart({ contagem = {}, onSelecionar }: FilaDeProducaoChartProps) {
  const quantidades = ETAPAS_DA_FILA.map((etapa) => contagem[etapa.status] ?? 0)
  const totalAberto = quantidades.reduce((soma, q) => soma + q, 0)
  const encerradas = (contagem.CONCLUIDA ?? 0) + (contagem.CANCELADA ?? 0)

  const option = {
    grid: { left: 8, right: 48, top: 8, bottom: 8, containLabel: true },
    tooltip: { trigger: 'item', formatter: '{b}: {c} ordens' },
    xAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#E2E8F0' } } },
    yAxis: { type: 'category', inverse: true, data: ETAPAS_DA_FILA.map((e) => e.rotulo), axisTick: { show: false } },
    series: [
      {
        type: 'bar',
        barWidth: 28,
        cursor: onSelecionar ? 'pointer' : 'default',
        label: { show: true, position: 'right', fontWeight: 600 },
        data: ETAPAS_DA_FILA.map((etapa, i) => ({ value: quantidades[i], itemStyle: { color: etapa.cor, borderRadius: [0, 4, 4, 0] } })),
      },
    ],
  }

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent sx={{ p: 3, height: '100%' }}>
        <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
          Fila de produção
        </Typography>
        <Typography
          variant="caption"
          sx={{ color: 'text.secondary', display: 'block', mb: 1 }}>
          {totalAberto} {totalAberto === 1 ? 'ordem aberta' : 'ordens abertas'}
          {onSelecionar ? ' · clique numa barra para ver as ordens' : ''}
        </Typography>
        {totalAberto === 0 ? (
          <Typography
            sx={{ color: 'text.secondary', mt: 4, textAlign: 'center' }}>
            Nenhuma ordem em andamento
          </Typography>
        ) : (
          <ReactECharts
            option={option}
            style={{ height: 220 }}
            onEvents={onSelecionar ? { click: (e: { dataIndex: number }) => onSelecionar(ETAPAS_DA_FILA[e.dataIndex].status) } : undefined}
          />
        )}
        <Typography
          variant="caption"
          sx={{ color: 'text.secondary', display: 'block', mt: 1 }}>
          {encerradas} {encerradas === 1 ? 'ordem encerrada' : 'ordens encerradas'} (concluídas ou canceladas) não entram na fila.
        </Typography>
      </CardContent>
    </Card>
  )
}
