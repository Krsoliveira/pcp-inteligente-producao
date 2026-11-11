import { describe, expect, it } from 'vitest'
import { diferencaDaSoma, mensagemDaSoma } from './unidades'

const kg = (quantidade: number) => ({ quantidade, unidadeDeMedida: 'KG' })
const g = (quantidade: number) => ({ quantidade, unidadeDeMedida: 'G' })

describe('diferencaDaSoma', () => {
  it('soma que bate com a quantidade base não avisa (200 g + 600 g + 200 g para 1 kg)', () => {
    expect(diferencaDaSoma([g(200), g(600), g(200)], 1, 'KG')).toBeNull()
  })

  it('soma menor que a base avisa, na unidade do material produzido (950 g para 1 kg = 0,95 KG)', () => {
    expect(diferencaDaSoma([g(200), g(550), g(200)], 1, 'KG')).toEqual({ soma: 0.95, unidade: 'KG' })
  })

  it('soma maior que a base avisa (80 kg em cada um de 4 componentes para 80 kg)', () => {
    expect(diferencaDaSoma([kg(80), kg(80), kg(80), kg(80)], 80, 'KG')).toEqual({ soma: 320, unidade: 'KG' })
  })

  it('converte entre unidades da mesma grandeza, de peso e de volume', () => {
    expect(diferencaDaSoma([{ quantidade: 500, unidadeDeMedida: 'ML' }, { quantidade: 0.5, unidadeDeMedida: 'L' }], 1, 'L')).toBeNull()
    expect(diferencaDaSoma([{ quantidade: 1, unidadeDeMedida: 'T' }], 1000, 'KG')).toBeNull()
  })

  it('a unidade pode vir em minúsculas', () => {
    expect(diferencaDaSoma([{ quantidade: 200, unidadeDeMedida: 'g' }], 1, 'kg')).toEqual({ soma: 0.2, unidade: 'KG' })
  })

  it('grandezas diferentes entre componentes não somam: sem aviso', () => {
    expect(diferencaDaSoma([kg(1), { quantidade: 2, unidadeDeMedida: 'UN' }], 5, 'KG')).toBeNull()
    expect(diferencaDaSoma([kg(1), { quantidade: 2, unidadeDeMedida: 'L' }], 5, 'KG')).toBeNull()
  })

  it('material produzido em unidade que não é peso nem volume: sem aviso', () => {
    expect(diferencaDaSoma([kg(2)], 1, 'UN')).toBeNull()
  })

  it('componente incompleto (quantidade vazia ou zero) ou base inválida: sem aviso, para não avisar enquanto se digita', () => {
    expect(diferencaDaSoma([g(200), { quantidade: 0, unidadeDeMedida: 'G' }], 1, 'KG')).toBeNull()
    expect(diferencaDaSoma([g(200), { quantidade: Number.NaN, unidadeDeMedida: 'G' }], 1, 'KG')).toBeNull()
    expect(diferencaDaSoma([g(200)], 0, 'KG')).toBeNull()
    expect(diferencaDaSoma([], 1, 'KG')).toBeNull()
  })

  it('erro de arredondamento de ponto flutuante não vira aviso (0,1 + 0,2 + 0,7 para 1)', () => {
    expect(diferencaDaSoma([kg(0.1), kg(0.2), kg(0.7)], 1, 'KG')).toBeNull()
  })
})

describe('mensagemDaSoma', () => {
  it('mostra a soma e a base com a unidade, no padrão brasileiro', () => {
    expect(mensagemDaSoma({ soma: 0.95, unidade: 'KG' }, 1)).toBe('A soma dos componentes (0,95 KG) é diferente da quantidade base (1 KG).')
  })
})
