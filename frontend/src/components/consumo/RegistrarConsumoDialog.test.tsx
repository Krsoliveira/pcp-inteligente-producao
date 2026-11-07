import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { registrarConsumo } from '../../api/consumos'
import { listarLotesDisponiveis } from '../../api/lotes'
import { lote, material } from '../../test/fabricas'
import { renderizarTela } from '../../test/renderizar'
import { erroHttp } from '../../test/erros'
import type { ConsumoMaterial } from '../../types'
import { RegistrarConsumoDialog } from './RegistrarConsumoDialog'

vi.mock('../../api/consumos', () => ({ registrarConsumo: vi.fn() }))
vi.mock('../../api/lotes', () => ({ listarLotesDisponiveis: vi.fn() }))

const CONSUMO: ConsumoMaterial = {
  id: 'c-1', ordemProducaoId: 'o-1', materialId: 'm-aco', quantidadePlanejada: 25, quantidadeConsumida: null, desvio: null,
  unidadeDeMedida: 'kg', registrado: false, justificado: false, naoPlanejado: false, justificativa: null, justificadoPor: null,
  justificadoEm: null, criadoEm: '', criadoPor: '', atualizadoEm: '', atualizadoPor: '',
}

const onFechar = vi.fn()
const onSucesso = vi.fn()

function montar() {
  return renderizarTela(
    <RegistrarConsumoDialog consumo={CONSUMO} material={material({ id: 'm-aco', codigo: '110000001' })} ordemId="o-1"
      onFechar={onFechar} onSucesso={onSucesso} />,
  )
}

async function informarQuantidade(texto: string) {
  const usuario = userEvent.setup()
  await usuario.type(screen.getByLabelText('Quantidade consumida *'), texto)
  await screen.findByLabelText('Quantidade do lote L1')
  return usuario
}

describe('RegistrarConsumoDialog', () => {
  beforeEach(() => {
    onFechar.mockReset()
    onSucesso.mockReset()
    vi.mocked(registrarConsumo).mockReset().mockResolvedValue(CONSUMO)
    vi.mocked(listarLotesDisponiveis).mockReset().mockResolvedValue([
      lote({ id: 'l-1', numeroLote: 'L1', materialId: 'm-aco', saldo: 20, dataValidade: '2027-01-01', origem: 'COMPRA', notaFiscal: 'NF-77' }),
      lote({ id: 'l-2', numeroLote: 'L2', materialId: 'm-aco', saldo: 500, dataValidade: '2028-01-01', origem: 'PRODUCAO' }),
    ])
  })

  it('sugere os lotes pela regra FEFO, mostra a origem e registra com a distribuição sugerida', async () => {
    montar()
    const usuario = await informarQuantidade('25')

    await waitFor(() => expect(screen.getByLabelText('Quantidade do lote L1')).toHaveValue('20'))
    expect(screen.getByLabelText('Quantidade do lote L2')).toHaveValue('5')
    expect(screen.getByText(/Alocado 25 de 25 kg/)).toBeInTheDocument()
    expect(screen.getByText('NF NF-77')).toBeInTheDocument()
    expect(screen.getByText('Produção')).toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: 'Registrar' }))

    await waitFor(() => expect(registrarConsumo).toHaveBeenCalledWith('o-1', 'c-1', {
      quantidadeConsumida: 25,
      justificativa: undefined,
      alocacoes: [{ loteId: 'l-1', quantidade: 20 }, { loteId: 'l-2', quantidade: 5 }],
    }))
    await waitFor(() => expect(onSucesso).toHaveBeenCalled())
  })

  it('recusa quando a soma dos lotes ajustados à mão não bate com a quantidade, e "Distribuir (FEFO)" refaz a sugestão', async () => {
    montar()
    const usuario = await informarQuantidade('25')
    await waitFor(() => expect(screen.getByLabelText('Quantidade do lote L2')).toHaveValue('5'))

    await usuario.clear(screen.getByLabelText('Quantidade do lote L2'))
    await usuario.click(screen.getByRole('button', { name: 'Registrar' }))

    expect(await screen.findByText('A soma dos lotes deve ser igual à quantidade consumida.')).toBeInTheDocument()
    expect(registrarConsumo).not.toHaveBeenCalled()

    await usuario.click(screen.getByRole('button', { name: 'Distribuir (FEFO)' }))
    await waitFor(() => expect(screen.getByLabelText('Quantidade do lote L2')).toHaveValue('5'))
  })

  it('recusa quantidade acima do saldo de um lote', async () => {
    montar()
    const usuario = await informarQuantidade('25')
    await waitFor(() => expect(screen.getByLabelText('Quantidade do lote L1')).toHaveValue('20'))

    await usuario.clear(screen.getByLabelText('Quantidade do lote L1'))
    await usuario.type(screen.getByLabelText('Quantidade do lote L1'), '21')
    await usuario.click(screen.getByRole('button', { name: 'Registrar' }))

    expect(await screen.findByText('O lote L1 não tem esse saldo.')).toBeInTheDocument()
    expect(registrarConsumo).not.toHaveBeenCalled()
  })

  it('com desvio, exige a justificativa', async () => {
    montar()
    const usuario = await informarQuantidade('26')
    await usuario.click(screen.getByRole('button', { name: 'Registrar' }))

    expect(await screen.findByText('Há desvio: informe a justificativa.')).toBeInTheDocument()
    expect(registrarConsumo).not.toHaveBeenCalled()
  })

  it('recusado pelo servidor, mostra o motivo e busca de novo o saldo dos lotes, que pode ter mudado', async () => {
    vi.mocked(registrarConsumo).mockRejectedValue(erroHttp(422, { detail: 'Só é possível registrar o consumo de uma ordem em produção.' }))
    montar()
    const usuario = await informarQuantidade('25')
    const consultasAntes = vi.mocked(listarLotesDisponiveis).mock.calls.length

    await usuario.click(screen.getByRole('button', { name: 'Registrar' }))

    expect(await screen.findByText('Só é possível registrar o consumo de uma ordem em produção.')).toBeInTheDocument()
    await waitFor(() => expect(vi.mocked(listarLotesDisponiveis).mock.calls.length).toBeGreaterThan(consultasAntes))
    expect(onSucesso).not.toHaveBeenCalled()
  })

  it('consumo zero registra sem lotes e não pede distribuição', async () => {
    vi.mocked(listarLotesDisponiveis).mockResolvedValue([])
    montar()
    const usuario = userEvent.setup()
    await usuario.type(screen.getByLabelText('Quantidade consumida *'), '0')
    await usuario.type(screen.getByLabelText('Justificativa do desvio *'), 'Não foi usado')

    expect(screen.queryByText('De quais lotes saiu?')).not.toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Registrar' }))

    await waitFor(() => expect(registrarConsumo).toHaveBeenCalledWith('o-1', 'c-1', {
      quantidadeConsumida: 0, justificativa: 'Não foi usado', alocacoes: [],
    }))
  })

  it('sem lote disponível, avisa e orienta o que fazer', async () => {
    vi.mocked(listarLotesDisponiveis).mockResolvedValue([])
    montar()
    await userEvent.setup().type(screen.getByLabelText('Quantidade consumida *'), '25')

    expect(await screen.findByText(/Não há lote disponível deste material/)).toBeInTheDocument()
    expect(screen.getByText(/Registre uma entrada de material/)).toBeInTheDocument()
  })

  it('mostra a mensagem da API quando o registro é recusado', async () => {
    vi.mocked(registrarConsumo).mockRejectedValue(new Error('falha'))
    montar()
    const usuario = await informarQuantidade('25')
    await waitFor(() => expect(screen.getByLabelText('Quantidade do lote L2')).toHaveValue('5'))
    await usuario.click(screen.getByRole('button', { name: 'Registrar' }))

    expect(await screen.findByRole('alert')).toBeInTheDocument()
    expect(onSucesso).not.toHaveBeenCalled()
  })
})
