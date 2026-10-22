import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ativarListaTecnica, buscarListaTecnicaPorId } from '../api/listasTecnicas'
import { listarMateriais } from '../api/materiais'
import { erroDeRede, erroHttp } from '../test/erros'
import { listaTecnica, material } from '../test/fabricas'
import { renderizarTela } from '../test/renderizar'
import { ListaTecnicaDetalhePage } from './ListaTecnicaDetalhePage'

vi.mock('../api/listasTecnicas', () => ({
  buscarListaTecnicaPorId: vi.fn(),
  ativarListaTecnica: vi.fn(),
}))
vi.mock('../api/materiais', () => ({ listarMateriais: vi.fn() }))

const buscarMock = vi.mocked(buscarListaTecnicaPorId)
const ativarMock = vi.mocked(ativarListaTecnica)
const materiaisMock = vi.mocked(listarMateriais)

const PADRAO = '/listas-tecnicas/:id'

function montar() {
  return renderizarTela(<ListaTecnicaDetalhePage />, { rotas: ['/listas-tecnicas/l-1'], padrao: PADRAO })
}

describe('ListaTecnicaDetalhePage', () => {
  beforeEach(() => {
    buscarMock.mockReset()
    ativarMock.mockReset()
    materiaisMock.mockReset()
    materiaisMock.mockResolvedValue([
      material({ id: 'm-1', codigo: '103000001', descricao: 'Viga metálica' }),
      material({ id: 'm-2', codigo: '110000001', descricao: 'Aço inox', tipo: 'MATERIA_PRIMA', unidadeDeMedida: 'kg' }),
    ])
    buscarMock.mockResolvedValue(listaTecnica())
  })

  it('mostra o produto, a versão e a receita com cada componente', async () => {
    montar()

    expect(await screen.findByRole('heading', { name: 'Viga metálica' })).toBeInTheDocument()
    expect(screen.getByText('103.000.001 · versão v1')).toBeInTheDocument()
    expect(await screen.findByText('Aço inox')).toBeInTheDocument()
    expect(screen.getByText('110.000.001')).toBeInTheDocument()
    expect(screen.getByText('Matéria-Prima')).toBeInTheDocument()
    expect(screen.getByText('2,5 kg')).toBeInTheDocument()
  })

  it('oferece ativar a versão que está em revisão e chama o sistema ao clicar', async () => {
    ativarMock.mockResolvedValue()
    montar()

    await userEvent.click(await screen.findByRole('button', { name: 'Ativar esta versão' }))

    expect(ativarMock).toHaveBeenCalledWith('l-1')
  })

  it('não oferece ativar uma versão que já está ativa', async () => {
    buscarMock.mockResolvedValue(listaTecnica({ status: 'ATIVA' }))
    montar()

    await screen.findByRole('heading', { name: 'Viga metálica' })

    expect(screen.queryByRole('button', { name: 'Ativar esta versão' })).not.toBeInTheDocument()
  })

  it('mostra o motivo quando não consegue ativar', async () => {
    ativarMock.mockRejectedValue(erroHttp(422, { detail: 'Só é possível ativar listas em revisão.' }))
    montar()

    await userEvent.click(await screen.findByRole('button', { name: 'Ativar esta versão' }))

    expect(await screen.findByText('Só é possível ativar listas em revisão.')).toBeInTheDocument()
  })

  it('diz "não encontrada" apenas quando o sistema responde 404', async () => {
    buscarMock.mockRejectedValue(erroHttp(404))
    montar()

    expect(await screen.findByText('Lista técnica não encontrada.')).toBeInTheDocument()
  })

  it('não diz "não encontrada" quando o problema é a conexão', async () => {
    buscarMock.mockRejectedValue(erroDeRede())
    montar()

    expect(await screen.findByText(/não foi possível conectar ao sistema/i)).toBeInTheDocument()
    expect(screen.queryByText('Lista técnica não encontrada.')).not.toBeInTheDocument()
  })
})
