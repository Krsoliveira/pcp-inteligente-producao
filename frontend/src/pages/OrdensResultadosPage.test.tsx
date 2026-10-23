import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { pesquisarOrdens } from '../api/ordens'
import { erroHttp } from '../test/erros'
import { ordem, pagina } from '../test/fabricas'
import { renderizarTela } from '../test/renderizar'
import { OrdensResultadosPage } from './OrdensResultadosPage'

vi.mock('../api/ordens', async (importarOriginal) => ({
  ...(await importarOriginal<typeof import('../api/ordens')>()),
  pesquisarOrdens: vi.fn(),
}))

const pesquisarMock = vi.mocked(pesquisarOrdens)
const ROTA = '/ordens/resultados'

const tresOrdens = [
  ordem({ id: '1', codigo: 'OP-2026-0003', centroDeTrabalho: 'Usinagem CNC' }),
  ordem({ id: '2', codigo: 'OP-2026-0002', centroDeTrabalho: 'Soldagem MIG/TIG' }),
  ordem({ id: '3', codigo: 'OP-2026-0001', centroDeTrabalho: 'Usinagem CNC' }),
]

function montar(url = ROTA) {
  return renderizarTela(<OrdensResultadosPage />, { rotas: [url], padrao: ROTA })
}

describe('OrdensResultadosPage', () => {
  beforeEach(() => {
    pesquisarMock.mockReset()
    pesquisarMock.mockResolvedValue(pagina(tresOrdens, { total: 3 }))
  })

  it('consulta o servidor com os filtros e a página que estão na URL', async () => {
    montar(`${ROTA}?busca=OP-2026&status=LIBERADA&pagina=1&tamanho=10`)

    await screen.findByText('OP-2026-0003')

    expect(pesquisarMock).toHaveBeenCalledWith({ busca: 'OP-2026', status: 'LIBERADA', pagina: 1, tamanho: 10 })
  })

  it('usa a primeira página com 25 itens quando a URL não tem filtros', async () => {
    montar()

    await screen.findByText('OP-2026-0003')

    expect(pesquisarMock).toHaveBeenCalledWith({ busca: undefined, status: undefined, pagina: 0, tamanho: 25 })
  })

  it('ignora um status inválido na URL', async () => {
    montar(`${ROTA}?status=INEXISTENTE`)

    await screen.findByText('OP-2026-0003')

    expect(pesquisarMock).toHaveBeenCalledWith(expect.objectContaining({ status: undefined }))
  })

  it('mostra as ordens e o total encontrado', async () => {
    montar(`${ROTA}?busca=OP`)

    expect(await screen.findByText('OP-2026-0002')).toBeInTheDocument()
    expect(screen.getByText('3 ordens encontradas')).toBeInTheDocument()
  })

  it('chama a consulta ao pesquisar, voltando para a primeira página', async () => {
    montar(`${ROTA}?pagina=3`)
    await screen.findByText('OP-2026-0003')

    await userEvent.type(screen.getByLabelText('Código da ordem'), 'OP-9')
    await userEvent.click(screen.getByRole('button', { name: 'Pesquisar' }))

    await waitFor(() => expect(screen.getByTestId('rota')).toHaveTextContent(`${ROTA}?busca=OP-9`))
    expect(pesquisarMock).toHaveBeenLastCalledWith(expect.objectContaining({ busca: 'OP-9', pagina: 0 }))
  })

  it('limpa os filtros', async () => {
    montar(`${ROTA}?busca=OP-9&status=LIBERADA`)
    await screen.findByText('OP-2026-0003')

    await userEvent.click(screen.getByRole('button', { name: 'Limpar' }))

    await waitFor(() => expect(screen.getByTestId('rota')).toHaveTextContent(ROTA))
    expect(screen.getByTestId('rota')).not.toHaveTextContent('busca')
    expect(screen.getByLabelText('Código da ordem')).toHaveValue('')
  })

  it('ordena pelo título da coluna, sem consultar o servidor de novo', async () => {
    montar()
    await screen.findByText('OP-2026-0003')
    const consultasAntes = pesquisarMock.mock.calls.length

    await userEvent.click(screen.getByRole('button', { name: 'Ordenar Código' }))

    expect(screen.getAllByText(/OP-2026-000\d/).map((c) => c.textContent)).toEqual(['OP-2026-0001', 'OP-2026-0002', 'OP-2026-0003'])

    await userEvent.click(screen.getByRole('button', { name: 'Ordenar Código' }))

    expect(screen.getAllByText(/OP-2026-000\d/).map((c) => c.textContent)).toEqual(['OP-2026-0003', 'OP-2026-0002', 'OP-2026-0001'])
    expect(pesquisarMock.mock.calls.length).toBe(consultasAntes)
  })

  it('filtra pelo campo da coluna só nas linhas já consultadas', async () => {
    montar()
    await screen.findByText('OP-2026-0003')
    const consultasAntes = pesquisarMock.mock.calls.length

    await userEvent.click(screen.getByRole('button', { name: 'Filtrar Centro de Trabalho' }))
    await userEvent.type(screen.getByRole('textbox', { name: 'Texto do filtro de Centro de Trabalho' }), 'soldagem')
    await userEvent.keyboard('{Escape}')

    expect(screen.getByText('OP-2026-0002')).toBeInTheDocument()
    expect(screen.queryByText('OP-2026-0003')).not.toBeInTheDocument()
    expect(pesquisarMock.mock.calls.length).toBe(consultasAntes)
  })

  it('consulta o servidor com o período do início planejado que está na URL', async () => {
    montar(`${ROTA}?de=2026-09-01&ate=2026-09-30`)

    await screen.findByText('OP-2026-0003')

    expect(pesquisarMock).toHaveBeenCalledWith(expect.objectContaining({ de: '2026-09-01', ate: '2026-09-30' }))
    expect(screen.getByText('3 ordens encontradas')).toBeInTheDocument()
  })

  it('ignora datas inválidas na URL', async () => {
    montar(`${ROTA}?de=ontem&ate=amanha`)

    await screen.findByText('OP-2026-0003')

    expect(pesquisarMock).toHaveBeenCalledWith(expect.objectContaining({ de: undefined, ate: undefined }))
  })

  it('não consulta o servidor quando o período da URL está invertido e avisa', async () => {
    montar(`${ROTA}?de=2026-09-30&ate=2026-09-01`)

    expect(await screen.findByText('O fim do período não pode ser anterior ao início.')).toBeInTheDocument()
    expect(pesquisarMock).not.toHaveBeenCalled()
  })

  it('pesquisar com período atualiza a URL e volta para a primeira página', async () => {
    montar(`${ROTA}?pagina=2`)
    await screen.findByText('OP-2026-0003')

    await userEvent.type(screen.getByLabelText('Início de'), '2026-09-01')
    await userEvent.click(screen.getByRole('button', { name: 'Pesquisar' }))

    await waitFor(() => expect(screen.getByTestId('rota')).toHaveTextContent(`${ROTA}?de=2026-09-01`))
  })

  it('não pesquisa com o período invertido nos campos e mostra o erro do campo', async () => {
    montar()
    await screen.findByText('OP-2026-0003')

    await userEvent.type(screen.getByLabelText('Início de'), '2026-09-30')
    await userEvent.type(screen.getByLabelText('até'), '2026-09-01')
    await userEvent.click(screen.getByRole('button', { name: 'Pesquisar' }))

    expect(screen.getByText('Anterior ao início.')).toBeInTheDocument()
    expect(screen.getByTestId('rota')).not.toHaveTextContent('de=')
  })

  it('mostra uma mensagem quando nenhuma ordem atende aos filtros', async () => {
    pesquisarMock.mockResolvedValue(pagina([], { total: 0 }))
    montar(`${ROTA}?busca=zzz`)

    expect(await screen.findByText('Nenhuma ordem encontrada com os filtros')).toBeInTheDocument()
    expect(screen.getByText('Tente ajustar os filtros de busca.')).toBeInTheDocument()
  })

  it('explica a falha de conexão em linguagem simples', async () => {
    pesquisarMock.mockRejectedValue(erroHttp(503))
    montar()

    const alerta = await screen.findByRole('alert')

    expect(within(alerta).getByText(/temporariamente indisponível/i)).toBeInTheDocument()
  })

  it('volta para a última página quando a página pedida deixou de existir', async () => {
    pesquisarMock.mockImplementation(async ({ pagina: numero }) =>
      numero === 5
        ? pagina([], { total: 30, pagina: 5, totalPaginas: 2 })
        : pagina(tresOrdens, { total: 30, pagina: numero ?? 0, totalPaginas: 2 }))
    montar(`${ROTA}?pagina=5`)

    await screen.findByText('OP-2026-0003')

    expect(screen.getByTestId('rota')).toHaveTextContent('pagina=1')
  })
})
