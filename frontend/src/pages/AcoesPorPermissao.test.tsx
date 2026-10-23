import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listarListasTecnicas, pesquisarListasTecnicas } from '../api/listasTecnicas'
import { listarMateriais, pesquisarMateriais } from '../api/materiais'
import { pesquisarNotasFiscais } from '../api/notasFiscais'
import { pesquisarOrdens } from '../api/ordens'
import { listarTiposOrdem } from '../api/tiposOrdem'
import { listaTecnica, material, notaFiscal, ordem, pagina, TODAS_AS_PERMISSOES } from '../test/fabricas'
import { renderizarTela } from '../test/renderizar'
import type { Permissao } from '../types'
import { ListasTecnicasPage } from './ListasTecnicasPage'
import { MateriaisPage } from './MateriaisPage'
import { NotasFiscaisPage } from './NotasFiscaisPage'
import { OrdensPage } from './OrdensPage'
import { TiposOrdemPage } from './TiposOrdemPage'

vi.mock('../api/ordens', async (importarOriginal) => ({ ...(await importarOriginal<typeof import('../api/ordens')>()), pesquisarOrdens: vi.fn() }))
vi.mock('../api/materiais', async (importarOriginal) => ({
  ...(await importarOriginal<typeof import('../api/materiais')>()), pesquisarMateriais: vi.fn(), listarMateriais: vi.fn(),
}))
vi.mock('../api/listasTecnicas', async (importarOriginal) => ({
  ...(await importarOriginal<typeof import('../api/listasTecnicas')>()), pesquisarListasTecnicas: vi.fn(), listarListasTecnicas: vi.fn(),
}))
vi.mock('../api/notasFiscais', async (importarOriginal) => ({
  ...(await importarOriginal<typeof import('../api/notasFiscais')>()), pesquisarNotasFiscais: vi.fn(),
}))
vi.mock('../api/tiposOrdem', async (importarOriginal) => ({ ...(await importarOriginal<typeof import('../api/tiposOrdem')>()), listarTiposOrdem: vi.fn() }))

const sem = (...retiradas: Permissao[]) => TODAS_AS_PERMISSOES.filter((p) => !retiradas.includes(p))

beforeEach(() => {
  vi.mocked(pesquisarOrdens).mockReset().mockResolvedValue(pagina([ordem({ id: 'o-1', codigo: 'OP-1' })]))
  vi.mocked(pesquisarMateriais).mockReset().mockResolvedValue(pagina([material()]))
  vi.mocked(listarMateriais).mockReset().mockResolvedValue([material()])
  vi.mocked(pesquisarListasTecnicas).mockReset().mockResolvedValue(pagina([listaTecnica({ status: 'ATIVA' })]))
  vi.mocked(listarListasTecnicas).mockReset().mockResolvedValue([listaTecnica()])
  vi.mocked(pesquisarNotasFiscais).mockReset().mockResolvedValue(pagina([notaFiscal()]))
  vi.mocked(listarTiposOrdem).mockReset().mockResolvedValue([{ id: 't-1', nome: 'Produção', descricao: '', cor: '#1565c0' }] as never)
})

describe('botões de cadastro por permissão', () => {
  it('Ordens: "Nova Ordem" só com ORDEM_CRIAR; o lápis de status só com ORDEM_ATUALIZAR_STATUS', async () => {
    const { unmount } = renderizarTela(<OrdensPage />)
    await screen.findByText('OP-1')
    expect(screen.getByRole('button', { name: 'Nova Ordem' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Atualizar status' })).toBeInTheDocument()
    unmount()

    renderizarTela(<OrdensPage />, { permissoes: sem('ORDEM_CRIAR', 'ORDEM_ATUALIZAR_STATUS') })
    await screen.findByText('OP-1')
    expect(screen.queryByRole('button', { name: 'Nova Ordem' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Atualizar status' })).not.toBeInTheDocument()
  })

  it('Ordens: sem ORDEM_CRIAR o estado vazio não oferece "Criar primeira ordem"', async () => {
    vi.mocked(pesquisarOrdens).mockResolvedValue(pagina([], { total: 0 }))
    renderizarTela(<OrdensPage />, { permissoes: sem('ORDEM_CRIAR') })

    expect(await screen.findByText('Nenhuma ordem cadastrada')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Criar primeira ordem' })).not.toBeInTheDocument()
  })

  it('Materiais: "Novo Material" só com MATERIAL_CADASTRAR', async () => {
    const { unmount } = renderizarTela(<MateriaisPage />)
    expect(await screen.findByRole('button', { name: 'Novo Material' })).toBeInTheDocument()
    unmount()

    renderizarTela(<MateriaisPage />, { permissoes: sem('MATERIAL_CADASTRAR') })
    await screen.findByRole('heading', { name: 'Materiais' })
    expect(screen.queryByRole('button', { name: 'Novo Material' })).not.toBeInTheDocument()
  })

  it('Listas técnicas: "Nova Lista" só com LISTA_CADASTRAR', async () => {
    const { unmount } = renderizarTela(<ListasTecnicasPage />)
    expect(await screen.findByRole('button', { name: 'Nova Lista' })).toBeInTheDocument()
    unmount()

    renderizarTela(<ListasTecnicasPage />, { permissoes: sem('LISTA_CADASTRAR') })
    await screen.findByRole('heading', { name: 'Listas Técnicas' })
    expect(screen.queryByRole('button', { name: 'Nova Lista' })).not.toBeInTheDocument()
  })

  it('Entrada de notas: "Nova entrada" só com NOTA_FISCAL_REGISTRAR', async () => {
    const { unmount } = renderizarTela(<NotasFiscaisPage />)
    expect(await screen.findByRole('button', { name: 'Nova entrada' })).toBeInTheDocument()
    unmount()

    renderizarTela(<NotasFiscaisPage />, { permissoes: sem('NOTA_FISCAL_REGISTRAR') })
    await screen.findByRole('heading', { name: 'Entrada de notas' })
    expect(screen.queryByRole('button', { name: 'Nova entrada' })).not.toBeInTheDocument()
  })

  it('Tipos de ordem: "Novo Tipo" e o editar só com TIPO_ORDEM_GERENCIAR', async () => {
    const { unmount } = renderizarTela(<TiposOrdemPage />)
    await screen.findByText('Produção')
    expect(screen.getByRole('button', { name: 'Novo Tipo' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Editar' })).toBeInTheDocument()
    unmount()

    renderizarTela(<TiposOrdemPage />, { permissoes: sem('TIPO_ORDEM_GERENCIAR') })
    await screen.findByText('Produção')
    expect(screen.queryByRole('button', { name: 'Novo Tipo' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Editar' })).not.toBeInTheDocument()
  })
})
