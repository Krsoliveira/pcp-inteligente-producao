import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { consultarUsuarioLogado } from '../api/acessos'
import { useAuthStore } from '../store/authStore'
import { erroHttp } from '../test/erros'
import { usuarioLogado } from '../test/fabricas'
import { renderizarTela } from '../test/renderizar'
import type { Permissao } from '../types'
import { ExigirPermissao } from './ExigirPermissao'
import { SePermitido } from './SePermitido'

vi.mock('../api/acessos', () => ({ consultarUsuarioLogado: vi.fn() }))

const eu = vi.mocked(consultarUsuarioLogado)

function montar(qualquer: Permissao[], permissoes: Permissao[] | null, paginaInicial = false) {
  return renderizarTela(
    <Routes>
      <Route element={<ExigirPermissao qualquer={qualquer} paginaInicial={paginaInicial} />}>
        <Route path="/secreto" element={<div>conteúdo protegido</div>} />
        <Route path="/dashboard" element={<div>conteúdo do painel</div>} />
      </Route>
      <Route path="/ordens" element={<div>tela de ordens</div>} />
    </Routes>,
    { rotas: [paginaInicial ? '/dashboard' : '/secreto'], permissoes },
  )
}

describe('ExigirPermissao', () => {
  beforeEach(() => { eu.mockReset(); useAuthStore.setState({ token: 'token-de-teste' }) })
  afterEach(() => useAuthStore.setState({ token: null }))

  it('abre a tela quando o perfil tem a permissão (basta uma das pedidas)', async () => {
    montar(['LISTA_VISUALIZAR_ATIVAS', 'LISTA_VISUALIZAR_TODAS'], ['LISTA_VISUALIZAR_TODAS'])

    expect(await screen.findByText('conteúdo protegido')).toBeInTheDocument()
  })

  it('sem a permissão mostra "sem acesso", sem o conteúdo, e leva ao início permitido', async () => {
    montar(['ACESSO_VISUALIZAR'], ['ORDEM_VISUALIZAR'])

    expect(await screen.findByText('Você não tem acesso a esta tela')).toBeInTheDocument()
    expect(screen.queryByText('conteúdo protegido')).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Ir para o início' }))
    expect(screen.getByTestId('rota')).toHaveTextContent('/ordens')
  })

  it('perfil sem nenhuma tela não oferece o botão de início', async () => {
    montar(['ACESSO_VISUALIZAR'], ['ORDEM_CRIAR'])

    expect(await screen.findByText(/ainda não dá acesso a nenhuma tela/i)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Ir para o início' })).not.toBeInTheDocument()
  })

  it('na entrada do sistema, sem acesso ao painel, vai direto à primeira tela permitida', async () => {
    montar(['DASHBOARD_VISUALIZAR'], ['ORDEM_VISUALIZAR'], true)

    expect(await screen.findByText('tela de ordens')).toBeInTheDocument()
    expect(screen.queryByText('conteúdo do painel')).not.toBeInTheDocument()
  })

  it('enquanto verifica o acesso não mostra o conteúdo; ao responder, libera', async () => {
    eu.mockResolvedValue(usuarioLogado({ permissoes: ['ACESSO_VISUALIZAR'] }))
    montar(['ACESSO_VISUALIZAR'], null)

    expect(screen.queryByText('conteúdo protegido')).not.toBeInTheDocument()
    expect(await screen.findByText('conteúdo protegido')).toBeInTheDocument()
  })

  it('se a verificação do acesso falha, avisa e deixa tentar de novo', async () => {
    eu.mockRejectedValueOnce(erroHttp(500))
    montar(['ACESSO_VISUALIZAR'], null)

    expect(await screen.findByText(/não foi possível verificar o seu acesso/i)).toBeInTheDocument()
    expect(screen.queryByText('conteúdo protegido')).not.toBeInTheDocument()

    eu.mockResolvedValue(usuarioLogado({ permissoes: ['ACESSO_VISUALIZAR'] }))
    await userEvent.click(screen.getByRole('button', { name: 'Tentar de novo' }))
    expect(await screen.findByText('conteúdo protegido')).toBeInTheDocument()
  })
})

describe('SePermitido', () => {
  it('mostra o conteúdo a quem tem ao menos uma das permissões e esconde dos demais', () => {
    renderizarTela(
      <>
        <SePermitido qualquer={['ORDEM_CRIAR']}><button>Nova ordem</button></SePermitido>
        <SePermitido qualquer={['MATERIAL_CADASTRAR', 'ORDEM_CONCLUIR']}><button>Cadastrar ou concluir</button></SePermitido>
        <SePermitido qualquer={['PERFIL_GERENCIAR']}><button>Gerenciar perfis</button></SePermitido>
      </>,
      { permissoes: ['ORDEM_CRIAR', 'ORDEM_CONCLUIR'] },
    )

    expect(screen.getByRole('button', { name: 'Nova ordem' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cadastrar ou concluir' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Gerenciar perfis' })).not.toBeInTheDocument()
  })
})
