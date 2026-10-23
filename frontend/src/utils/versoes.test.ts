import { describe, expect, it } from 'vitest'
import { sugerirProximaVersao } from './versoes'

describe('sugerirProximaVersao', () => {
  it('sugere a seguinte ao maior número', () => {
    expect(sugerirProximaVersao(['v1', 'v2'])).toBe('v3')
    expect(sugerirProximaVersao(['v1', 'v3', 'v2'])).toBe('v4')
  })

  it('não preenche os buracos: segue o maior número', () => {
    expect(sugerirProximaVersao(['v1', 'v5'])).toBe('v6')
  })

  it('ignora rótulos livres e aceita maiúscula e espaços nas pontas', () => {
    expect(sugerirProximaVersao(['v2-tolerancia-extra', 'V2 ', 'rev-a'])).toBe('v3')
  })

  it('sem nenhum rótulo no padrão devolve vazio para o usuário digitar', () => {
    expect(sugerirProximaVersao([])).toBe('')
    expect(sugerirProximaVersao(['rev-a', 'v2-extra'])).toBe('')
  })
})
