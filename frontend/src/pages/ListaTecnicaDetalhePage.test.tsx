import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ativarListaTecnica, buscarListaTecnicaPorId, cadastrarListaTecnica, listarListasTecnicas, retirarListaTecnicaDeUso } from '../api/listasTecnicas'
import { listarMateriais } from '../api/materiais'
import { erroDeRede, erroHttp } from '../test/erros'
import { listaTecnica, material } from '../test/fabricas'
import { renderizarTela } from '../test/renderizar'
import { ListaTecnicaDetalhePage } from './ListaTecnicaDetalhePage'

vi.mock('../api/listasTecnicas', () => ({
  buscarListaTecnicaPorId: vi.fn(),
  ativarListaTecnica: vi.fn(),
  retirarListaTecnicaDeUso: vi.fn(),
  listarListasTecnicas: vi.fn(),
  cadastrarListaTecnica: vi.fn(),
}))
vi.mock('../api/materiais', () => ({ listarMateriais: vi.fn() }))

const buscarMock = vi.mocked(buscarListaTecnicaPorId)
const ativarMock = vi.mocked(ativarListaTecnica)
const retirarMock = vi.mocked(retirarListaTecnicaDeUso)
const versoesMock = vi.mocked(listarListasTecnicas)
const cadastrarMock = vi.mocked(cadastrarListaTecnica)
const materiaisMock = vi.mocked(listarMateriais)

const PADRAO = '/listas-tecnicas/:id'

function montar() {
  return renderizarTela(<ListaTecnicaDetalhePage />, { rotas: ['/listas-tecnicas/l-1'], padrao: PADRAO })
}

describe('ListaTecnicaDetalhePage', () => {
  beforeEach(() => {
    buscarMock.mockReset()
    ativarMock.mockReset()
    retirarMock.mockReset()
    versoesMock.mockReset()
    cadastrarMock.mockReset()
    versoesMock.mockResolvedValue([listaTecnica()])
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

  it('oferece retirar de uso a versão ativa, pede confirmação e só então chama o sistema', async () => {
    buscarMock.mockResolvedValue(listaTecnica({ status: 'ATIVA' }))
    retirarMock.mockResolvedValue()
    montar()

    await userEvent.click(await screen.findByRole('button', { name: 'Retirar de uso' }))
    expect(retirarMock).not.toHaveBeenCalled()
    expect(screen.getByText(/não pode ser ativada de novo/i)).toBeInTheDocument()

    // com o diálogo aberto, o botão da página fica oculto e só o do diálogo responde
    await userEvent.click(screen.getByRole('button', { name: 'Retirar de uso' }))

    await waitFor(() => expect(retirarMock).toHaveBeenCalledWith('l-1'))
  })

  it('não oferece retirar de uso uma versão que não está ativa', async () => {
    montar()

    await screen.findByRole('heading', { name: 'Viga metálica' })

    expect(screen.queryByRole('button', { name: 'Retirar de uso' })).not.toBeInTheDocument()
  })

  it('lista as outras versões do mesmo material e leva à comparação', async () => {
    versoesMock.mockResolvedValue([
      listaTecnica({ id: 'l-1', versao: 'v1' }),
      listaTecnica({ id: 'l-2', versao: 'v2', status: 'ATIVA', itens: [] }),
    ])
    montar()

    await userEvent.click(await screen.findByRole('button', { name: 'Comparar com a versão v2' }))

    expect(screen.getByTestId('rota')).toHaveTextContent('/listas-tecnicas/comparar?a=l-1&b=l-2')
  })

  it('não mostra "outras versões" quando o material só tem esta', async () => {
    montar()

    await screen.findByRole('heading', { name: 'Viga metálica' })

    expect(screen.queryByText('Outras versões desta lista')).not.toBeInTheDocument()
  })

  it('oferece criar uma nova versão a partir da que está aberta, em qualquer status', async () => {
    buscarMock.mockResolvedValue(listaTecnica({ status: 'OBSOLETA' }))
    montar()

    await userEvent.click(await screen.findByRole('button', { name: 'Nova versão a partir desta' }))

    expect(await screen.findByText('Nova versão a partir da v1')).toBeInTheDocument()
    expect(screen.getByLabelText(/^Versão/)).toHaveValue('v2')
  })

  it('sem LISTA_CADASTRAR não oferece criar uma nova versão', async () => {
    renderizarTela(<ListaTecnicaDetalhePage />, {
      rotas: ['/listas-tecnicas/l-1'], padrao: PADRAO, permissoes: ['LISTA_VISUALIZAR_TODAS', 'LISTA_ATIVAR'],
    })

    await screen.findByRole('heading', { name: 'Viga metálica' })

    expect(screen.queryByRole('button', { name: 'Nova versão a partir desta' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ativar esta versão' })).toBeInTheDocument()
  })

  it('depois de criar a versão nova, abre o detalhe dela', async () => {
    const v1 = listaTecnica({ id: 'l-1', versao: 'v1' })
    const v2 = listaTecnica({ id: 'l-9', versao: 'v2' })
    let criada = false
    versoesMock.mockImplementation(async () => (criada ? [v1, v2] : [v1]))
    cadastrarMock.mockImplementation(async () => { criada = true })
    montar()

    await userEvent.click(await screen.findByRole('button', { name: 'Nova versão a partir desta' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Criar versão' }))

    await waitFor(() => expect(screen.getByTestId('rota')).toHaveTextContent('/listas-tecnicas/l-9'))
    expect(cadastrarMock.mock.calls[0][0]).toMatchObject({ materialId: 'm-1', versao: 'v2' })
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
