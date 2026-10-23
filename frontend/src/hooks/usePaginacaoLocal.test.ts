import { act, renderHook } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { usePaginacaoLocal } from './usePaginacaoLocal'

const itens = (quantidade: number) => Array.from({ length: quantidade }, (_, i) => i + 1)

describe('usePaginacaoLocal', () => {
  it('mostra a primeira página com 25 itens por padrão', () => {
    const { result } = renderHook(() => usePaginacaoLocal(itens(60)))

    expect(result.current.visiveis).toHaveLength(25)
    expect(result.current.visiveis[0]).toBe(1)
    expect(result.current.total).toBe(60)
    expect(result.current.pagina).toBe(0)
  })

  it('navega entre as páginas e a última traz só o que sobrou', () => {
    const { result } = renderHook(() => usePaginacaoLocal(itens(60)))

    act(() => result.current.setPagina(2))

    expect(result.current.visiveis).toHaveLength(10)
    expect(result.current.visiveis[0]).toBe(51)
  })

  it('volta para a primeira página ao mudar o tamanho', () => {
    const { result } = renderHook(() => usePaginacaoLocal(itens(60)))
    act(() => result.current.setPagina(2))

    act(() => result.current.setTamanho(50))

    expect(result.current.pagina).toBe(0)
    expect(result.current.visiveis).toHaveLength(50)
  })

  it('ajusta para a última página válida quando um filtro reduz a lista', () => {
    const { result, rerender } = renderHook(({ lista }) => usePaginacaoLocal(lista), {
      initialProps: { lista: itens(60) },
    })
    act(() => result.current.setPagina(2))

    rerender({ lista: itens(30) })

    expect(result.current.pagina).toBe(1)
    expect(result.current.visiveis).toHaveLength(5)
  })

  it('aceita lista vazia', () => {
    const { result } = renderHook(() => usePaginacaoLocal<number>([]))

    expect(result.current.visiveis).toEqual([])
    expect(result.current.pagina).toBe(0)
  })
})
