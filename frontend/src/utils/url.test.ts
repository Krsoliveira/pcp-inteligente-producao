import { describe, expect, it } from 'vitest'
import { montarBusca } from './url'

describe('montarBusca', () => {
  it('monta a query string com os valores informados', () => {
    expect(montarBusca({ busca: 'OP-1', status: 'LIBERADA' })).toBe('?busca=OP-1&status=LIBERADA')
  })

  it('ignora valores vazios, nulos e indefinidos', () => {
    expect(montarBusca({ busca: '', status: undefined, tipo: null, pagina: 2 })).toBe('?pagina=2')
  })

  it('devolve texto vazio quando não sobra nada', () => {
    expect(montarBusca({})).toBe('')
    expect(montarBusca({ busca: '' })).toBe('')
  })

  it('mantém o número zero', () => {
    expect(montarBusca({ pagina: 0 })).toBe('?pagina=0')
  })

  it('codifica caracteres especiais', () => {
    expect(montarBusca({ busca: 'a&b c' })).toBe('?busca=a%26b+c')
  })
})
