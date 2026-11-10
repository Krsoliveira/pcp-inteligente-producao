import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { consultarResumoEstoque, pesquisarEstoque } from '../api/estoque'
import { pagina, posicaoEstoque } from '../test/fabricas'
import { renderizarTela } from '../test/renderizar'
import { EstoquePage } from './EstoquePage'

vi.mock('../api/estoque', () => ({ pesquisarEstoque: vi.fn(), consultarResumoEstoque: vi.fn() }))

const ROTA = '/estoque'

function montar() {
  return renderizarTela(<EstoquePage />, { rotas: [ROTA], padrao: ROTA })
}

describe('EstoquePage', () => {
  beforeEach(() => {
    vi.mocked(pesquisarEstoque).mockReset()
    vi.mocked(pesquisarEstoque).mockResolvedValue(pagina([posicaoEstoque({ descricao: 'Aço inox' })], { total: 1 }))
    vi.mocked(consultarResumoEstoque).mockResolvedValue({ totalMateriais: 1, materiaisVencendo: 0, materiasPrimasSemSaldo: 0 })
  })

  it('ligar "Só com saldo" já abre os resultados filtrados, sem apertar Pesquisar', async () => {
    montar()
    await screen.findByText('Aço inox')

    await userEvent.click(screen.getByRole('switch', { name: 'Só com saldo' }))

    await waitFor(() => expect(screen.getByTestId('rota')).toHaveTextContent('/estoque/resultados?somenteComSaldo=true'))
  })

  it('o texto já digitado vai junto ao ligar "Só com saldo"', async () => {
    montar()
    await screen.findByText('Aço inox')

    await userEvent.type(screen.getByLabelText('Código ou descrição'), 'aço')
    await userEvent.click(screen.getByRole('switch', { name: 'Só com saldo' }))

    await waitFor(() => expect(screen.getByTestId('rota')).toHaveTextContent('/estoque/resultados?busca=a%C3%A7o&somenteComSaldo=true'))
  })
})
