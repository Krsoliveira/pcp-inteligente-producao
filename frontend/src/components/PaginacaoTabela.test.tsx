import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { PaginacaoTabela } from './PaginacaoTabela'

function montar(props: Partial<Parameters<typeof PaginacaoTabela>[0]> = {}) {
  const onPagina = vi.fn()
  const onTamanho = vi.fn()
  render(<PaginacaoTabela total={100} pagina={0} tamanho={25} onPagina={onPagina} onTamanho={onTamanho} {...props} />)
  return { onPagina, onTamanho }
}

describe('PaginacaoTabela', () => {
  it('mostra o intervalo e o total em português', () => {
    montar()

    expect(screen.getByText('1–25 de 100')).toBeInTheDocument()
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
    expect(screen.getByText('76–100 de 100')).toBeInTheDocument()
  })

  it('troca a quantidade de linhas por página', async () => {
    const { onTamanho } = montar()

    await userEvent.click(screen.getByRole('combobox'))
    await userEvent.click(screen.getByRole('option', { name: '50' }))

    expect(onTamanho).toHaveBeenCalledWith(50)
  })
})
