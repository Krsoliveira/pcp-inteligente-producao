import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { criarOrdem } from '../api/ordens'
import { listarListasTecnicas } from '../api/listasTecnicas'
import { listarMateriais } from '../api/materiais'
import { listarTiposOrdem } from '../api/tiposOrdem'
import { listaTecnica, material } from '../test/fabricas'
import { renderizarTela } from '../test/renderizar'
import { TODAS_AS_PERMISSOES } from '../test/fabricas'
import type { Permissao } from '../types'
import { CriarOrdemDialog } from './CriarOrdemDialog'

vi.mock('../api/ordens', () => ({ criarOrdem: vi.fn() }))
vi.mock('../api/listasTecnicas', () => ({ listarListasTecnicas: vi.fn() }))
vi.mock('../api/materiais', () => ({ listarMateriais: vi.fn() }))
vi.mock('../api/tiposOrdem', () => ({ listarTiposOrdem: vi.fn() }))

const listasMock = vi.mocked(listarListasTecnicas)
const criarMock = vi.mocked(criarOrdem)

function montar(permissoes?: Permissao[]) {
  return renderizarTela(<CriarOrdemDialog aberto onFechar={() => {}} />, { permissoes })
}

async function escolherMaterial() {
  await userEvent.click(await screen.findByRole('combobox', { name: /^Material/ }))
  await userEvent.click(await screen.findByRole('option', { name: /103\.000\.001/ }))
}

async function preencherOrdem() {
  await userEvent.type(screen.getByLabelText(/^Código/), 'OP-0001')
  await userEvent.click(screen.getByRole('combobox', { name: /^Centro de Trabalho/ }))
  await userEvent.click(await screen.findByRole('option', { name: 'Montagem' }))
  await userEvent.type(screen.getByLabelText(/^Início Planejado/), '2026-11-01')
  await userEvent.type(screen.getByLabelText(/^Fim Planejado/), '2026-11-10')
}

describe('CriarOrdemDialog: versão da lista técnica', () => {
  beforeEach(() => {
    listasMock.mockReset()
    criarMock.mockReset()
    vi.mocked(listarMateriais).mockResolvedValue([material({ id: 'm-1', codigo: '103000001', descricao: 'Viga metálica' })])
    vi.mocked(listarTiposOrdem).mockResolvedValue([])
  })

  it('sem TIPO_ORDEM_VISUALIZAR não pede os tipos de ordem e esconde o campo opcional', async () => {
    listasMock.mockResolvedValue([])
    montar(TODAS_AS_PERMISSOES.filter((p) => p !== 'TIPO_ORDEM_VISUALIZAR'))

    await screen.findByText('Nova Ordem de Produção')

    expect(listarTiposOrdem).not.toHaveBeenCalled()
    expect(screen.queryByLabelText(/Tipo de Ordem/)).not.toBeInTheDocument()
  })

  it('com TIPO_ORDEM_VISUALIZAR pede os tipos e mostra o campo', async () => {
    listasMock.mockResolvedValue([])
    montar()

    expect(await screen.findByLabelText(/Tipo de Ordem/)).toBeInTheDocument()
    expect(listarTiposOrdem).toHaveBeenCalled()
  })

  it('com uma só versão em uso, ela já vem selecionada', async () => {
    listasMock.mockResolvedValue([
      listaTecnica({ id: 'l-1', versao: 'v1', status: 'ATIVA' }),
      listaTecnica({ id: 'l-2', versao: 'v2', status: 'EM_REVISAO' }),
    ])
    montar()

    await escolherMaterial()

    await waitFor(() => expect(screen.getByRole('combobox', { name: /Versão da Lista Técnica/ })).toHaveTextContent('v1'))
  })

  it('versões em revisão ou obsoletas não aparecem para escolha', async () => {
    listasMock.mockResolvedValue([
      listaTecnica({ id: 'l-1', versao: 'v1', status: 'ATIVA' }),
      listaTecnica({ id: 'l-2', versao: 'v2', status: 'ATIVA' }),
      listaTecnica({ id: 'l-3', versao: 'v3', status: 'EM_REVISAO' }),
      listaTecnica({ id: 'l-4', versao: 'v0', status: 'OBSOLETA' }),
    ])
    montar()
    await escolherMaterial()

    await userEvent.click(await screen.findByRole('combobox', { name: /Versão da Lista Técnica/ }))

    expect(screen.getByRole('option', { name: /^v1/ })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: /^v2/ })).toBeInTheDocument()
    expect(screen.queryByRole('option', { name: /^v3/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('option', { name: /^v0/ })).not.toBeInTheDocument()
  })

  it('com mais de uma versão em uso nada vem selecionado e a escolha é obrigatória', async () => {
    listasMock.mockResolvedValue([
      listaTecnica({ id: 'l-1', versao: 'v1', status: 'ATIVA' }),
      listaTecnica({ id: 'l-2', versao: 'v2', status: 'ATIVA' }),
    ])
    montar()
    await escolherMaterial()
    expect(await screen.findByText(/Este material tem 2 versões em uso/)).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: /Versão da Lista Técnica/ })).not.toHaveTextContent('v1')
    await preencherOrdem()

    await userEvent.click(screen.getByRole('button', { name: 'Criar Ordem' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/escolha qual usar nesta ordem/i)
    expect(criarMock).not.toHaveBeenCalled()
  })

  it('envia a versão escolhida entre as que estão em uso', async () => {
    listasMock.mockResolvedValue([
      listaTecnica({ id: 'l-1', versao: 'v1', status: 'ATIVA' }),
      listaTecnica({ id: 'l-2', versao: 'v2', status: 'ATIVA' }),
    ])
    criarMock.mockResolvedValue({} as Awaited<ReturnType<typeof criarOrdem>>)
    montar()
    await escolherMaterial()
    await userEvent.click(await screen.findByRole('combobox', { name: /Versão da Lista Técnica/ }))
    await userEvent.click(await screen.findByRole('option', { name: /^v2/ }))
    await preencherOrdem()

    await userEvent.click(screen.getByRole('button', { name: 'Criar Ordem' }))

    // o React Query repassa mais argumentos além do pedido; só o primeiro interessa
    await waitFor(() => expect(criarMock).toHaveBeenCalled())
    expect(criarMock.mock.calls[0][0]).toMatchObject({ materialId: 'm-1', listaTecnicaId: 'l-2' })
  })

  it('material sem versão em uso avisa e não deixa escolher', async () => {
    listasMock.mockResolvedValue([listaTecnica({ id: 'l-3', versao: 'v1', status: 'EM_REVISAO' })])
    montar()

    await escolherMaterial()

    expect(await screen.findByText('Nenhuma versão da lista técnica em uso para este material.')).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: /Versão da Lista Técnica/ })).toHaveAttribute('aria-disabled', 'true')
  })
})
