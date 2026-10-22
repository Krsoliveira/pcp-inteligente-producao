import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listarMateriais } from '../api/materiais'
import { pesquisarNotasFiscais } from '../api/notasFiscais'
import { lote, material, notaFiscal, pagina } from '../test/fabricas'
import { renderizarTela } from '../test/renderizar'
import { NotasFiscaisResultadosPage } from './NotasFiscaisResultadosPage'

vi.mock('../api/notasFiscais', () => ({ pesquisarNotasFiscais: vi.fn() }))
vi.mock('../api/materiais', () => ({ listarMateriais: vi.fn() }))

const pesquisarMock = vi.mocked(pesquisarNotasFiscais)
const ROTA = '/notas-fiscais/resultados'

const notas = [
  notaFiscal({ id: 'nf-3', numero: 'NF-003', fornecedor: 'Aços Brasil', itens: [lote({ numeroLote: 'LOTE-B7' })] }),
  notaFiscal({ id: 'nf-2', numero: 'NF-002', fornecedor: 'Metais Sul', itens: [lote({ numeroLote: 'ZINCO-9' })] }),
]

function montar(url = ROTA) {
  return renderizarTela(<NotasFiscaisResultadosPage />, { rotas: [url], padrao: ROTA })
}

describe('NotasFiscaisResultadosPage', () => {
  beforeEach(() => {
    pesquisarMock.mockReset()
    pesquisarMock.mockResolvedValue(pagina(notas, { total: 2 }))
    vi.mocked(listarMateriais).mockResolvedValue([material({ id: 'm-2', codigo: '110000001', descricao: 'Aço inox' })])
  })

  it('consulta o servidor com a busca, o período e a página que estão na URL', async () => {
    montar(`${ROTA}?busca=aço&de=2026-09-01&ate=2026-09-30&pagina=1&tamanho=10`)

    await screen.findByText('NF-003')

    expect(pesquisarMock).toHaveBeenCalledWith({
      busca: 'aço', de: '2026-09-01', ate: '2026-09-30', pagina: 1, tamanho: 10,
    })
  })

  it('ignora datas inválidas na URL', async () => {
    montar(`${ROTA}?de=ontem&ate=amanha`)

    await screen.findByText('NF-003')

    expect(pesquisarMock).toHaveBeenCalledWith(expect.objectContaining({ de: undefined, ate: undefined }))
  })

  it('não consulta o servidor quando o período da URL está invertido e avisa', async () => {
    montar(`${ROTA}?de=2026-09-30&ate=2026-09-01`)

    expect(await screen.findByText('O fim do período não pode ser anterior ao início.')).toBeInTheDocument()
    expect(pesquisarMock).not.toHaveBeenCalled()
  })

  it('mostra as notas, o total e o código do material de cada uma', async () => {
    montar(`${ROTA}?busca=nf`)

    expect(await screen.findByText('NF-002')).toBeInTheDocument()
    expect(screen.getByText('2 notas fiscais encontradas')).toBeInTheDocument()
    expect((await screen.findAllByText('110.000.001')).length).toBeGreaterThan(0)
  })

  it('o subfiltro encontra a nota pelo número de lote de um item, sem nova consulta', async () => {
    montar()
    await screen.findByText('NF-003')
    const consultasAntes = pesquisarMock.mock.calls.length

    await userEvent.type(screen.getByPlaceholderText(/filtrar nas linhas desta página/i), 'zinco')

    expect(screen.getByText('NF-002')).toBeInTheDocument()
    expect(screen.queryByText('NF-003')).not.toBeInTheDocument()
    expect(pesquisarMock.mock.calls.length).toBe(consultasAntes)
  })

  it('pesquisa atualizando a URL e voltando para a primeira página', async () => {
    montar(`${ROTA}?pagina=2`)
    await screen.findByText('NF-003')

    await userEvent.type(screen.getByLabelText('Nota, fornecedor ou lote'), 'ZINCO-9')
    await userEvent.click(screen.getByRole('button', { name: 'Pesquisar' }))

    await waitFor(() => expect(screen.getByTestId('rota')).toHaveTextContent(`${ROTA}?busca=ZINCO-9`))
  })

  it('não pesquisa com o período invertido nos campos e mostra o erro do campo', async () => {
    montar()
    await screen.findByText('NF-003')

    await userEvent.type(screen.getByLabelText('Recebida de'), '2026-09-30')
    await userEvent.type(screen.getByLabelText('até'), '2026-09-01')
    await userEvent.click(screen.getByRole('button', { name: 'Pesquisar' }))

    expect(screen.getByText('Anterior ao início.')).toBeInTheDocument()
    expect(screen.getByTestId('rota')).toHaveTextContent(`${ROTA}`)
    expect(screen.getByTestId('rota')).not.toHaveTextContent('de=')
  })

  it('mostra uma mensagem quando nenhuma nota atende aos filtros', async () => {
    pesquisarMock.mockResolvedValue(pagina([], { total: 0 }))
    montar(`${ROTA}?busca=zzz`)

    expect(await screen.findByText('Nenhuma nota fiscal encontrada com os filtros')).toBeInTheDocument()
  })
})
