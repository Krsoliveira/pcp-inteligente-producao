import { describe, expect, it } from 'vitest'
import type { ConsumoMaterial } from '../../types'
import { RASCUNHO_VAZIO, lerQuantidadeOpcional, montarLancamento, type ItemAdicionado, type Rascunho } from './lancamento'

const consumo = (id: string, materialId: string, planejado: number): ConsumoMaterial => ({
  id, ordemProducaoId: 'o-1', materialId, quantidadePlanejada: planejado, quantidadeConsumida: null, desvio: null,
  unidadeDeMedida: 'kg', registrado: false, justificado: false, naoPlanejado: false, justificativa: null, justificadoPor: null,
  justificadoEm: null, criadoEm: '', criadoPor: '', atualizadoEm: '', atualizadoPor: '',
})

const ACO = consumo('c-aco', 'm-aco', 25)
const PARAFUSO = consumo('c-par', 'm-par', 40)
const rotulo = (id: string) => `Mat-${id}`

const rascunho = (qtd: string, alocado: Record<string, string>, justificativa = ''): Rascunho => ({ qtd, justificativa, alocado })
const item = (extras: Partial<ItemAdicionado>): ItemAdicionado => ({
  chave: 'novo-1', materialId: 'm-cola', qtd: '4', justificativa: 'Faltou na lista', alocado: { 'l-cola': '4' }, ...extras,
})

describe('lerQuantidadeOpcional', () => {
  it('vazio é "não informado"; número com vírgula ou ponto vale; inválido e negativo viram NaN', () => {
    expect(lerQuantidadeOpcional('')).toBeNull()
    expect(lerQuantidadeOpcional('  ')).toBeNull()
    expect(lerQuantidadeOpcional('12,5')).toBe(12.5)
    expect(lerQuantidadeOpcional('0')).toBe(0)
    expect(lerQuantidadeOpcional('abc')).toBeNaN()
    expect(lerQuantidadeOpcional('-3')).toBeNaN()
  })
})

describe('montarLancamento', () => {
  it('só entram as linhas preenchidas: a que ficou em branco não é lançada', () => {
    const r = montarLancamento([ACO, PARAFUSO], { 'c-aco': rascunho('25', { 'l-aco': '25' }) }, [], rotulo)

    expect(r).toEqual({ corpo: {
      consumos: [{ consumoId: 'c-aco', quantidadeConsumida: 25, justificativa: undefined, alocacoes: [{ loteId: 'l-aco', quantidade: 25 }] }],
      itensAdicionais: [],
    } })
  })

  it('sem nada preenchido pede ao menos uma linha', () => {
    expect(montarLancamento([ACO], {}, [], rotulo)).toEqual({ erro: 'Informe o consumo de ao menos uma linha.' })
  })

  it('desvio exige justificativa e a mensagem diz o material', () => {
    const r = montarLancamento([ACO], { 'c-aco': rascunho('26', { 'l-aco': '26' }) }, [], rotulo)

    expect(r).toEqual({ erro: 'Mat-m-aco: há desvio, informe a justificativa.' })
  })

  it('com justificativa o desvio passa, e a justificativa vai aparada', () => {
    const r = montarLancamento([ACO], { 'c-aco': rascunho('26', { 'l-aco': '26' }, '  Perda no corte ') }, [], rotulo)

    expect(r).toMatchObject({ corpo: { consumos: [{ justificativa: 'Perda no corte' }] } })
  })

  it('a soma dos lotes tem que bater com a quantidade (sem erro de ponto flutuante)', () => {
    expect(montarLancamento([ACO], { 'c-aco': rascunho('25', { 'l-1': '10', 'l-2': '14,9' }) }, [], rotulo))
      .toMatchObject({ erro: expect.stringContaining('a soma dos lotes') })
    expect(montarLancamento([ACO], { 'c-aco': rascunho('25', { 'l-1': '10,1', 'l-2': '14,9' }) }, [], rotulo))
      .toMatchObject({ corpo: { consumos: [{ alocacoes: [{ loteId: 'l-1', quantidade: 10.1 }, { loteId: 'l-2', quantidade: 14.9 }] }] } })
  })

  it('consumo zero não leva lotes, mas conta como desvio (exige justificativa)', () => {
    expect(montarLancamento([ACO], { 'c-aco': rascunho('0', { 'l-aco': '5' }) }, [], rotulo)).toMatchObject({ erro: expect.stringContaining('justificativa') })
    expect(montarLancamento([ACO], { 'c-aco': rascunho('0', { 'l-aco': '5' }, 'Não usou') }, [], rotulo))
      .toMatchObject({ corpo: { consumos: [{ quantidadeConsumida: 0, alocacoes: [] }] } })
  })

  it('quantidade inválida diz o material', () => {
    expect(montarLancamento([ACO], { 'c-aco': rascunho('abc', {}) }, [], rotulo)).toEqual({ erro: 'Mat-m-aco: informe uma quantidade válida.' })
  })

  it('item adicionado: material, quantidade positiva, justificativa e lotes são obrigatórios', () => {
    expect(montarLancamento([], {}, [item({ materialId: '' })], rotulo)).toEqual({ erro: 'Escolha o material de cada item adicionado.' })
    expect(montarLancamento([], {}, [item({ qtd: '' })], rotulo)).toMatchObject({ erro: expect.stringContaining('quantidade consumida') })
    expect(montarLancamento([], {}, [item({ qtd: '0' })], rotulo)).toMatchObject({ erro: expect.stringContaining('quantidade consumida') })
    expect(montarLancamento([], {}, [item({ justificativa: ' ' })], rotulo)).toMatchObject({ erro: expect.stringContaining('exige justificativa') })
    expect(montarLancamento([], {}, [item({ alocado: {} })], rotulo)).toMatchObject({ erro: expect.stringContaining('a soma dos lotes') })
  })

  it('junta linhas da lista e itens adicionados no mesmo corpo', () => {
    const r = montarLancamento([ACO], { 'c-aco': rascunho('25', { 'l-aco': '25' }) }, [item({})], rotulo)

    expect(r).toMatchObject({ corpo: {
      consumos: [{ consumoId: 'c-aco' }],
      itensAdicionais: [{ materialId: 'm-cola', quantidadeConsumida: 4, justificativa: 'Faltou na lista', alocacoes: [{ loteId: 'l-cola', quantidade: 4 }] }],
    } })
  })

  it('linha sem rascunho é tratada como em branco', () => {
    expect(montarLancamento([ACO], { 'c-aco': RASCUNHO_VAZIO }, [item({})], rotulo)).toMatchObject({ corpo: { consumos: [] } })
  })
})
