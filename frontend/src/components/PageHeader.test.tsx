import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderizarTela } from '../test/renderizar'
import { PageHeader } from './PageHeader'

const comPai = [{ label: 'Produção' }, { label: 'Ordens', href: '/ordens' }, { label: 'OP-1' }]

describe('PageHeader', () => {
  it('mostra título, subtítulo e a ação', () => {
    renderizarTela(<PageHeader titulo="Ordens" subtitulo="12 ordens" action={<button>Nova</button>} />)

    expect(screen.getByRole('heading', { name: 'Ordens' })).toBeInTheDocument()
    expect(screen.getByText('12 ordens')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Nova' })).toBeInTheDocument()
  })

  it('não mostra Voltar em telas sem item pai com link', () => {
    renderizarTela(<PageHeader titulo="Ordens" breadcrumbs={[{ label: 'Produção' }, { label: 'Ordens' }]} />)

    expect(screen.queryByRole('button', { name: /voltar/i })).not.toBeInTheDocument()
  })

  it('mostra Voltar quando o breadcrumb tem um item pai com link', () => {
    renderizarTela(<PageHeader titulo="OP-1" breadcrumbs={comPai} />)

    expect(screen.getByRole('button', { name: /voltar/i })).toBeInTheDocument()
  })

  it('volta para a tela de origem quando há histórico', async () => {
    renderizarTela(<PageHeader titulo="OP-1" breadcrumbs={comPai} />, { rotas: ['/dashboard', '/ordens/1'], padrao: '/ordens/1' })

    await userEvent.click(screen.getByRole('button', { name: /voltar/i }))

    expect(screen.getByTestId('rota')).toHaveTextContent('/dashboard')
  })

  it('vai para o item pai quando a página foi aberta direto, sem histórico', async () => {
    renderizarTela(<PageHeader titulo="OP-1" breadcrumbs={comPai} />, { rotas: ['/ordens/1'], padrao: '/ordens/1' })

    await userEvent.click(screen.getByRole('button', { name: /voltar/i }))

    expect(screen.getByTestId('rota')).toHaveTextContent('/ordens')
  })

  it('o item pai do breadcrumb também navega ao ser clicado', async () => {
    renderizarTela(<PageHeader titulo="OP-1" breadcrumbs={comPai} />, { rotas: ['/ordens/1'], padrao: '/ordens/1' })

    await userEvent.click(screen.getByText('Ordens'))

    expect(screen.getByTestId('rota')).toHaveTextContent('/ordens')
  })
})
