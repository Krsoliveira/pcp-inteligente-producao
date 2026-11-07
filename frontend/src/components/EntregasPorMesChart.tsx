import ReactECharts from 'echarts-for-react'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Typography from '@mui/material/Typography'
import type { ReactNode } from 'react'
import type { EntregasDoMes } from '../types'
import { rotuloDoMes } from '../utils/periodos'
import { DadosDoGrafico } from './DadosDoGrafico'
import { COR_DA_GRADE_DO_GRAFICO, TOM_VIVO } from './tonsDosGraficos'

/** As quatro situações de uma ordem no mês do fim planejado, na ordem em que as barras empilham. */
export const SERIES_DE_ENTREGA: { campo: keyof Omit<EntregasDoMes, 'mes'>; rotulo: string; cor: string }[] = [
  { campo: 'noPrazo', rotulo: 'Concluídas no prazo', cor: TOM_VIVO.sucesso },
  { campo: 'comAtraso', rotulo: 'Concluídas com atraso', cor: TOM_VIVO.atencao },
  { campo: 'abertasAtrasadas', rotulo: 'Abertas atrasadas', cor: TOM_VIVO.erro },
  { campo: 'abertasNoPrazo', rotulo: 'Abertas no prazo', cor: TOM_VIVO.neutro },
]

interface EntregasPorMesChartProps {
  entregas?: EntregasDoMes[]
  /** Controle do período (3, 6 ou 12 meses), exibido no cabeçalho do cartão. */
  acaoDoCabecalho?: ReactNode
  /** Chamado com o mês (AAAA-MM) ao clicar numa barra; ausente quando o perfil não pode ver as ordens. */
  onSelecionarMes?: (mes: string) => void
}

export function EntregasPorMesChart({ entregas = [], acaoDoCabecalho, onSelecionarMes }: EntregasPorMesChartProps) {
  const total = entregas.reduce((soma, m) => soma + m.noPrazo + m.comAtraso + m.abertasAtrasadas + m.abertasNoPrazo, 0)

  const resumo = entregas
    .map((m) => `${rotuloDoMes(m.mes)}: ${SERIES_DE_ENTREGA.map((s) => `${s.rotulo} ${m[s.campo]}`).join(', ')}`)
    .join('; ')

  const option = {
    grid: { left: 8, right: 8, top: 36, bottom: 8, containLabel: true },
    legend: { top: 0, icon: 'circle', itemGap: 16 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'category', data: entregas.map((m) => rotuloDoMes(m.mes)), axisTick: { show: false } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: COR_DA_GRADE_DO_GRAFICO } } },
    series: SERIES_DE_ENTREGA.map((serie) => ({
      name: serie.rotulo,
      type: 'bar',
      stack: 'ordens',
      cursor: onSelecionarMes ? 'pointer' : 'default',
      itemStyle: { color: serie.cor },
      emphasis: { focus: 'series' },
      data: entregas.map((m) => m[serie.campo]),
    })),
  }

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent sx={{ p: 3, height: '100%' }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 2, flexWrap: 'wrap' }}>
          <Box>
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>Entregas por mês</Typography>
            <Typography
              variant="caption"
              sx={{ color: 'text.secondary', display: 'block', mb: 1 }}>
              Ordens pelo mês do fim planejado, sem as canceladas{onSelecionarMes ? ' · clique num mês para ver as ordens' : ''}
            </Typography>
          </Box>
          {acaoDoCabecalho}
        </Box>
        {total === 0 ? (
          <Typography
            sx={{ color: 'text.secondary', mt: 4, textAlign: 'center' }}>
            Nenhuma ordem com fim planejado neste período
          </Typography>
        ) : (
          <>
            <Box role="img" aria-label={`Entregas por mês: ${resumo}.`}>
              <ReactECharts
                option={option}
                style={{ height: 280 }}
                onEvents={onSelecionarMes && entregas.length > 0
                  ? { click: (e: { dataIndex: number }) => onSelecionarMes(entregas[e.dataIndex].mes) }
                  : undefined}
              />
            </Box>
            <DadosDoGrafico
              titulo="Entregas por mês"
              rotuloDaColuna="Mês"
              colunas={SERIES_DE_ENTREGA.map((s) => s.rotulo)}
              linhas={entregas.map((m) => ({
                rotulo: rotuloDoMes(m.mes),
                valores: SERIES_DE_ENTREGA.map((s) => m[s.campo]),
                onSelecionar: onSelecionarMes && (() => onSelecionarMes(m.mes)),
              }))}
            />
          </>
        )}
      </CardContent>
    </Card>
  )
}
