import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { renderizarTela } from '../test/renderizar'
import type { Permissao, UsuarioLogado } from '../types'
import { Layout } from './Layout'

function montar(rota: string, permissoes: Permissao[], usuario?: Partial<UsuarioLogado>) {
  return renderizarTela(
    <Routes>
      <Route element={<Layout />}>
        <Route path="/dashboard" element={<div>conteúdo do painel</div>} />
        <Route path="/ordens" element={<div>conteúdo de ordens</div>} />
        <Route path="/minha-senha" element={<div>tela da troca de senha</div>} />
      </Route>
    </Routes>,
    { rotas: [rota], permissoes, usuario },
  )
}

describe('Layout: navegação por teclado e leitor de tela', () => {
  it('o menu é uma região de navegação com links de verdade, e marca a página atual', () => {
    montar('/ordens', ['DASHBOARD_VISUALIZAR', 'ORDEM_VISUALIZAR'])

    const menu = screen.getAllByRole('navigation', { name: 'Navegação principal', hidden: true })[0]
    expect(within(menu).getByRole('link', { name: 'Dashboard', hidden: true })).toHaveAttribute('href', '/dashboard')
    const ordens = within(menu).getByRole('link', { name: 'Ordens', hidden: true })
    expect(ordens).toHaveAttribute('href', '/ordens')
    expect(ordens).toHaveAttribute('aria-current', 'page')
  })

  it('o primeiro item da página pula o menu e leva o foco ao conteúdo', async () => {
    montar('/dashboard', ['DASHBOARD_VISUALIZAR'])

    await userEvent.tab()
    expect(screen.getByRole('link', { name: 'Pular para o conteúdo' })).toHaveFocus()

    await userEvent.keyboard('{Enter}')
    expect(screen.getByRole('main')).toHaveFocus()
  })

  it('o botão do menu no celular tem nome', () => {
    montar('/dashboard', ['DASHBOARD_VISUALIZAR'])

    expect(screen.getByRole('button', { name: 'Abrir menu de navegação', hidden: true })).toBeInTheDocument()
  })
})

describe('Layout: menu e senha provisória', () => {
  it('mostra no menu só o que o perfil permite, e quem está logado com o perfil', () => {
    montar('/dashboard', ['DASHBOARD_VISUALIZAR', 'ORDEM_VISUALIZAR'], { nome: 'Ana Teste', perfil: { id: 'p', nome: 'Operador' } })

    expect(screen.getByText('conteúdo do painel')).toBeInTheDocument()
    expect(screen.getAllByText('Dashboard').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Ordens').length).toBeGreaterThan(0)
    expect(screen.queryByText('Materiais')).not.toBeInTheDocument()
    expect(screen.queryByText('Perfis de acesso')).not.toBeInTheDocument()
    expect(screen.getAllByText('Ana Teste').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Operador').length).toBeGreaterThan(0)
  })

  it('com a troca de senha pendente, qualquer tela leva à troca e o menu não aparece', () => {
    montar('/ordens', ['ORDEM_VISUALIZAR', 'DASHBOARD_VISUALIZAR'], { deveTrocarSenha: true })

    expect(screen.getByTestId('rota')).toHaveTextContent('/minha-senha')
    expect(screen.queryByText('conteúdo de ordens')).not.toBeInTheDocument()
    expect(screen.queryByText('Alterar minha senha')).not.toBeInTheDocument()
  })

  it('a tela da troca de senha continua acessível enquanto a troca está pendente', () => {
    montar('/minha-senha', [], { deveTrocarSenha: true })

    expect(screen.getByText('tela da troca de senha')).toBeInTheDocument()
  })

  it('depois do primeiro acesso há "Alterar minha senha" no menu, que leva à tela da troca', async () => {
    montar('/dashboard', ['DASHBOARD_VISUALIZAR'])

    await userEvent.click(screen.getAllByText('Alterar minha senha')[0])

    expect(screen.getByTestId('rota')).toHaveTextContent('/minha-senha')
    expect(screen.getByText('tela da troca de senha')).toBeInTheDocument()
  })
})
