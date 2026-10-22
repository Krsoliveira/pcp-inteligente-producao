import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  codigoMaterialCorresponde,
  diasAte,
  formatarCodigoMaterial,
  formatarData,
  formatarQuantidade,
} from './formatacao'

describe('formatarData', () => {
  it('converte AAAA-MM-DD em DD/MM/AAAA', () => {
    expect(formatarData('2026-10-21')).toBe('21/10/2026')
  })

  it('devolve traço quando não há data', () => {
    expect(formatarData(null)).toBe('—')
    expect(formatarData(undefined)).toBe('—')
    expect(formatarData('')).toBe('—')
  })
})

describe('formatarCodigoMaterial', () => {
  it('aplica a máscara em códigos de 9 dígitos', () => {
    expect(formatarCodigoMaterial('103000001')).toBe('103.000.001')
  })

  it('não altera outros formatos', () => {
    expect(formatarCodigoMaterial('ABC-1')).toBe('ABC-1')
    expect(formatarCodigoMaterial('1030000')).toBe('1030000')
  })

  it('devolve traço quando não há código', () => {
    expect(formatarCodigoMaterial(null)).toBe('—')
  })
})

describe('codigoMaterialCorresponde', () => {
  it.each([
    ['103000001', '103.000.001'],
    ['103000001', '103000001'],
    ['103000001', '000001'],
    ['ABC-1', 'abc'],
  ])('%s corresponde a "%s"', (codigo, termo) => {
    expect(codigoMaterialCorresponde(codigo, termo)).toBe(true)
  })

  it('não corresponde a um trecho que não está no código', () => {
    expect(codigoMaterialCorresponde('103000001', '999')).toBe(false)
  })

  it('texto sem dígitos não casa com o código por acidente', () => {
    expect(codigoMaterialCorresponde('103000001', 'viga')).toBe(false)
  })
})

describe('formatarQuantidade', () => {
  it('formata no padrão brasileiro, com a unidade', () => {
    expect(formatarQuantidade(1234.5, 'kg')).toBe('1.234,5 kg')
  })

  it('limita a 4 casas decimais', () => {
    expect(formatarQuantidade(0.123456)).toBe('0,1235')
  })

  it('devolve traço quando não há valor', () => {
    expect(formatarQuantidade(null)).toBe('—')
  })
})

describe('diasAte', () => {
  afterEach(() => vi.useRealTimers())

  it('conta os dias corridos até a data, negativo se já passou', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 9, 21, 15, 30))

    expect(diasAte('2026-10-24')).toBe(3)
    expect(diasAte('2026-10-21')).toBe(0)
    expect(diasAte('2026-10-20')).toBe(-1)
  })
})
