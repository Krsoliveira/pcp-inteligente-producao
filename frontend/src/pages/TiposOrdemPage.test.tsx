import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { atualizarTipoOrdem, buscarTipoOrdem, listarTiposOrdem } from '../api/tiposOrdem'
import { erroHttp } from '../test/erros'
import { renderizarTela } from '../test/renderizar'
import { TiposOrdemPage } from './TiposOrdemPage'

vi.mock('../api/tiposOrdem', async (importarOriginal) => ({
  ...(await importarOriginal<typeof import('../api/tiposOrdem')>()),
  listarTiposOrdem: vi.fn(),
  atualizarTipoOrdem: vi.fn(),
  buscarTipoOrdem: vi.fn(),
}))

const tipo = (extras: Record<string, unknown>) => ({
  id: 't-1', nome: 'Produção Normal', descricao: 'Ordem planejada', cor: '#1565c0', conversao: false,
  criadoEm: '2026-10-01T10:00:00Z', atualizadoEm: '2026-10-01T10:00:00Z', criadoPor: 'admin', atualizadoPor: 'admin', versao: 0, ...extras,
})

describe('TiposOrdemPage: tipo de sistema Conversão', () => {
  beforeEach(() => {
    vi.mocked(listarTiposOrdem).mockReset().mockResolvedValue([
      tipo({}), tipo({ id: 't-conv', nome: 'Conversão', descricao: 'Acabado em semiacabado', cor: '#6a1b9a', conversao: true }),
    ] as never)
  })

  it('o tipo Conversão aparece como do sistema e não tem o botão de editar', async () => {
    renderizarTela(<TiposOrdemPage />)

    await screen.findByText('Conversão')

    expect(screen.getByText('Sistema')).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Editar' })).toHaveLength(1) // só o tipo comum
  })

  it('os tipos do usuário seguem editáveis e sem o selo', async () => {
    vi.mocked(listarTiposOrdem).mockResolvedValue([tipo({})] as never)
    renderizarTela(<TiposOrdemPage />)

    await screen.findByText('Produção Normal')

    expect(screen.queryByText('Sistema')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Editar' })).toBeInTheDocument()
  })
})

describe('TiposOrdemPage: edição', () => {
  beforeEach(() => {
    vi.mocked(listarTiposOrdem).mockReset().mockResolvedValue([tipo({})] as never)
  })

  it('abre a edição com os dados do tipo escolhido', async () => {
    renderizarTela(<TiposOrdemPage />)

    await userEvent.click(await screen.findByRole('button', { name: 'Editar' }))

    expect(await screen.findByLabelText(/^Nome/)).toHaveValue('Produção Normal')
    expect(screen.getByLabelText('Descrição')).toHaveValue('Ordem planejada')
  })

  it('envia a versão do tipo que abriu, para o servidor recusar a edição sobre dados antigos', async () => {
    vi.mocked(listarTiposOrdem).mockResolvedValue([tipo({ versao: 4 })] as never)
    vi.mocked(atualizarTipoOrdem).mockReset().mockResolvedValue(tipo({}) as never)
    renderizarTela(<TiposOrdemPage />)

    await userEvent.click(await screen.findByRole('button', { name: 'Editar' }))
    await userEvent.click(screen.getByRole('button', { name: 'Salvar' }))

    await waitFor(() => expect(atualizarTipoOrdem).toHaveBeenCalled())
    expect(vi.mocked(atualizarTipoOrdem).mock.calls[0]).toEqual(['t-1', expect.objectContaining({ nome: 'Produção Normal', versao: 4 })])
  })

  it('outra pessoa salvou antes: avisa, mantém o que foi digitado e carrega a versão atual quando pedido', async () => {
    vi.mocked(atualizarTipoOrdem).mockReset().mockRejectedValueOnce(erroHttp(409, {
      detail: 'O tipo de ordem "Produção Normal" foi alterado por outra pessoa enquanto você editava.',
      codigo: 'EDICAO_CONCORRENTE',
    }))
    vi.mocked(buscarTipoOrdem).mockReset().mockResolvedValue(tipo({ descricao: 'Mudado pela Ana', versao: 1 }) as never)
    renderizarTela(<TiposOrdemPage />)
    await userEvent.click(await screen.findByRole('button', { name: 'Editar' }))
    const descricao = await screen.findByLabelText('Descrição')
    await userEvent.clear(descricao)
    await userEvent.type(descricao, 'Do Bruno')

    await userEvent.click(screen.getByRole('button', { name: 'Salvar' }))

    expect(await screen.findByText(/foi alterado por outra pessoa/)).toBeInTheDocument()
    expect(screen.getByLabelText('Descrição')).toHaveValue('Do Bruno')

    await userEvent.click(screen.getByRole('button', { name: 'Carregar versão atual' }))

    await waitFor(() => expect(screen.getByLabelText('Descrição')).toHaveValue('Mudado pela Ana'))
    vi.mocked(atualizarTipoOrdem).mockResolvedValue(tipo({}) as never)
    await userEvent.click(screen.getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(atualizarTipoOrdem).toHaveBeenCalledTimes(2))
    expect(vi.mocked(atualizarTipoOrdem).mock.calls[1][1].versao).toBe(1)
  })

  it('outro conflito (nome repetido) mostra o motivo sem oferecer carregar a versão atual', async () => {
    vi.mocked(atualizarTipoOrdem).mockReset().mockRejectedValue(erroHttp(409, { detail: 'Já existe um tipo com esse nome.' }))
    renderizarTela(<TiposOrdemPage />)
    await userEvent.click(await screen.findByRole('button', { name: 'Editar' }))

    await userEvent.click(await screen.findByRole('button', { name: 'Salvar' }))

    expect(await screen.findByText('Já existe um tipo com esse nome.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Carregar versão atual' })).not.toBeInTheDocument()
  })
})
