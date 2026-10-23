import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { consultarResumoProducao } from '../api/dashboard'
import { pesquisarOrdens } from '../api/ordens'
import { ordem, pagina } from '../test/fabricas'
import { renderizarTela } from '../test/renderizar'
import type { Permissao, ResumoProducao } from '../types'
import { DashboardPage } from './DashboardPage'

vi.mock('../api/dashboard', () => ({ consultarResumoProducao: vi.fn() }))
vi.mock('../api/ordens', async (importarOriginal) => ({
  ...(await importarOriginal<typeof import('../api/ordens')>()),
  pesquisarOrdens: vi.fn(),
}))
// O ECharts desenha em canvas, que o jsdom não tem: um botão por barra faz o papel do clique no gráfico.
vi.mock('echarts-for-react', () => ({
  default: ({ option, onEvents }: { option: { yAxis: { data: string[] } }; onEvents?: { click: (e: { dataIndex: number }) => void } }) => (
    <div data-testid="grafico">
      {option.yAxis.data.map((rotulo, i) => (
        <button key={rotulo} onClick={() => onEvents?.click({ dataIndex: i })}>{`barra ${rotulo}`}</button>
      ))}
    </div>
  ),
}))

const RESUMO: ResumoProducao = {
  totalOrdens: 845,
  ordensPorStatus: { PLANEJADA: 52, LIBERADA: 3, EM_PRODUCAO: 18, CONCLUIDA: 746, CANCELADA: 26 },
  ordensAtrasadas: 34,
  lotesDisponiveis: 180,
}

function montar(permissoes?: Permissao[]) {
  return renderizarTela(<DashboardPage />, { permissoes, rotas: ['/dashboard'], padrao: '/dashboard' })
}

/** Cartão de indicador pelo título (o link tem o título no nome acessível). */
const cartao = (titulo: string) => screen.getByRole('link', { name: new RegExp(`^${titulo}:`) })

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.mocked(consultarResumoProducao).mockReset().mockResolvedValue(RESUMO)
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

  it('o gráfico mostra só a fila aberta e clicar numa barra filtra as ordens por aquele status', async () => {
    montar()
    const grafico = await screen.findByTestId('grafico')

    expect(within(grafico).getAllByRole('button').map((b) => b.textContent))
      .toEqual(['barra Planejada', 'barra Liberada', 'barra Em produção']) // sem concluídas nem canceladas
    expect(screen.getByText(/^73 ordens abertas/)).toBeInTheDocument()
    expect(screen.getByText(/772 ordens encerradas/)).toBeInTheDocument()

    await userEvent.click(within(grafico).getByRole('button', { name: 'barra Liberada' }))

    expect(screen.getByTestId('rota')).toHaveTextContent('/ordens/resultados?status=LIBERADA')
  })

  it('sem ORDEM_VISUALIZAR os indicadores de ordens são só informativos (sem link nem clique no gráfico)', async () => {
    montar(['DASHBOARD_VISUALIZAR', 'ESTOQUE_VISUALIZAR'])
    await screen.findByText('Atrasadas')

    expect(screen.queryByRole('link', { name: /^Atrasadas/ })).not.toBeInTheDocument()
    expect(screen.getByText('34')).toBeInTheDocument()
    expect(cartao('Lotes disponíveis')).toHaveAttribute('href', '/estoque') // esse o perfil pode abrir
    await userEvent.click(within(screen.getByTestId('grafico')).getByRole('button', { name: 'barra Liberada' }))
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

  it('sem ordens abertas o gráfico explica em vez de desenhar barras vazias', async () => {
    vi.mocked(consultarResumoProducao).mockResolvedValue({
      ...RESUMO, ordensPorStatus: { PLANEJADA: 0, LIBERADA: 0, EM_PRODUCAO: 0, CONCLUIDA: 10, CANCELADA: 0 },
    })
    montar()

    expect(await screen.findByText('Nenhuma ordem em andamento')).toBeInTheDocument()
    expect(screen.queryByTestId('grafico')).not.toBeInTheDocument()
  })
})
