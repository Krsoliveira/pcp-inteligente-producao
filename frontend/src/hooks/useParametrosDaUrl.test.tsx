import { act, renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { useParametrosDaUrl } from './useParametrosDaUrl'

function montar(url: string) {
  const wrapper = ({ children }: { children: ReactNode }) => <MemoryRouter initialEntries={[url]}>{children}</MemoryRouter>
  return renderHook(() => ({ url: useParametrosDaUrl(), local: useLocation() }), { wrapper })
}

describe('useParametrosDaUrl', () => {
  it('lê textos da URL, com vazio quando a chave não existe', () => {
    const { result } = montar('/ordens/resultados?busca=OP-1&status=LIBERADA')

    expect(result.current.url.texto('busca')).toBe('OP-1')
    expect(result.current.url.texto('status')).toBe('LIBERADA')
    expect(result.current.url.texto('inexistente')).toBe('')
  })

  it('lê números, com o padrão quando a chave falta ou o valor é inválido', () => {
    const { result } = montar('/x?pagina=2&tamanho=abc&negativo=-3&decimal=1.5')

    expect(result.current.url.numero('pagina', 0)).toBe(2)
    expect(result.current.url.numero('tamanho', 25)).toBe(25)
    expect(result.current.url.numero('negativo', 7)).toBe(7)
    expect(result.current.url.numero('decimal', 7)).toBe(7)
    expect(result.current.url.numero('ausente', 9)).toBe(9)
  })

  it('altera só as chaves informadas e preserva as demais', () => {
    const { result } = montar('/x?busca=a&status=LIBERADA&pagina=3')

    act(() => result.current.url.atualizar({ busca: 'b' }))

    expect(result.current.local.search).toBe('?busca=b&status=LIBERADA&pagina=3')
  })

  it('remove a chave quando o valor é vazio, nulo ou indefinido', () => {
    const { result } = montar('/x?busca=a&status=LIBERADA&pagina=3')

    act(() => result.current.url.atualizar({ busca: '', status: undefined, pagina: null }))

    expect(result.current.local.search).toBe('')
  })

  it('aceita números e mantém o zero', () => {
    const { result } = montar('/x')

    act(() => result.current.url.atualizar({ pagina: 0, tamanho: 50 }))

    expect(result.current.local.search).toBe('?pagina=0&tamanho=50')
  })
})
