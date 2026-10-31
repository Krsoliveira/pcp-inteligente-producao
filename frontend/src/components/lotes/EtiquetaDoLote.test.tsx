import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { buscarOrdemPorId } from '../../api/ordens'
import { lote, material, ordem } from '../../test/fabricas'
import { renderizarTela } from '../../test/renderizar'
import type { Lote } from '../../types'
import { EtiquetaDoLoteDialog } from './EtiquetaDoLote'

vi.mock('../../api/ordens', () => ({ buscarOrdemPorId: vi.fn() }))
// Sem canvas/SVG medido no jsdom: o QR é trocado por um texto com o que ele carregaria.
vi.mock('qrcode.react', () => ({
  QRCodeSVG: ({ value, title }: { value: string; title: string }) => <div data-testid="qrcode" title={title}>{value}</div>,
}))

const aco = material({ id: 'm-2', codigo: '110000003', descricao: 'Lingote de alumínio A380', tipo: 'MATERIA_PRIMA', unidadeDeMedida: 'kg' })

function montar(dados: Partial<Lote> = {}, onFechar = vi.fn()) {
  renderizarTela(<EtiquetaDoLoteDialog lote={lote({ id: 'lt-9', numeroLote: '2610030001', ...dados })} material={aco} aberto onFechar={onFechar} />)
  return onFechar
}

/** A prévia fica no diálogo; a cópia para a impressora fica fora dele, escondida na tela. */
const previa = () => within(screen.getByRole('dialog'))

describe('EtiquetaDoLoteDialog', () => {
  beforeEach(() => vi.mocked(buscarOrdemPorId).mockReset())

  it('o QR code leva à tela do lote no próprio sistema', () => {
    montar()

    expect(previa().getByTestId('qrcode')).toHaveTextContent(`${window.location.origin}/lotes/lt-9`)
    expect(previa().getByTestId('qrcode')).toHaveAttribute('title', 'QR code do lote 2610030001')
  })

  it('mostra os dados do lote para leitura sem leitor: número, material, quantidade e datas', () => {
    montar()

    expect(previa().getByText('2610030001')).toBeInTheDocument()
    expect(previa().getByText('110.000.003')).toBeInTheDocument()
    expect(previa().getByText('Lingote de alumínio A380')).toBeInTheDocument()
    expect(previa().getByText('250 kg')).toBeInTheDocument()
    expect(previa().getByText(/Fab\.: 01\/08\/2026/)).toBeInTheDocument()
    expect(previa().getByText('01/08/2031')).toBeInTheDocument()
  })

  it('lote de compra mostra a nota fiscal e o fornecedor, cada um na sua linha', () => {
    montar()

    expect(previa().getByText('NF NF-001')).toBeInTheDocument()
    expect(previa().getByText('Aços Brasil')).toBeInTheDocument()
    expect(buscarOrdemPorId).not.toHaveBeenCalled()
  })

  it('lote de compra sem nota fiscal informada não quebra a etiqueta', () => {
    montar({ notaFiscal: null, notaFiscalId: null, fornecedor: null })

    expect(previa().getByText('Lote de compra')).toBeInTheDocument()
  })

  it('lote de produção mostra a ordem que o gerou', async () => {
    vi.mocked(buscarOrdemPorId).mockResolvedValue(ordem({ id: 'o-7', codigo: 'OP-2026-0304' }))
    montar({ origem: 'PRODUCAO', ordemProducaoId: 'o-7', fornecedor: null, notaFiscal: null, notaFiscalId: null })

    expect(await previa().findByText('Ordem OP-2026-0304')).toBeInTheDocument()
    expect(buscarOrdemPorId).toHaveBeenCalledWith('o-7')
  })

  it('Imprimir abre o diálogo de impressão do navegador com a etiqueta pronta fora da tela', async () => {
    const imprimir = vi.spyOn(window, 'print').mockImplementation(() => {})
    montar()

    await userEvent.click(screen.getByRole('button', { name: 'Imprimir' }))

    expect(imprimir).toHaveBeenCalledTimes(1)
    const areaDeImpressao = document.body.querySelector(':scope > .etiqueta-area-de-impressao')
    expect(areaDeImpressao).not.toBeNull()
    expect(areaDeImpressao).toHaveTextContent('2610030001')
    imprimir.mockRestore()
  })

  it('Fechar fecha o diálogo', async () => {
    const onFechar = montar()

    await userEvent.click(screen.getByRole('button', { name: 'Fechar' }))

    await waitFor(() => expect(onFechar).toHaveBeenCalled())
  })
})
