import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { consultarAtrasosPorCentro, consultarEntregasPorMes, consultarResumoProducao } from '../api/dashboard'
import { pesquisarOrdens } from '../api/ordens'
import { ordem, pagina } from '../test/fabricas'
import { renderizarTela } from '../test/renderizar'
import type { AtrasoDoCentro, EntregasDoMes, Permissao, ResumoProducao } from '../types'
import { DashboardPage, aderenciaAoPrazo } from './DashboardPage'

vi.mock('../api/dashboard', () => ({
  consultarResumoProducao: vi.fn(),
  consultarEntregasPorMes: vi.fn(),
  consultarAtrasosPorCentro: vi.fn(),
}))
vi.mock('../api/ordens', async (importarOriginal) => ({
  ...(await importarOriginal<typeof import('../api/ordens')>()),
  pesquisarOrdens: vi.fn(),
}))

type OpcaoDoGrafico = { legend?: unknown; xAxis: { data?: string[] }; yAxis: { data?: string[] } }
// O ECharts desenha em canvas, que o jsdom não tem: um botão por barra (ou por mês) faz o papel do clique no gráfico.
vi.mock('echarts-for-react', () => ({
  default: ({ option, onEvents }: { option: OpcaoDoGrafico; onEvents?: { click: (e: { dataIndex: number }) => void } }) => {
    const rotulos = option.yAxis.data ?? option.xAxis.data ?? []
    const id = option.legend ? 'entregas' : rotulos.includes('Planejada') ? 'fila' : 'atrasos'
    return (
      <div data-testid={`grafico-${id}`}>
        {rotulos.map((rotulo, i) => (
          <button key={rotulo} onClick={() => onEvents?.click({ dataIndex: i })}>{`barra ${rotulo}`}</button>
        ))}
      </div>
    )
  },
}))

const RESUMO: ResumoProducao = {
  totalOrdens: 845,
  ordensPorStatus: { PLANEJADA: 52, LIBERADA: 3, EM_PRODUCAO: 18, CONCLUIDA: 746, CANCELADA: 26 },
  ordensAtrasadas: 34,
  lotesDisponiveis: 180,
}

const mes = (anoMes: string, extras: Partial<EntregasDoMes> = {}): EntregasDoMes =>
  ({ mes: anoMes, noPrazo: 0, comAtraso: 0, abertasAtrasadas: 0, abertasNoPrazo: 0, ...extras })

const ENTREGAS: EntregasDoMes[] = [
  mes('2026-08', { noPrazo: 6, comAtraso: 2 }),
  mes('2026-09', { noPrazo: 3, comAtraso: 1, abertasAtrasadas: 4 }),
  mes('2026-10', { abertasNoPrazo: 5 }),
]

const ATRASOS: AtrasoDoCentro[] = [
  { centroDeTrabalho: 'Usinagem CNC', ordensAtrasadas: 12, diasDeAtrasoDaMaisAntiga: 40 },
  { centroDeTrabalho: 'Soldagem MIG/TIG', ordensAtrasadas: 3, diasDeAtrasoDaMaisAntiga: 5 },
]

function montar(permissoes?: Permissao[]) {
  return renderizarTela(<DashboardPage />, { permissoes, rotas: ['/dashboard'], padrao: '/dashboard' })
}

/** Cartão de indicador pelo título (o link tem o título no nome acessível). */
const cartao = (titulo: string) => screen.getByRole('link', { name: new RegExp(`^${titulo}:`) })

describe('aderenciaAoPrazo', () => {
  it('é o percentual de concluídas no prazo sobre as concluídas do período', () => {
    expect(aderenciaAoPrazo(ENTREGAS)).toEqual({ noPrazo: 9, concluidas: 12, percentual: 75 })
  })

  it('arredonda para o inteiro mais próximo', () => {
    expect(aderenciaAoPrazo([mes('2026-10', { noPrazo: 2, comAtraso: 1 })])?.percentual).toBe(67)
  })

  it('sem ordens concluídas não há aderência (em vez de 0% ou 100%)', () => {
    expect(aderenciaAoPrazo([])).toBeNull()
    expect(aderenciaAoPrazo([mes('2026-10', { abertasAtrasadas: 3 })])).toBeNull()
  })
})

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.mocked(consultarResumoProducao).mockReset().mockResolvedValue(RESUMO)
    vi.mocked(consultarEntregasPorMes).mockReset().mockResolvedValue(ENTREGAS)
    vi.mocked(consultarAtrasosPorCentro).mockReset().mockResolvedValue(ATRASOS)
    vi.mocked(pesquisarOrdens).mockReset().mockResolvedValue(pagina([ordem({ id: '1', codigo: 'OP-2026-0001' })]))
  })

  it('mostra os totais do servidor', async () => {
    montar()

    expect(await screen.findByRole('link', { name: /^Atrasadas: 34/ })).toBeInTheDocument()
    expect(cartao('Total de ordens')).toHaveTextContent('845')
    expect(cartao('Em produção')).toHaveTextContent('18')
    expect(cartao('Concluídas')).toHaveTextContent('746')
    expect(cartao('Lotes disponíveis')).toHaveTextContent('180')
  })

  it('cada indicador leva à listagem já filtrada', async () => {
    montar()
    await screen.findByRole('link', { name: /^Atrasadas/ })

    expect(cartao('Total de ordens')).toHaveAttribute('href', '/ordens/resultados')
    expect(cartao('Atrasadas')).toHaveAttribute('href', '/ordens/resultados?atrasadas=true')
    expect(cartao('Em produção')).toHaveAttribute('href', '/ordens/resultados?status=EM_PRODUCAO')
    expect(cartao('Concluídas')).toHaveAttribute('href', '/ordens/resultados?status=CONCLUIDA')
    expect(cartao('Lotes disponíveis')).toHaveAttribute('href', '/estoque')
  })

  it('clicar em Atrasadas abre a tela de ordens com o filtro na URL', async () => {
    montar()

    await userEvent.click(await screen.findByRole('link', { name: /^Atrasadas/ }))

    expect(screen.getByTestId('rota')).toHaveTextContent('/ordens/resultados?atrasadas=true')
  })

  it('o gráfico da fila mostra só a fila aberta e clicar numa barra filtra as ordens por aquele status', async () => {
    montar()
    const grafico = await screen.findByTestId('grafico-fila')

    expect(within(grafico).getAllByRole('button').map((b) => b.textContent))
      .toEqual(['barra Planejada', 'barra Liberada', 'barra Em produção']) // sem concluídas nem canceladas
    expect(screen.getByText(/^73 ordens abertas/)).toBeInTheDocument()
    expect(screen.getByText(/772 ordens encerradas/)).toBeInTheDocument()

    await userEvent.click(within(grafico).getByRole('button', { name: 'barra Liberada' }))

    expect(screen.getByTestId('rota')).toHaveTextContent('/ordens/resultados?status=LIBERADA')
  })

  describe('entregas por mês e aderência ao prazo', () => {
    it('pede 12 meses por padrão e mostra um mês por barra', async () => {
      montar()
      const grafico = await screen.findByTestId('grafico-entregas')

      expect(consultarEntregasPorMes).toHaveBeenCalledWith(12)
      expect(within(grafico).getAllByRole('button').map((b) => b.textContent))
        .toEqual(['barra ago/26', 'barra set/26', 'barra out/26'])
    })

    it('a aderência ao prazo usa as concluídas do período e diz de quantas', async () => {
      montar()

      expect(await screen.findByText('75%')).toBeInTheDocument()
      expect(screen.getByText('9 de 12 concluídas no prazo · 12 meses')).toBeInTheDocument()
    })

    it('sem concluídas no período mostra "—" e explica, em vez de 0%', async () => {
      vi.mocked(consultarEntregasPorMes).mockResolvedValue([mes('2026-10', { abertasNoPrazo: 2 })])
      montar()

      expect(await screen.findByText('Sem ordens concluídas nos últimos 12 meses')).toBeInTheDocument()
      expect(screen.queryByText(/%$/)).not.toBeInTheDocument()
    })

    it('trocar o período busca de novo com a nova quantidade de meses', async () => {
      montar()
      await screen.findByTestId('grafico-entregas')

      await userEvent.click(screen.getByRole('button', { name: 'Últimos 6 meses' }))

      await waitFor(() => expect(consultarEntregasPorMes).toHaveBeenLastCalledWith(6))
      expect(await screen.findByText(/concluídas no prazo · 6 meses/)).toBeInTheDocument()
    })

    it('clicar num mês abre as ordens com fim planejado naquele mês', async () => {
      montar()

      await userEvent.click(await screen.findByRole('button', { name: 'barra set/26' }))

      expect(screen.getByTestId('rota')).toHaveTextContent('/ordens/resultados?fimDe=2026-09-01&fimAte=2026-09-30')
    })

    it('sem nenhuma ordem no período o gráfico explica em vez de desenhar barras vazias', async () => {
      vi.mocked(consultarEntregasPorMes).mockResolvedValue([mes('2026-10')])
      montar()

      expect(await screen.findByText('Nenhuma ordem com fim planejado neste período')).toBeInTheDocument()
      expect(screen.queryByTestId('grafico-entregas')).not.toBeInTheDocument()
    })
  })

  describe('atrasos por centro de trabalho', () => {
    it('mostra os centros com atraso e clicar num deles abre as ordens atrasadas daquele centro', async () => {
      montar()
      const grafico = await screen.findByTestId('grafico-atrasos')

      expect(within(grafico).getAllByRole('button').map((b) => b.textContent))
        .toEqual(['barra Usinagem CNC', 'barra Soldagem MIG/TIG'])

      await userEvent.click(within(grafico).getByRole('button', { name: 'barra Usinagem CNC' }))

      expect(screen.getByTestId('rota')).toHaveTextContent('/ordens/resultados?centro=Usinagem+CNC&atrasadas=true')
    })

    it('sem atrasos diz isso em vez de mostrar um gráfico vazio', async () => {
      vi.mocked(consultarAtrasosPorCentro).mockResolvedValue([])
      montar()

      expect(await screen.findByText('Nenhum centro com ordens atrasadas')).toBeInTheDocument()
      expect(screen.queryByTestId('grafico-atrasos')).not.toBeInTheDocument()
    })
  })

  it('sem ORDEM_VISUALIZAR os indicadores e os gráficos são só informativos (sem link nem clique)', async () => {
    montar(['DASHBOARD_VISUALIZAR', 'ESTOQUE_VISUALIZAR'])
    await screen.findByText('Atrasadas')

    expect(screen.queryByRole('link', { name: /^Atrasadas/ })).not.toBeInTheDocument()
    expect(screen.getByText('34')).toBeInTheDocument()
    expect(cartao('Lotes disponíveis')).toHaveAttribute('href', '/estoque') // esse o perfil pode abrir
    await userEvent.click(within(await screen.findByTestId('grafico-fila')).getByRole('button', { name: 'barra Liberada' }))
    await userEvent.click(screen.getByRole('button', { name: 'barra set/26' }))
    await userEvent.click(screen.getByRole('button', { name: 'barra Usinagem CNC' }))
    expect(screen.getByTestId('rota')).toHaveTextContent('/dashboard')
  })

  it('sem ESTOQUE_VISUALIZAR os lotes disponíveis não viram link', async () => {
    montar(['DASHBOARD_VISUALIZAR', 'ORDEM_VISUALIZAR'])
    await screen.findByText('Lotes disponíveis')

    expect(screen.queryByRole('link', { name: /^Lotes disponíveis/ })).not.toBeInTheDocument()
  })

  it('sem atrasos diz isso e não pinta o número de alerta', async () => {
    vi.mocked(consultarResumoProducao).mockResolvedValue({ ...RESUMO, ordensAtrasadas: 0 })
    montar()

    expect(await screen.findByText('Nenhuma ordem atrasada')).toBeInTheDocument()
  })

  it('sem ordens abertas o gráfico da fila explica em vez de desenhar barras vazias', async () => {
    vi.mocked(consultarResumoProducao).mockResolvedValue({
      ...RESUMO, ordensPorStatus: { PLANEJADA: 0, LIBERADA: 0, EM_PRODUCAO: 0, CONCLUIDA: 10, CANCELADA: 0 },
    })
    montar()

    expect(await screen.findByText('Nenhuma ordem em andamento')).toBeInTheDocument()
    expect(screen.queryByTestId('grafico-fila')).not.toBeInTheDocument()
  })
})
