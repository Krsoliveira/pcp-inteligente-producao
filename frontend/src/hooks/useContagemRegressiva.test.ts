import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useContagemRegressiva } from './useContagemRegressiva'

describe('useContagemRegressiva', () => {
  beforeEach(() => vi.useFakeTimers())
  afterEach(() => vi.useRealTimers())

  // Um segundo por vez: o hook reagenda o próximo tick depois de cada atualização, como no uso real.
  const passar = (segundos: number) => {
    for (let i = 0; i < segundos; i++) act(() => { vi.advanceTimersByTime(1000) })
  }

  it('começa parada em zero', () => {
    const { result } = renderHook(() => useContagemRegressiva())

    expect(result.current.restante).toBe(0)
  })

  it('desce um segundo por vez até zero e para', () => {
    const { result } = renderHook(() => useContagemRegressiva())

    act(() => result.current.iniciar(3))
    expect(result.current.restante).toBe(3)

    passar(1)
    expect(result.current.restante).toBe(2)
    passar(1)
    expect(result.current.restante).toBe(1)
    passar(1)
    expect(result.current.restante).toBe(0)
    passar(5)
    expect(result.current.restante).toBe(0)
  })

  it('arredonda segundos fracionados para cima', () => {
    const { result } = renderHook(() => useContagemRegressiva())

    act(() => result.current.iniciar(2.2))

    expect(result.current.restante).toBe(3)
  })

  it('ignora valores negativos', () => {
    const { result } = renderHook(() => useContagemRegressiva())

    act(() => result.current.iniciar(-4))

    expect(result.current.restante).toBe(0)
  })

  it('reinicia a contagem no meio do caminho', () => {
    const { result } = renderHook(() => useContagemRegressiva())

    act(() => result.current.iniciar(10))
    passar(4)
    expect(result.current.restante).toBe(6)

    act(() => result.current.iniciar(60))
    expect(result.current.restante).toBe(60)
  })
})
