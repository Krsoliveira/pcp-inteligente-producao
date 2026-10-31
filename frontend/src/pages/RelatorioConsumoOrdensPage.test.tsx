import { createTheme } from '@mui/material/styles'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { consultarConsumoDeOrdens } from '../api/relatorios'
import { erroHttp } from '../test/erros'
import { renderizarTela } from '../test/renderizar'
import type { LinhaDeConsumoDeOrdem, Permissao, RelatorioConsumoDeOrdens } from '../types'
import { RelatorioConsumoOrdensPage, periodoLongoDemais } from './RelatorioConsumoOrdensPage'

vi.mock('../api/relatorios', () => ({ consultarConsumoDeOrdens: vi.fn() }))

const consultarMock = vi.mocked(consultarConsumoDeOrdens)
const ROTA = '/relatorios/consumo-de-ordens'

const linha = (extras: Partial<LinhaDeConsumoDeOrdem>): LinhaDeConsumoDeOrdem => ({
  ordemId: 'o1', ordemCodigo: 'OP-2026-0001', fimPlanejado: '2026-09-10',
  produtoCodigo: '103000002', produtoDescricao: 'Redutor de velocidade R50',
  componenteCodigo: '105000004', componenteDescricao: 'Carcaça usinada', unidadeDeMedida: 'un',
  quantidadePlanejada: 20, quantidadeConsumida: 20, desvio: 0, ...extras,
})

// A ordem 1 tem 3 componentes (a ordem se repete); a ordem 2 tem 1.
const RELATORIO: RelatorioConsumoDeOrdens = {
  de: '2026-09-01', ate: '2026-09-30', totalDeOrdens: 2,
  linhas: [
    linha({}),
    linha({ componenteCodigo: '110000002', componenteDescricao: 'Chapa de aço', unidadeDeMedida: 'kg', quantidadePlanejada: 25, quantidadeConsumida: 26, desvio: 1 }),
    linha({ componenteCodigo: '110000007', componenteDescricao: 'Parafuso M8', quantidadePlanejada: 40, quantidadeConsumida: 38, desvio: -2 }),
    linha({ ordemId: 'o2', ordemCodigo: 'OP-2026-0002', fimPlanejado: '2026-09-20' }),
  ],
}

function montar(url = ROTA, permissoes?: Permissao[]) {
  return renderizarTela(<RelatorioConsumoOrdensPage />, { rotas: [url], padrao: ROTA, permissoes })
}

describe('periodoLongoDemais', () => {
  it('366 dias inclusive é o limite', () => {
    expect(periodoLongoDemais('2026-01-01', '2027-01-01')).toBe(false) // 366 dias contando as duas pontas
    expect(periodoLongoDemais('2026-01-01', '2027-01-02')).toBe(true)
    expect(periodoLongoDemais('', '2027-01-02')).toBe(false)
  })
})

describe('RelatorioConsumoOrdensPage', () => {
  beforeEach(() => {
    consultarMock.mockReset()
    consultarMock.mockResolvedValue(RELATORIO)
  })

  it('sem período na URL pede para escolher o período e não consulta o servidor', () => {
    montar()

    expect(screen.getByText('Escolha o período')).toBeInTheDocument()
    expect(consultarMock).not.toHaveBeenCalled()
  })

  it('consulta o servidor com o período da URL e mostra uma linha por ordem e componente, repetindo a ordem', async () => {
    montar(`${ROTA}?de=2026-09-01&ate=2026-09-30`)

    await screen.findByText('Chapa de aço')

    expect(consultarMock).toHaveBeenCalledWith('2026-09-01', '2026-09-30')
    expect(screen.getAllByText('OP-2026-0001')).toHaveLength(3) // ordem 1: três componentes, três linhas
    expect(screen.getAllByText('OP-2026-0002')).toHaveLength(1)
    expect(screen.getByText('4 linhas de 2 ordens concluídas')).toBeInTheDocument()
  })

  it('mostra planejado, consumido e desvio de cada componente, com o código formatado', async () => {
    montar(`${ROTA}?de=2026-09-01&ate=2026-09-30`)
    const linhaDaChapa = within((await screen.findByText('Chapa de aço')).closest('tr') as HTMLElement)

    expect(linhaDaChapa.getByText('110.000.002')).toBeInTheDocument()
    expect(linhaDaChapa.getByText('25 kg')).toBeInTheDocument() // planejado, com a unidade
    expect(linhaDaChapa.getByText('26 kg')).toBeInTheDocument() // consumido (a soma dos lotes)
    expect(linhaDaChapa.getByText('1 kg')).toBeInTheDocument() // desvio
    expect(within(screen.getByText('Parafuso M8').closest('tr') as HTMLElement).getByText('-2 un')).toBeInTheDocument()
  })

  it('o desvio é colorido: acima do planejado em vermelho, abaixo em laranja, sem desvio em cinza', async () => {
    // Regressão da migração para o MUI 9: o `color` calculado na Typography deixou de pintar o texto.
    const { palette } = createTheme()
    montar(`${ROTA}?de=2026-09-01&ate=2026-09-30`)
    const desvioDe = async (componente: string, texto: string) =>
      within((await screen.findAllByText(componente))[0].closest('tr') as HTMLElement).getByText(texto)

    expect(await desvioDe('Chapa de aço', '1 kg')).toHaveStyle({ color: palette.error.main })
    expect(await desvioDe('Parafuso M8', '-2 un')).toHaveStyle({ color: palette.warning.main })
    expect(await desvioDe('Carcaça usinada', '0 un')).toHaveStyle({ color: palette.text.secondary })
  })

  it('o código da ordem abre a ordem para quem pode vê-la', async () => {
    montar(`${ROTA}?de=2026-09-01&ate=2026-09-30`)

    expect((await screen.findAllByRole('link', { name: 'OP-2026-0002' }))[0]).toHaveAttribute('href', '/ordens/o2')
  })

  it('sem ORDEM_VISUALIZAR o código da ordem não vira link', async () => {
    montar(`${ROTA}?de=2026-09-01&ate=2026-09-30`, ['RELATORIO_CONSUMO_VISUALIZAR'])

    await screen.findByText('Chapa de aço')

    expect(screen.queryByRole('link', { name: 'OP-2026-0002' })).not.toBeInTheDocument()
  })

  it('Pesquisar coloca o período na URL e consulta o servidor', async () => {
    montar()

    await userEvent.type(screen.getByLabelText('Fim planejado de'), '2026-09-01')
    await userEvent.type(screen.getByLabelText('até'), '2026-09-30')
    await userEvent.click(screen.getByRole('button', { name: 'Pesquisar' }))

    await screen.findByText('Chapa de aço')
    expect(screen.getByTestId('rota')).toHaveTextContent('de=2026-09-01&ate=2026-09-30')
    expect(consultarMock).toHaveBeenCalledWith('2026-09-01', '2026-09-30')
  })

  it('período invertido avisa e não consulta', async () => {
    montar()

    await userEvent.type(screen.getByLabelText('Fim planejado de'), '2026-09-30')
    await userEvent.type(screen.getByLabelText('até'), '2026-09-01')
    await userEvent.click(screen.getByRole('button', { name: 'Pesquisar' }))

    expect((await screen.findAllByText(/não pode ser anterior ao início/)).length).toBeGreaterThan(0)
    expect(consultarMock).not.toHaveBeenCalled()
  })

  it('período maior que 366 dias avisa e não consulta', async () => {
    montar()

    await userEvent.type(screen.getByLabelText('Fim planejado de'), '2025-01-01')
    await userEvent.type(screen.getByLabelText('até'), '2026-09-01')
    await userEvent.click(screen.getByRole('button', { name: 'Pesquisar' }))

    expect(await screen.findByText('O período pode ter no máximo 366 dias.')).toBeInTheDocument()
    expect(consultarMock).not.toHaveBeenCalled()
  })

  it('data inválida na URL é ignorada', () => {
    montar(`${ROTA}?de=ontem&ate=hoje`)

    expect(screen.getByText('Escolha o período')).toBeInTheDocument()
    expect(consultarMock).not.toHaveBeenCalled()
  })

  it('período sem ordens concluídas explica em vez de mostrar uma tabela vazia', async () => {
    consultarMock.mockResolvedValue({ de: '2026-09-01', ate: '2026-09-30', totalDeOrdens: 0, linhas: [] })
    montar(`${ROTA}?de=2026-09-01&ate=2026-09-30`)

    expect(await screen.findByText('Nenhuma ordem concluída neste período')).toBeInTheDocument()
  })

  it('mostra o motivo quando o servidor recusa (ex.: relatório grande demais)', async () => {
    consultarMock.mockRejectedValue(erroHttp(422, { detail: 'O relatório passa de 5000 linhas; reduza o período.' }))
    montar(`${ROTA}?de=2026-09-01&ate=2026-09-30`)

    expect(await screen.findByText('O relatório passa de 5000 linhas; reduza o período.')).toBeInTheDocument()
  })

  it('Limpar tira o período da URL e volta ao pedido de escolha', async () => {
    montar(`${ROTA}?de=2026-09-01&ate=2026-09-30`)
    await screen.findByText('Chapa de aço')

    await userEvent.click(screen.getByRole('button', { name: 'Limpar' }))

    await waitFor(() => expect(screen.getByText('Escolha o período')).toBeInTheDocument())
    expect(screen.getByTestId('rota')).not.toHaveTextContent('de=')
  })
})
