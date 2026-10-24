import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listarTiposOrdem } from '../api/tiposOrdem'
import { renderizarTela } from '../test/renderizar'
import { TiposOrdemPage } from './TiposOrdemPage'

vi.mock('../api/tiposOrdem', async (importarOriginal) => ({
  ...(await importarOriginal<typeof import('../api/tiposOrdem')>()),
  listarTiposOrdem: vi.fn(),
}))

const tipo = (extras: Record<string, unknown>) => ({
  id: 't-1', nome: 'Produção Normal', descricao: 'Ordem planejada', cor: '#1565c0', conversao: false,
  criadoEm: '2026-10-01T10:00:00Z', atualizadoEm: '2026-10-01T10:00:00Z', criadoPor: 'admin', atualizadoPor: 'admin', ...extras,
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
