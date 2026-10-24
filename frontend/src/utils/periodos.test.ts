import { describe, expect, it } from 'vitest'
import { intervaloDoMes, rotuloDoMes } from './periodos'

describe('rotuloDoMes', () => {
  it('abrevia o mês em português com o ano de dois dígitos', () => {
    expect(rotuloDoMes('2026-10')).toBe('out/26')
    expect(rotuloDoMes('2025-01')).toBe('jan/25')
    expect(rotuloDoMes('2026-12')).toBe('dez/26')
  })
})

describe('intervaloDoMes', () => {
  it('vai do primeiro ao último dia do mês', () => {
    expect(intervaloDoMes('2026-10')).toEqual({ de: '2026-10-01', ate: '2026-10-31' })
    expect(intervaloDoMes('2026-09')).toEqual({ de: '2026-09-01', ate: '2026-09-30' })
  })

  it('respeita fevereiro e o ano bissexto', () => {
    expect(intervaloDoMes('2026-02').ate).toBe('2026-02-28')
    expect(intervaloDoMes('2028-02').ate).toBe('2028-02-29')
  })
})
