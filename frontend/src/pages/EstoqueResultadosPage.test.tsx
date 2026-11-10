import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { consultarResumoEstoque, pesquisarEstoque } from '../api/estoque'
import { pagina, posicaoEstoque } from '../test/fabricas'
import { renderizarTela } from '../test/renderizar'
import { EstoqueResultadosPage } from './EstoqueResultadosPage'

vi.mock('../api/estoque', () => ({ pesquisarEstoque: vi.fn(), consultarResumoEstoque: vi.fn() }))

const pesquisarMock = vi.mocked(pesquisarEstoque)
const ROTA = '/estoque/resultados'

const posicoes = [
  posicaoEstoque({ materialId: 'm-2', codigo: '110000001', descricao: 'Aço inox', saldoDisponivel: 750 }),
  posicaoEstoque({ materialId: 'm-3', codigo: '110000002', descricao: 'Alumínio', saldoDisponivel: 0, lotesDisponiveis: 0 }),
]

function montar(url = ROTA) {
  return renderizarTela(<EstoqueResultadosPage />, { rotas: [url], padrao: ROTA })
}

describe('EstoqueResultadosPage', () => {
  beforeEach(() => {
    pesquisarMock.mockReset()
    pesquisarMock.mockResolvedValue(pagina(posicoes, { total: 2 }))
    vi.mocked(consultarResumoEstoque).mockResolvedValue({ totalMateriais: 2, materiaisVencendo: 0, materiasPrimasSemSaldo: 1 })
  })

  it('um só resultado fica no singular, no total e no resumo da tabela', async () => {
    pesquisarMock.mockResolvedValue(pagina([posicoes[0]], { total: 1 }))
    montar(`${ROTA}?busca=aço`)

    expect(await screen.findByText('1 material encontrado · saldo calculado a partir dos lotes')).toBeInTheDocument()
    expect(screen.getByText(/^1 linha nesta página/)).toBeInTheDocument()
  })

  it('o resumo da tabela é anunciado ao leitor de tela quando o resultado muda', async () => {
    montar()

    expect(await screen.findByRole('status')).toHaveTextContent(/^2 linhas nesta página/)
  })

  it('cada material abre por um link de verdade, sem depender do mouse', async () => {
    montar()

    expect(await screen.findByRole('link', { name: '110.000.001' })).toHaveAttribute('href', '/estoque/m-2')
  })

  it('consulta o servidor com os filtros e a página da URL', async () => {
    montar(`${ROTA}?busca=aço&tipo=MATERIA_PRIMA&somenteComSaldo=true&pagina=2&tamanho=50`)

    await screen.findByText('Aço inox')

    expect(pesquisarMock).toHaveBeenCalledWith({
      busca: 'aço', tipo: 'MATERIA_PRIMA', somenteComSaldo: true, pagina: 2, tamanho: 50,
    })
  })

  it('ignora um tipo inválido e trata "só com saldo" ausente como desligado', async () => {
    montar(`${ROTA}?tipo=INEXISTENTE`)

    await screen.findByText('Aço inox')

    expect(pesquisarMock).toHaveBeenCalledWith({
      busca: undefined, tipo: undefined, somenteComSaldo: undefined, pagina: 0, tamanho: 25,
    })
  })

  it('mostra código formatado, saldo com unidade e o alerta de matéria-prima sem saldo', async () => {
    montar()

    expect(await screen.findByText('110.000.001')).toBeInTheDocument()
    expect(screen.getByText('750 kg')).toBeInTheDocument()
    expect(await screen.findByText(/1 matéria-prima está sem saldo disponível/i)).toBeInTheDocument()
  })

  it('ligar "Só com saldo" já aplica o filtro, sem apertar Pesquisar', async () => {
    montar()
    await screen.findByText('Aço inox')

    await userEvent.click(screen.getByRole('switch', { name: 'Só com saldo' }))

    await waitFor(() => expect(screen.getByTestId('rota')).toHaveTextContent(`${ROTA}?somenteComSaldo=true`))
    await waitFor(() => expect(pesquisarMock).toHaveBeenLastCalledWith(expect.objectContaining({ somenteComSaldo: true })))
  })

  it('desligar "Só com saldo" já tira o filtro', async () => {
    montar(`${ROTA}?somenteComSaldo=true`)
    await screen.findByText('Aço inox')

    await userEvent.click(screen.getByRole('switch', { name: 'Só com saldo' }))

    await waitFor(() => expect(screen.getByTestId('rota')).not.toHaveTextContent('somenteComSaldo'))
    await waitFor(() => expect(pesquisarMock).toHaveBeenLastCalledWith(expect.objectContaining({ somenteComSaldo: undefined })))
  })

  it('ao ligar "Só com saldo" o texto já digitado também é aplicado', async () => {
    montar()
    await screen.findByText('Aço inox')

    await userEvent.type(screen.getByLabelText('Código ou descrição'), 'aço')
    await userEvent.click(screen.getByRole('switch', { name: 'Só com saldo' }))

    await waitFor(() => expect(screen.getByTestId('rota')).toHaveTextContent(`${ROTA}?busca=a%C3%A7o&somenteComSaldo=true`))
  })

  it('ordena as linhas da página pelo saldo disponível, sem consultar de novo', async () => {
    montar()
    await screen.findByText('Aço inox')
    const consultasAntes = pesquisarMock.mock.calls.length

    await userEvent.click(screen.getByRole('button', { name: 'Ordenar Disponível' }))

    expect(screen.getAllByText(/^(Aço inox|Alumínio)$/).map((c) => c.textContent)).toEqual(['Alumínio', 'Aço inox'])
    expect(pesquisarMock.mock.calls.length).toBe(consultasAntes)
  })

  it('limpa os filtros da URL', async () => {
    montar(`${ROTA}?busca=aço&somenteComSaldo=true`)
    await screen.findByText('Aço inox')

    await userEvent.click(screen.getByRole('button', { name: 'Limpar' }))

    await waitFor(() => expect(screen.getByTestId('rota')).toHaveTextContent(ROTA))
    expect(screen.getByTestId('rota')).not.toHaveTextContent('busca')
    expect(screen.getByRole('switch', { name: 'Só com saldo' })).not.toBeChecked()
  })

  it('mostra uma mensagem quando nenhum material atende aos filtros', async () => {
    pesquisarMock.mockResolvedValue(pagina([], { total: 0 }))
    montar(`${ROTA}?busca=zzz`)

    expect(await screen.findByText('Nenhum material encontrado com os filtros')).toBeInTheDocument()
  })
})
