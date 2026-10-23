import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { PainelDeConsulta } from './PainelDeConsulta'

function montar() {
  const onPesquisar = vi.fn()
  const onLimpar = vi.fn()
  render(
    <PainelDeConsulta onPesquisar={onPesquisar} onLimpar={onLimpar}>
      <input aria-label="Código" />
    </PainelDeConsulta>,
  )
  return { onPesquisar, onLimpar }
}

describe('PainelDeConsulta', () => {
  it('mostra os campos recebidos e os botões Pesquisar e Limpar', () => {
    montar()

    expect(screen.getByLabelText('Código')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Pesquisar' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Limpar' })).toBeInTheDocument()
  })

  it('pesquisa ao clicar em Pesquisar', async () => {
    const { onPesquisar } = montar()

    await userEvent.click(screen.getByRole('button', { name: 'Pesquisar' }))

    expect(onPesquisar).toHaveBeenCalledTimes(1)
  })

  it('pesquisa ao apertar Enter dentro de um campo', async () => {
    const { onPesquisar } = montar()

    await userEvent.type(screen.getByLabelText('Código'), 'OP-1{Enter}')

    expect(onPesquisar).toHaveBeenCalledTimes(1)
  })

  it('limpa sem disparar a pesquisa', async () => {
    const { onPesquisar, onLimpar } = montar()

    await userEvent.click(screen.getByRole('button', { name: 'Limpar' }))

    expect(onLimpar).toHaveBeenCalledTimes(1)
    expect(onPesquisar).not.toHaveBeenCalled()
  })
})
