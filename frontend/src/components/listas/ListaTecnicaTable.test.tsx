import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { listaTecnica, material } from '../../test/fabricas'
import { SondaDeRota } from '../../test/renderizar'
import { ListaTecnicaTable } from './ListaTecnicaTable'

const materialPorId = new Map([['m-1', material({ id: 'm-1', codigo: '103000001', descricao: 'Viga metálica' })]])

function montar(props: Partial<React.ComponentProps<typeof ListaTecnicaTable>> = {}) {
  const onAtivar = vi.fn()
  const onRetirarDeUso = vi.fn()
  render(
    <MemoryRouter>
      <ListaTecnicaTable
        listas={[listaTecnica({ id: 'l-1', versao: 'v1', status: 'ATIVA' })]}
        materialPorId={materialPorId}
        onAtivar={onAtivar}
        onRetirarDeUso={onRetirarDeUso}
        {...props}
      />
      <SondaDeRota />
    </MemoryRouter>,
  )
  return { onAtivar, onRetirarDeUso }
}

describe('ListaTecnicaTable', () => {
  it('mostra o código do material e a descrição em colunas separadas', () => {
    montar()

    expect(screen.getByRole('columnheader', { name: 'Código' })).toBeInTheDocument()
    expect(screen.getByRole('columnheader', { name: 'Descrição' })).toBeInTheDocument()
    const celulas = within(screen.getAllByRole('row')[1]).getAllByRole('cell')
    expect(celulas[0]).toHaveTextContent('103.000.001')
    expect(celulas[1]).toHaveTextContent('Viga metálica')
    expect(celulas[0]).not.toHaveTextContent('Viga')
  })

  it('versão ativa oferece retirar de uso, e não ativar', async () => {
    const { onAtivar, onRetirarDeUso } = montar()

    expect(screen.queryByRole('button', { name: 'Ativar versão v1' })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Retirar versão v1 de uso' }))

    expect(onRetirarDeUso).toHaveBeenCalledWith(expect.objectContaining({ id: 'l-1' }))
    expect(onAtivar).not.toHaveBeenCalled()
  })

  it('versão em revisão oferece ativar, e não retirar de uso', async () => {
    const { onAtivar } = montar({ listas: [listaTecnica({ id: 'l-2', versao: 'v2', status: 'EM_REVISAO' })] })

    expect(screen.queryByRole('button', { name: 'Retirar versão v2 de uso' })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Ativar versão v2' }))

    expect(onAtivar).toHaveBeenCalledWith('l-2')
  })

  it('versão obsoleta não tem ação', () => {
    montar({ listas: [listaTecnica({ id: 'l-3', versao: 'v0', status: 'OBSOLETA' })] })

    expect(screen.queryByRole('button', { name: /versão v0/ })).not.toBeInTheDocument()
  })

  it('clicar na ação não abre o detalhe da linha', async () => {
    montar()

    await userEvent.click(screen.getByRole('button', { name: 'Retirar versão v1 de uso' }))

    expect(screen.getByTestId('rota')).toHaveTextContent(/^\/$/)
    await userEvent.click(screen.getByText('Viga metálica'))
    expect(screen.getByTestId('rota')).toHaveTextContent('/listas-tecnicas/l-1')
  })
})
