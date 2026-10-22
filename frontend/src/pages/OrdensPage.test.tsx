import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { pesquisarOrdens } from '../api/ordens'
import { erroDeRede } from '../test/erros'
import { ordem, pagina } from '../test/fabricas'
import { renderizarTela } from '../test/renderizar'
import { OrdensPage } from './OrdensPage'

vi.mock('../api/ordens', async (importarOriginal) => ({
  ...(await importarOriginal<typeof import('../api/ordens')>()),
  pesquisarOrdens: vi.fn(),
}))

const pesquisarMock = vi.mocked(pesquisarOrdens)

function montar() {
  return renderizarTela(<OrdensPage />, { rotas: ['/ordens'], padrao: '/ordens' })
}

describe('OrdensPage (consulta)', () => {
  beforeEach(() => {
    pesquisarMock.mockReset()
    pesquisarMock.mockResolvedValue(pagina([
      ordem({ id: '1', codigo: 'OP-2026-0002' }),
      ordem({ id: '2', codigo: 'OP-2026-0001' }),
    ]))
  })

  it('pede ao servidor apenas as 10 ordens mais recentes', async () => {
    montar()

    await screen.findByText('OP-2026-0002')

    expect(pesquisarMock).toHaveBeenCalledWith({ pagina: 0, tamanho: 10 })
    expect(screen.getByText('10 ordens mais recentes')).toBeInTheDocument()
  })

  it('leva à tela de resultados com os filtros escolhidos', async () => {
    montar()
    await screen.findByText('OP-2026-0002')

    await userEvent.type(screen.getByLabelText('Código da ordem'), '  OP-2026  ')
    await userEvent.click(screen.getByRole('button', { name: 'Pesquisar' }))

    await waitFor(() => expect(screen.getByTestId('rota')).toHaveTextContent('/ordens/resultados?busca=OP-2026'))
  })

  it('"Ver todos" abre os resultados sem filtro', async () => {
    montar()
    await screen.findByText('OP-2026-0002')

    await userEvent.click(screen.getByRole('button', { name: /ver todos/i }))

    expect(screen.getByTestId('rota')).toHaveTextContent(/^\/ordens\/resultados$/)
  })

  it('limpar esvazia os campos sem sair da tela', async () => {
    montar()
    await userEvent.type(screen.getByLabelText('Código da ordem'), 'OP-1')

    await userEvent.click(screen.getByRole('button', { name: 'Limpar' }))

    expect(screen.getByLabelText('Código da ordem')).toHaveValue('')
    expect(screen.getByTestId('rota')).toHaveTextContent(/^\/ordens$/)
  })

  it('oferece criar a primeira ordem quando não há nenhuma', async () => {
    pesquisarMock.mockResolvedValue(pagina([]))
    montar()

    expect(await screen.findByText('Nenhuma ordem cadastrada')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Criar primeira ordem' })).toBeInTheDocument()
  })

  it('explica a falta de conexão em linguagem simples', async () => {
    pesquisarMock.mockRejectedValue(erroDeRede())
    montar()

    expect(await screen.findByText(/não foi possível conectar ao sistema/i)).toBeInTheDocument()
  })
})
