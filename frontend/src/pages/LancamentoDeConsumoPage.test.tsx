import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { lancarConsumo, listarConsumosPorOrdem } from '../api/consumos'
import { listarLotesDisponiveis } from '../api/lotes'
import { listarMateriais } from '../api/materiais'
import { buscarOrdemPorId } from '../api/ordens'
import { listarTiposOrdem } from '../api/tiposOrdem'
import { erroHttp } from '../test/erros'
import { TODAS_AS_PERMISSOES, lote, material, ordem } from '../test/fabricas'
import { renderizarTela } from '../test/renderizar'
import type { ConsumoMaterial, Permissao } from '../types'
import { LancamentoDeConsumoPage } from './LancamentoDeConsumoPage'

vi.mock('../api/consumos', () => ({ lancarConsumo: vi.fn(), listarConsumosPorOrdem: vi.fn() }))
vi.mock('../api/lotes', () => ({ listarLotesDisponiveis: vi.fn() }))
vi.mock('../api/materiais', () => ({ listarMateriais: vi.fn() }))
vi.mock('../api/ordens', () => ({ buscarOrdemPorId: vi.fn() }))
vi.mock('../api/tiposOrdem', () => ({ listarTiposOrdem: vi.fn() }))

const ROTA = '/ordens/o-1/lancar-consumo'

const consumo = (extras: Partial<ConsumoMaterial>): ConsumoMaterial => ({
  id: 'c-aco', ordemProducaoId: 'o-1', materialId: 'm-aco', quantidadePlanejada: 25, quantidadeConsumida: null, desvio: null,
  unidadeDeMedida: 'kg', registrado: false, justificado: false, naoPlanejado: false, justificativa: null, justificadoPor: null,
  justificadoEm: null, criadoEm: '', criadoPor: '', atualizadoEm: '', atualizadoPor: '', ...extras,
})

const MATERIAIS = [
  material({ id: 'm-1', codigo: '103000001', descricao: 'Viga', tipo: 'PRODUTO_ACABADO' }),
  material({ id: 'm-aco', codigo: '110000001', descricao: 'Aço carbono', tipo: 'MATERIA_PRIMA', unidadeDeMedida: 'kg' }),
  material({ id: 'm-par', codigo: '110000007', descricao: 'Parafuso M8', tipo: 'MATERIA_PRIMA', unidadeDeMedida: 'un' }),
  material({ id: 'm-cola', codigo: '110000005', descricao: 'Cola', tipo: 'MATERIA_PRIMA', unidadeDeMedida: 'kg' }),
  material({ id: 'm-sa', codigo: '105000001', descricao: 'Chapa cortada', tipo: 'SEMIACABADO', unidadeDeMedida: 'un' }),
  material({ id: 'm-pa', codigo: '103000009', descricao: 'Peça pronta', tipo: 'PRODUTO_ACABADO', unidadeDeMedida: 'un' }),
]

function montar(permissoes: Permissao[] = TODAS_AS_PERMISSOES) {
  return renderizarTela(<LancamentoDeConsumoPage />, { rotas: [ROTA], padrao: '/ordens/:id/lancar-consumo', permissoes })
}

describe('LancamentoDeConsumoPage', () => {
  beforeEach(() => {
    vi.mocked(buscarOrdemPorId).mockReset().mockResolvedValue(ordem({ id: 'o-1', codigo: 'OP-1', materialId: 'm-1', status: 'EM_PRODUCAO' }))
    vi.mocked(listarConsumosPorOrdem).mockReset().mockResolvedValue([
      consumo({ id: 'c-aco', materialId: 'm-aco', quantidadePlanejada: 25 }),
      consumo({ id: 'c-par', materialId: 'm-par', quantidadePlanejada: 40, unidadeDeMedida: 'un' }),
    ])
    vi.mocked(listarMateriais).mockReset().mockResolvedValue(MATERIAIS)
    vi.mocked(listarTiposOrdem).mockReset().mockResolvedValue([])
    vi.mocked(lancarConsumo).mockReset().mockResolvedValue([])
    vi.mocked(listarLotesDisponiveis).mockReset().mockImplementation(async (materialId) => [
      lote({ id: `l-${materialId}-1`, numeroLote: `L1-${materialId}`, materialId, saldo: 20, dataValidade: '2027-01-01' }),
      lote({ id: `l-${materialId}-2`, numeroLote: `L2-${materialId}`, materialId, saldo: 500, dataValidade: '2028-01-01' }),
    ])
  })

  it('lista as linhas pendentes da lista técnica com o planejado e os campos de consumo', async () => {
    montar()

    expect(await screen.findByText('110.000.001')).toBeInTheDocument()
    expect(screen.getByText('Aço carbono')).toBeInTheDocument()
    expect(screen.getByText('25 kg')).toBeInTheDocument()
    expect(screen.getByLabelText('Quantidade consumida de 110.000.007')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Lançar consumo' })).toBeInTheDocument()
  })

  it('ao digitar a quantidade sugere os lotes pela regra FEFO (vence primeiro, sai primeiro)', async () => {
    montar()

    await userEvent.type(await screen.findByLabelText('Quantidade consumida de 110.000.001'), '25')

    await waitFor(() => expect(screen.getByLabelText('Quantidade do lote L1-m-aco')).toHaveValue('20'))
    expect(screen.getByLabelText('Quantidade do lote L2-m-aco')).toHaveValue('5')
    expect(screen.getByText(/Alocado 25 de 25 kg/)).toBeInTheDocument()
  })

  it('lança só as linhas preenchidas; a em branco fica de fora, e depois volta à ordem', async () => {
    montar()

    await userEvent.type(await screen.findByLabelText('Quantidade consumida de 110.000.001'), '25')
    await screen.findByLabelText('Quantidade do lote L1-m-aco')
    await userEvent.click(screen.getByRole('button', { name: 'Lançar consumo' }))

    await waitFor(() => expect(lancarConsumo).toHaveBeenCalled())
    expect(vi.mocked(lancarConsumo).mock.calls[0]).toEqual(['o-1', {
      consumos: [{
        consumoId: 'c-aco', quantidadeConsumida: 25, justificativa: undefined,
        alocacoes: [{ loteId: 'l-m-aco-1', quantidade: 20 }, { loteId: 'l-m-aco-2', quantidade: 5 }],
      }],
      itensAdicionais: [],
    }])
    await waitFor(() => expect(screen.getByTestId('rota')).toHaveTextContent('/ordens/o-1'))
    expect(screen.getByTestId('rota')).not.toHaveTextContent('lancar-consumo')
  })

  it('desvio mostra o campo da justificativa e exige que seja preenchido antes de enviar', async () => {
    montar()
    await userEvent.type(await screen.findByLabelText('Quantidade consumida de 110.000.001'), '26')

    expect(await screen.findByText('Desvio: +1 kg')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Lançar consumo' }))

    expect(await screen.findByText('110.000.001: há desvio, informe a justificativa.')).toBeInTheDocument()
    expect(lancarConsumo).not.toHaveBeenCalled()

    await userEvent.type(screen.getByLabelText('Justificativa do desvio de 110.000.001'), 'Perda no corte')
    await userEvent.click(screen.getByRole('button', { name: 'Lançar consumo' }))

    await waitFor(() => expect(lancarConsumo).toHaveBeenCalled())
    expect(vi.mocked(lancarConsumo).mock.calls[0][1].consumos[0]).toMatchObject({ quantidadeConsumida: 26, justificativa: 'Perda no corte' })
  })

  it('o aviso de erro some quando o usuário volta a editar a linha', async () => {
    montar()
    await userEvent.type(await screen.findByLabelText('Quantidade consumida de 110.000.001'), '26')
    await userEvent.click(screen.getByRole('button', { name: 'Lançar consumo' }))
    expect(await screen.findByText('110.000.001: há desvio, informe a justificativa.')).toBeInTheDocument()

    await userEvent.type(screen.getByLabelText('Justificativa do desvio de 110.000.001'), 'P')

    expect(screen.queryByText('110.000.001: há desvio, informe a justificativa.')).not.toBeInTheDocument()
  })

  it('sem nada preenchido não envia e pede ao menos uma linha', async () => {
    montar()
    await screen.findByText('110.000.001')

    await userEvent.click(screen.getByRole('button', { name: 'Lançar consumo' }))

    expect(await screen.findByText('Informe o consumo de ao menos uma linha.')).toBeInTheDocument()
    expect(lancarConsumo).not.toHaveBeenCalled()
  })

  it('se o servidor recusa, mostra o motivo e fica na tela com o que foi digitado (nada foi lançado)', async () => {
    vi.mocked(lancarConsumo).mockRejectedValue(erroHttp(422, { detail: 'Material 110000005: O lote LOTE-X não tem saldo suficiente.' }))
    montar()
    await userEvent.type(await screen.findByLabelText('Quantidade consumida de 110.000.001'), '25')
    await screen.findByLabelText('Quantidade do lote L1-m-aco')

    await userEvent.click(screen.getByRole('button', { name: 'Lançar consumo' }))

    expect(await screen.findByText('Material 110000005: O lote LOTE-X não tem saldo suficiente.')).toBeInTheDocument()
    expect(screen.getByTestId('rota')).toHaveTextContent(ROTA)
    expect(screen.getByLabelText('Quantidade consumida de 110.000.001')).toHaveValue('25')
  })

  describe('adicionar item fora da lista', () => {
    it('com a permissão, adiciona a linha, escolhe o material, informa quantidade e justificativa e envia junto', async () => {
      montar()
      await userEvent.click(await screen.findByRole('button', { name: 'Adicionar item fora da lista' }))

      await userEvent.click(screen.getByRole('combobox', { name: /Material fora da lista/ }))
      await userEvent.click(await screen.findByRole('option', { name: /Cola/ }))
      await userEvent.type(screen.getByLabelText('Quantidade consumida do item adicionado'), '4')
      await userEvent.type(screen.getByLabelText('Justificativa do item adicionado'), 'A lista não previa a cola')
      await screen.findByLabelText('Quantidade do lote L1-m-cola')
      await userEvent.click(screen.getByRole('button', { name: 'Lançar consumo' }))

      await waitFor(() => expect(lancarConsumo).toHaveBeenCalled())
      expect(vi.mocked(lancarConsumo).mock.calls[0][1]).toEqual({
        consumos: [],
        itensAdicionais: [{
          materialId: 'm-cola', quantidadeConsumida: 4, justificativa: 'A lista não previa a cola',
          alocacoes: [{ loteId: 'l-m-cola-1', quantidade: 4 }],
        }],
      })
    })

    it('não oferece o que já está na ordem, o que a ordem produz, nem produto acabado (ordem comum)', async () => {
      montar()
      await userEvent.click(await screen.findByRole('button', { name: 'Adicionar item fora da lista' }))

      await userEvent.click(screen.getByRole('combobox', { name: /Material fora da lista/ }))
      const opcoes = screen.getAllByRole('option').map((o) => o.textContent ?? '')

      expect(opcoes.join(' | ')).toContain('Cola')
      expect(opcoes.join(' | ')).toContain('Chapa cortada') // semiacabado pode
      expect(opcoes.join(' | ')).not.toContain('Aço carbono') // já está na ordem
      expect(opcoes.join(' | ')).not.toContain('Viga') // é o que a ordem produz
      expect(opcoes.join(' | ')).not.toContain('Peça pronta') // acabado só na Conversão
    })

    it('em ordem do tipo Conversão oferece também o produto acabado', async () => {
      vi.mocked(buscarOrdemPorId).mockResolvedValue(ordem({ id: 'o-1', codigo: 'OP-1', materialId: 'm-sa', tipoOrdemId: 't-conv', status: 'EM_PRODUCAO' }))
      vi.mocked(listarTiposOrdem).mockResolvedValue([{ id: 't-conv', nome: 'Conversão', conversao: true }] as never)
      montar()
      await userEvent.click(await screen.findByRole('button', { name: 'Adicionar item fora da lista' }))

      await userEvent.click(screen.getByRole('combobox', { name: /Material fora da lista/ }))
      await waitFor(() => expect(screen.getAllByRole('option').map((o) => o.textContent).join(' | ')).toContain('Peça pronta'))
    })

    it('item adicionado sem justificativa não é enviado', async () => {
      montar()
      await userEvent.click(await screen.findByRole('button', { name: 'Adicionar item fora da lista' }))
      await userEvent.click(screen.getByRole('combobox', { name: /Material fora da lista/ }))
      await userEvent.click(await screen.findByRole('option', { name: /Cola/ }))
      await userEvent.type(screen.getByLabelText('Quantidade consumida do item adicionado'), '4')
      await screen.findByLabelText('Quantidade do lote L1-m-cola')

      await userEvent.click(screen.getByRole('button', { name: 'Lançar consumo' }))

      expect(await screen.findByText(/item fora da lista exige justificativa/)).toBeInTheDocument()
      expect(lancarConsumo).not.toHaveBeenCalled()
    })

    it('é possível remover o item adicionado', async () => {
      montar()
      await userEvent.click(await screen.findByRole('button', { name: 'Adicionar item fora da lista' }))

      await userEvent.click(screen.getByRole('button', { name: 'Remover item adicionado' }))

      expect(screen.queryByRole('combobox', { name: /Material fora da lista/ })).not.toBeInTheDocument()
    })

    it('sem CONSUMO_ADICIONAR_ITEM o botão de adicionar não aparece', async () => {
      montar(TODAS_AS_PERMISSOES.filter((p) => p !== 'CONSUMO_ADICIONAR_ITEM'))
      await screen.findByText('110.000.001')

      expect(screen.queryByRole('button', { name: 'Adicionar item fora da lista' })).not.toBeInTheDocument()
    })
  })

  it('ordem que não está em produção explica e não mostra a tabela de lançamento', async () => {
    vi.mocked(buscarOrdemPorId).mockResolvedValue(ordem({ id: 'o-1', codigo: 'OP-1', status: 'PLANEJADA' }))
    montar()

    expect(await screen.findByText('O consumo só pode ser lançado em ordens em produção.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Lançar consumo' })).not.toBeInTheDocument()
  })

  it('as linhas já lançadas aparecem só como resumo, não como campos', async () => {
    vi.mocked(listarConsumosPorOrdem).mockResolvedValue([
      consumo({ id: 'c-aco', materialId: 'm-aco', registrado: true, quantidadeConsumida: 25 }),
      consumo({ id: 'c-par', materialId: 'm-par', quantidadePlanejada: 40, unidadeDeMedida: 'un' }),
    ])
    montar()

    expect(await screen.findByLabelText('Quantidade consumida de 110.000.007')).toBeInTheDocument()
    expect(screen.queryByLabelText('Quantidade consumida de 110.000.001')).not.toBeInTheDocument()
    expect(screen.getByText(/Já lançados \(1\): 110\.000\.001/)).toBeInTheDocument()
  })

  it('com tudo já lançado avisa em vez de mostrar campos', async () => {
    vi.mocked(listarConsumosPorOrdem).mockResolvedValue([consumo({ id: 'c-aco', materialId: 'm-aco', registrado: true, quantidadeConsumida: 25 })])
    montar()

    expect(await screen.findByText('Todas as linhas da lista já foram lançadas.')).toBeInTheDocument()
    expect(within(screen.getByRole('table')).queryByRole('textbox')).not.toBeInTheDocument()
  })
})
