import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listarUnidades } from '../api/materiais'
import { renderizarTela } from '../test/renderizar'
import { CampoUnidadeDeMedida } from './CampoUnidadeDeMedida'

vi.mock('../api/materiais', () => ({ listarUnidades: vi.fn() }))

/** O campo é controlado pela tela; aqui, um estado local faz o papel dela. */
function Tela() {
  const [unidade, setUnidade] = useState('')
  return <><CampoUnidadeDeMedida value={unidade} onChange={setUnidade} /><output data-testid="valor">{unidade}</output></>
}

describe('CampoUnidadeDeMedida', () => {
  beforeEach(() => {
    vi.mocked(listarUnidades).mockReset()
    vi.mocked(listarUnidades).mockResolvedValue(['G', 'KG', 'UN'])
  })

  it('o que a pessoa digita já aparece em maiúsculas', async () => {
    renderizarTela(<Tela />)

    await userEvent.type(screen.getByRole('combobox', { name: 'Unidade de medida' }), 'kg')

    expect(screen.getByRole('combobox', { name: 'Unidade de medida' })).toHaveValue('KG')
    expect(screen.getByTestId('valor')).toHaveTextContent('KG')
  })

  it('oferece as unidades já cadastradas como sugestão', async () => {
    renderizarTela(<Tela />)

    await userEvent.click(screen.getByRole('combobox', { name: 'Unidade de medida' }))

    expect(await screen.findByRole('option', { name: 'KG' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'G' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'UN' })).toBeInTheDocument()
  })

  it('escolher uma sugestão preenche o campo', async () => {
    renderizarTela(<Tela />)
    await userEvent.click(screen.getByRole('combobox', { name: 'Unidade de medida' }))

    await userEvent.click(await screen.findByRole('option', { name: 'UN' }))

    expect(screen.getByTestId('valor')).toHaveTextContent('UN')
  })

  it('uma unidade nova, fora das sugestões, continua permitida', async () => {
    renderizarTela(<Tela />)

    await userEvent.type(screen.getByRole('combobox', { name: 'Unidade de medida' }), 'cx')

    expect(screen.getByTestId('valor')).toHaveTextContent('CX')
  })

  it('sem permissão de ver material não consulta as sugestões e o campo segue como texto', async () => {
    renderizarTela(<Tela />, { permissoes: [] })

    await userEvent.type(screen.getByRole('combobox', { name: 'Unidade de medida' }), 'm')

    expect(listarUnidades).not.toHaveBeenCalled()
    expect(screen.getByTestId('valor')).toHaveTextContent('M')
  })
})
