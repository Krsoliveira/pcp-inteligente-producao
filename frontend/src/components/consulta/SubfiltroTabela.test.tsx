import { act, renderHook, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { SubfiltroTabela, useSubfiltro } from './SubfiltroTabela'

const linhas = [
  { codigo: 'OP-1', centro: 'Usinagem' },
  { codigo: 'OP-2', centro: 'Soldagem' },
  { codigo: 'OP-3', centro: 'Usinagem CNC' },
]
const textoDe = (l: (typeof linhas)[number]) => `${l.codigo} ${l.centro}`

describe('useSubfiltro', () => {
  it('sem texto devolve todas as linhas', () => {
    const { result } = renderHook(() => useSubfiltro(linhas, textoDe))

    expect(result.current.filtrados).toHaveLength(3)
  })

  it('filtra sem diferenciar maiúsculas e minúsculas', () => {
    const { result } = renderHook(() => useSubfiltro(linhas, textoDe))

    act(() => result.current.setTexto('USINAGEM'))

    expect(result.current.filtrados.map((l) => l.codigo)).toEqual(['OP-1', 'OP-3'])
  })

  it('ignora espaços nas pontas e texto só com espaços', () => {
    const { result } = renderHook(() => useSubfiltro(linhas, textoDe))

    act(() => result.current.setTexto('  op-2  '))
    expect(result.current.filtrados).toHaveLength(1)

    act(() => result.current.setTexto('   '))
    expect(result.current.filtrados).toHaveLength(3)
  })

  it('devolve vazio quando nada corresponde', () => {
    const { result } = renderHook(() => useSubfiltro(linhas, textoDe))

    act(() => result.current.setTexto('inexistente'))

    expect(result.current.filtrados).toEqual([])
  })
})

describe('SubfiltroTabela', () => {
  it('informa quantas linhas há na página quando não há filtro', () => {
    render(<SubfiltroTabela valor="" onChange={() => {}} exibidas={25} naPagina={25} />)

    expect(screen.getByText('25 linhas nesta página')).toBeInTheDocument()
  })

  it('informa quantas linhas restam depois do filtro', () => {
    render(<SubfiltroTabela valor="usinagem" onChange={() => {}} exibidas={2} naPagina={25} />)

    expect(screen.getByText('Mostrando 2 de 25 linhas desta página')).toBeInTheDocument()
  })

  it('avisa a cada letra digitada', async () => {
    const onChange = vi.fn()
    render(<SubfiltroTabela valor="" onChange={onChange} exibidas={3} naPagina={3} />)

    await userEvent.type(screen.getByPlaceholderText(/filtrar nas linhas desta página/i), 'ab')

    expect(onChange).toHaveBeenCalledTimes(2)
    expect(onChange).toHaveBeenLastCalledWith('b')
  })
})
