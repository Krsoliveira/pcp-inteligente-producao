import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { compararListasTecnicas, listarListasTecnicas } from '../api/listasTecnicas'
import { listarMateriais } from '../api/materiais'
import { erroHttp } from '../test/erros'
import { listaTecnica, material } from '../test/fabricas'
import { renderizarTela } from '../test/renderizar'
import type { ComparacaoDeVersoes } from '../types'
import { ListaTecnicaComparacaoPage } from './ListaTecnicaComparacaoPage'

vi.mock('../api/listasTecnicas', () => ({ compararListasTecnicas: vi.fn(), listarListasTecnicas: vi.fn() }))
vi.mock('../api/materiais', () => ({ listarMateriais: vi.fn() }))

const compararMock = vi.mocked(compararListasTecnicas)
const ROTA = '/listas-tecnicas/comparar'

const versaoA = { id: 'l-1', listaId: 'lista-1', materialId: 'm-1', versao: 'v1', quantidadeBase: 1, status: 'ATIVA' as const }
const versaoB = { id: 'l-2', listaId: 'lista-1', materialId: 'm-1', versao: 'v2', quantidadeBase: 1, status: 'EM_REVISAO' as const }

const comDiferencas: ComparacaoDeVersoes = {
  versaoA,
  versaoB,
  temDiferencas: true,
  alterados: [{ materialComponenteId: 'm-2', quantidadeA: 2.5, unidadeA: 'kg', quantidadeB: 3, unidadeB: 'kg' }],
  adicionados: [{ materialComponenteId: 'm-3', quantidadeA: null, unidadeA: null, quantidadeB: 8, unidadeB: 'un' }],
  removidos: [{ materialComponenteId: 'm-4', quantidadeA: 1, unidadeA: 'l', quantidadeB: null, unidadeB: null }],
  iguais: [{ materialComponenteId: 'm-5', quantidadeA: 4, unidadeA: 'un', quantidadeB: 4, unidadeB: 'un' }],
}

function montar(url = `${ROTA}?a=l-1&b=l-2`) {
  return renderizarTela(<ListaTecnicaComparacaoPage />, { rotas: [url], padrao: ROTA })
}

describe('ListaTecnicaComparacaoPage', () => {
  beforeEach(() => {
    compararMock.mockReset()
    compararMock.mockResolvedValue(comDiferencas)
    vi.mocked(listarListasTecnicas).mockResolvedValue([
      listaTecnica({ id: 'l-1', versao: 'v1' }), listaTecnica({ id: 'l-2', versao: 'v2' }), listaTecnica({ id: 'l-3', versao: 'v3' }),
    ])
    vi.mocked(listarMateriais).mockResolvedValue([
      material({ id: 'm-1', codigo: '103000001', descricao: 'Viga metálica' }),
      material({ id: 'm-2', codigo: '110000001', descricao: 'Aço inox', tipo: 'MATERIA_PRIMA', unidadeDeMedida: 'kg' }),
      material({ id: 'm-3', codigo: '110000002', descricao: 'Parafuso', tipo: 'MATERIA_PRIMA', unidadeDeMedida: 'un' }),
      material({ id: 'm-4', codigo: '110000003', descricao: 'Tinta', tipo: 'MATERIA_PRIMA', unidadeDeMedida: 'l' }),
      material({ id: 'm-5', codigo: '110000004', descricao: 'Arruela', tipo: 'MATERIA_PRIMA', unidadeDeMedida: 'un' }),
    ])
  })

  it('versões com a mesma base não mostram o aviso de bases diferentes', async () => {
    montar()
    await screen.findByText('103.000.001 — Viga metálica')

    expect(screen.queryByText(/usam bases diferentes/)).not.toBeInTheDocument()
  })

  it('versões com bases diferentes avisam que a comparação é por unidade produzida', async () => {
    compararMock.mockResolvedValue({ ...comDiferencas, versaoB: { ...versaoB, quantidadeBase: 100 } })
    montar()

    const aviso = await screen.findByText(/usam bases diferentes/)

    expect(aviso).toHaveTextContent('a v1 vale para 1 e a v2 para 100')
    expect(aviso).toHaveTextContent('por unidade produzida')
  })

  it('compara as versões que estão na URL e mostra o produto', async () => {
    montar()

    expect(await screen.findByText('103.000.001 — Viga metálica')).toBeInTheDocument()
    expect(compararMock).toHaveBeenCalledWith('l-1', 'l-2')
  })

  it('mostra cada componente com a situação e as quantidades das duas versões', async () => {
    montar()

    const linha = (descricao: string) => within(screen.getByText(descricao).closest('tr') as HTMLElement)
    await screen.findByText('Aço inox')

    expect(linha('Aço inox').getByText('Alterado')).toBeInTheDocument()
    expect(linha('Aço inox').getByText('2,5 kg')).toBeInTheDocument()
    expect(linha('Aço inox').getByText('3 kg')).toBeInTheDocument()
    expect(linha('Parafuso').getByText('Adicionado em B')).toBeInTheDocument()
    expect(linha('Parafuso').getByText('8 un')).toBeInTheDocument()
    expect(linha('Parafuso').getByText('—')).toBeInTheDocument()
    expect(linha('Tinta').getByText('Removido em B')).toBeInTheDocument()
    expect(linha('Arruela').getByText('Igual')).toBeInTheDocument()
  })

  it('resume quantos componentes mudaram', async () => {
    montar()

    expect(await screen.findByText('1 alterado')).toBeInTheDocument()
    expect(screen.getByText('1 adicionado')).toBeInTheDocument()
    expect(screen.getByText('1 removido')).toBeInTheDocument()
    expect(screen.getByText('1 igual')).toBeInTheDocument()
  })

  it('usa o plural quando há mais de um ou nenhum', async () => {
    compararMock.mockResolvedValue({
      ...comDiferencas,
      alterados: [comDiferencas.alterados[0], { ...comDiferencas.alterados[0], materialComponenteId: 'm-3' }],
      adicionados: [],
    })
    montar()

    expect(await screen.findByText('2 alterados')).toBeInTheDocument()
    expect(screen.getByText('0 adicionados')).toBeInTheDocument()
  })

  it('as diferenças vêm antes do que não mudou', async () => {
    montar()
    await screen.findByText('Aço inox')

    const situacoes = screen.getAllByRole('row').slice(1).map((r) => within(r).getAllByRole('cell')[0].textContent)

    expect(situacoes).toEqual(['Alterado', 'Adicionado em B', 'Removido em B', 'Igual'])
  })

  it('avisa quando as versões são idênticas', async () => {
    compararMock.mockResolvedValue({ ...comDiferencas, temDiferencas: false, alterados: [], adicionados: [], removidos: [] })
    montar()

    expect(await screen.findByText(/têm os mesmos componentes e quantidades/i)).toBeInTheDocument()
  })

  it('trocar a versão B atualiza a URL e compara de novo', async () => {
    montar()
    await screen.findByText('Aço inox')

    await userEvent.click(screen.getByRole('combobox', { name: 'Versão B' }))
    await userEvent.click(screen.getByRole('option', { name: 'v3' }))

    await waitFor(() => expect(screen.getByTestId('rota')).toHaveTextContent(`${ROTA}?a=l-1&b=l-3`))
    expect(compararMock).toHaveBeenLastCalledWith('l-1', 'l-3')
  })

  it('mostra o motivo quando o sistema recusa a comparação (listas diferentes)', async () => {
    compararMock.mockRejectedValue(erroHttp(422, { detail: 'Só é possível comparar versões da mesma lista técnica.' }))
    montar()

    expect(await screen.findByText('Só é possível comparar versões da mesma lista técnica.')).toBeInTheDocument()
  })

  it('sem versões na URL orienta o usuário e não consulta o sistema', async () => {
    montar(ROTA)

    expect(await screen.findByText(/use "Comparar" em uma das outras versões/i)).toBeInTheDocument()
    expect(compararMock).not.toHaveBeenCalled()
  })
})
