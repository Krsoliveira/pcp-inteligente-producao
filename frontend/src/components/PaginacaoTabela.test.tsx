import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { PaginacaoTabela, totalDePaginas } from './PaginacaoTabela'

function montar(props: Partial<Parameters<typeof PaginacaoTabela>[0]> = {}) {
  const onPagina = vi.fn()
  const onTamanho = vi.fn()
  render(<PaginacaoTabela total={100} pagina={0} tamanho={25} onPagina={onPagina} onTamanho={onTamanho} {...props} />)
  return { onPagina, onTamanho }
}

describe('PaginacaoTabela', () => {
  it('mostra o intervalo e o total em português', () => {
    montar()

    expect(screen.getByText('1–25 de 100 · página 1 de 4')).toBeInTheDocument()
    expect(screen.getByText('Linhas por página')).toBeInTheDocument()
  })

  it('vai para a próxima página', async () => {
    const { onPagina } = montar()

    await userEvent.click(screen.getByRole('button', { name: 'Próxima página' }))

    expect(onPagina).toHaveBeenCalledWith(1)
  })

  it('volta para a página anterior', async () => {
    const { onPagina } = montar({ pagina: 2 })

    await userEvent.click(screen.getByRole('button', { name: 'Página anterior' }))

    expect(onPagina).toHaveBeenCalledWith(1)
  })

  it('não deixa voltar na primeira página nem avançar na última', () => {
    montar({ pagina: 0 })
    expect(screen.getByRole('button', { name: 'Página anterior' })).toBeDisabled()
  })

  it('desabilita "Próxima página" na última página', () => {
    montar({ pagina: 3 })

    expect(screen.getByRole('button', { name: 'Próxima página' })).toBeDisabled()
    expect(screen.getByText('76–100 de 100 · página 4 de 4')).toBeInTheDocument()
  })

  it('diz explicitamente em que página está e quantas existem', () => {
    montar({ total: 170, pagina: 2, tamanho: 25 })

    expect(screen.getByText('51–75 de 170 · página 3 de 7')).toBeInTheDocument()
  })

  it('a última página parcial conta como uma página', () => {
    montar({ total: 101, pagina: 4, tamanho: 25 })

    expect(screen.getByText('101–101 de 101 · página 5 de 5')).toBeInTheDocument()
  })

  it('sem resultados continua mostrando "página 1 de 1"', () => {
    montar({ total: 0 })

    expect(screen.getByText('0–0 de 0 · página 1 de 1')).toBeInTheDocument()
  })

  it('tem botões para ir direto à primeira e à última página', async () => {
    const { onPagina } = montar({ total: 100, pagina: 1, tamanho: 25 })

    await userEvent.click(screen.getByRole('button', { name: 'Última página' }))
    await userEvent.click(screen.getByRole('button', { name: 'Primeira página' }))

    expect(onPagina).toHaveBeenNthCalledWith(1, 3)
    expect(onPagina).toHaveBeenNthCalledWith(2, 0)
  })

  it('troca a quantidade de linhas por página', async () => {
    const { onTamanho } = montar()

    await userEvent.click(screen.getByRole('combobox'))
    await userEvent.click(screen.getByRole('option', { name: '50' }))

    expect(onTamanho).toHaveBeenCalledWith(50)
  })
})

describe('totalDePaginas', () => {
  it('arredonda para cima e nunca é menor que 1', () => {
    expect(totalDePaginas(0, 25)).toBe(1)
    expect(totalDePaginas(25, 25)).toBe(1)
    expect(totalDePaginas(26, 25)).toBe(2)
    expect(totalDePaginas(170, 25)).toBe(7)
  })
})
