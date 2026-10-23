import { screen } from '@testing-library/react'
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
