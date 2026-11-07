import { useLayoutEffect, useState } from 'react'
import { act, renderHook } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { useSincronizarAoMudar } from './useSincronizarAoMudar'

/** Um rascunho que acompanha o valor aplicado, como nas telas de resultados. Anota o que chega à tela, a cada commit. */
function useRascunho(aplicado: string, naTela: string[]) {
  const [rascunho, setRascunho] = useState('inicial')
  useSincronizarAoMudar([aplicado], () => setRascunho(aplicado))
  useLayoutEffect(() => { naTela.push(rascunho) })
  return { rascunho, setRascunho }
}

describe('useSincronizarAoMudar', () => {
  it('sincroniza já na primeira renderização', () => {
    const sincronizar = vi.fn()

    renderHook(() => useSincronizarAoMudar(['a'], sincronizar))

    expect(sincronizar).toHaveBeenCalledTimes(1)
  })

  it('não sincroniza de novo enquanto os valores forem os mesmos, mesmo em array novo', () => {
    const sincronizar = vi.fn()

    const { rerender } = renderHook(({ valor }) => useSincronizarAoMudar([valor, 1], sincronizar), { initialProps: { valor: 'a' } })
    rerender({ valor: 'a' })

    expect(sincronizar).toHaveBeenCalledTimes(1)
  })

  it('sincroniza quando qualquer um dos valores muda', () => {
    const sincronizar = vi.fn()

    const { rerender } = renderHook(({ a, b }) => useSincronizarAoMudar([a, b], sincronizar), { initialProps: { a: 'a', b: 'b' } })
    rerender({ a: 'a', b: 'outro' })

    expect(sincronizar).toHaveBeenCalledTimes(2)
  })

  it('não desfaz o que o usuário digitou enquanto o valor aplicado não muda', () => {
    const { result, rerender } = renderHook(({ aplicado }) => useRascunho(aplicado, []), { initialProps: { aplicado: 'x' } })

    act(() => result.current.setRascunho('digitado'))
    rerender({ aplicado: 'x' })

    expect(result.current.rascunho).toBe('digitado')
  })

  it('substitui o que foi digitado quando o valor aplicado muda', () => {
    const { result, rerender } = renderHook(({ aplicado }) => useRascunho(aplicado, []), { initialProps: { aplicado: 'x' } })

    act(() => result.current.setRascunho('digitado'))
    rerender({ aplicado: 'y' })

    expect(result.current.rascunho).toBe('y')
  })

  it('a tela nunca recebe o valor antigo, nem na primeira renderização nem na mudança', () => {
    const naTela: string[] = []

    const { rerender } = renderHook(({ aplicado }) => useRascunho(aplicado, naTela), { initialProps: { aplicado: 'x' } })
    rerender({ aplicado: 'y' })

    // Com useEffect seriam ['inicial', 'x', 'x', 'y']: cada mudança pintaria primeiro o valor antigo.
    expect(naTela).toEqual(['x', 'y'])
  })
})
