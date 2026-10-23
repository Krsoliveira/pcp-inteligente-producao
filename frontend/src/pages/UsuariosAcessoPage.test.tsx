import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { atribuirPerfil, criarUsuario, listarPerfis, listarUsuarios } from '../api/acessos'
import { erroHttp } from '../test/erros'
import { TODAS_AS_PERMISSOES } from '../test/fabricas'
import { renderizarTela } from '../test/renderizar'
import type { PerfilAcesso, Permissao, UsuarioAcesso } from '../types'
import { UsuariosAcessoPage } from './UsuariosAcessoPage'

vi.mock('../api/acessos', () => ({
  listarUsuarios: vi.fn(),
  listarPerfis: vi.fn(),
  criarUsuario: vi.fn(),
  atribuirPerfil: vi.fn(),
}))

function perfil(sobrescritas: Partial<PerfilAcesso>): PerfilAcesso {
  return {
    id: 'p-x', nome: 'Perfil', descricao: '', administrador: false, padraoDoAutocadastro: false,
    permissoes: ['ORDEM_VISUALIZAR'], quantidadeDeUsuarios: 0, atualizadoEm: '2026-10-20T10:00:00Z', atualizadoPor: 'admin',
    ...sobrescritas,
  }
}

function usuario(sobrescritas: Partial<UsuarioAcesso>): UsuarioAcesso {
  return { id: 'u-x', nome: 'Fulano', email: 'fulano@pcp.com', perfilId: 'p-op', perfilNome: 'Operador', criadoEm: '2026-10-20T10:00:00Z', ...sobrescritas }
}

const PERFIS = [
  perfil({ id: 'p-adm', nome: 'Administrador', administrador: true, permissoes: TODAS_AS_PERMISSOES }),
  perfil({ id: 'p-ger', nome: 'Gerente', permissoes: ['ORDEM_VISUALIZAR', 'ORDEM_CRIAR', 'USUARIO_GERENCIAR', 'ACESSO_VISUALIZAR'] }),
  perfil({ id: 'p-op', nome: 'Operador', permissoes: ['ORDEM_VISUALIZAR'] }),
  perfil({ id: 'p-vis', nome: 'Visualizador', padraoDoAutocadastro: true, permissoes: ['ORDEM_VISUALIZAR'] }),
]

const USUARIOS = [
  usuario({ id: 'u-eu', nome: 'Ana Teste', email: 'ana@pcp.com', perfilId: 'p-ger', perfilNome: 'Gerente' }),
  usuario({ id: 'u-bia', nome: 'Bia', email: 'bia@pcp.com', perfilId: 'p-op', perfilNome: 'Operador' }),
  usuario({ id: 'u-adm', nome: 'Chefe', email: 'chefe@pcp.com', perfilId: 'p-adm', perfilNome: 'Administrador' }),
]

/** Permissões do "Ana Teste" (ana@pcp.com): as do perfil Gerente. */
const DO_GERENTE: Permissao[] = ['ORDEM_VISUALIZAR', 'ORDEM_CRIAR', 'USUARIO_GERENCIAR', 'ACESSO_VISUALIZAR']

function montar(permissoes: Permissao[] = DO_GERENTE) {
  return renderizarTela(<UsuariosAcessoPage />, { permissoes })
}

describe('UsuariosAcessoPage', () => {
  beforeEach(() => {
    vi.mocked(listarUsuarios).mockReset().mockResolvedValue(USUARIOS)
    vi.mocked(listarPerfis).mockReset().mockResolvedValue(PERFIS)
    vi.mocked(criarUsuario).mockReset()
    vi.mocked(atribuirPerfil).mockReset()
  })

  it('lista os usuários com o perfil de cada um', async () => {
    montar()

    const linha = within((await screen.findByText('Bia')).closest('tr') as HTMLElement)
    expect(linha.getByText('bia@pcp.com')).toBeInTheDocument()
    expect(linha.getByText('Operador')).toBeInTheDocument()
  })

  it('ninguém altera o próprio perfil nem o de quem está acima do seu alcance', async () => {
    montar()
    await screen.findByText('Bia')

    expect(screen.getByRole('button', { name: 'Alterar o perfil de Bia' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Alterar o perfil de Ana Teste' })).not.toBeInTheDocument() // o próprio
    expect(screen.queryByRole('button', { name: 'Alterar o perfil de Chefe' })).not.toBeInTheDocument() // Administrador, acima do alcance
  })

  it('troca o perfil entre os que o usuário pode conceder e envia a escolha', async () => {
    vi.mocked(atribuirPerfil).mockResolvedValue(USUARIOS[1])
    montar()

    await userEvent.click(await screen.findByRole('button', { name: 'Alterar o perfil de Bia' }))
    await userEvent.click(await screen.findByRole('combobox', { name: 'Perfil' }))
    const opcoes = screen.getAllByRole('option').map((o) => o.textContent)
    expect(opcoes).toEqual(['Gerente', 'Operador', 'Visualizador']) // sem Administrador: passa do alcance
    await userEvent.click(screen.getByRole('option', { name: 'Gerente' }))
    await userEvent.click(screen.getByRole('button', { name: 'Salvar' }))

    await waitFor(() => expect(atribuirPerfil).toHaveBeenCalled())
    expect(vi.mocked(atribuirPerfil).mock.calls[0].slice(0, 2)).toEqual(['u-bia', 'p-ger'])
  })

  it('o botão de salvar só vale quando o perfil muda', async () => {
    montar()

    await userEvent.click(await screen.findByRole('button', { name: 'Alterar o perfil de Bia' }))

    expect(await screen.findByRole('button', { name: 'Salvar' })).toBeDisabled()
  })

  it('cria um usuário com o perfil escolhido e a senha provisória', async () => {
    vi.mocked(criarUsuario).mockResolvedValue(USUARIOS[1])
    montar()

    await userEvent.click(await screen.findByRole('button', { name: 'Novo usuário' }))
    await userEvent.type(await screen.findByLabelText(/^Nome/), 'Carla')
    await userEvent.type(screen.getByLabelText(/^E-mail/), 'carla@pcp.com')
    await userEvent.type(screen.getByLabelText(/^Senha provisória/), 'provisoria1')
    await userEvent.click(screen.getByRole('combobox', { name: /^Perfil/ }))
    await userEvent.click(screen.getByRole('option', { name: 'Operador' }))
    await userEvent.click(screen.getByRole('button', { name: 'Criar usuário' }))

    await waitFor(() => expect(criarUsuario).toHaveBeenCalled())
    expect(vi.mocked(criarUsuario).mock.calls[0][0]).toEqual({
      nome: 'Carla', email: 'carla@pcp.com', senhaProvisoria: 'provisoria1', perfilId: 'p-op',
    })
  })

  it('no cadastro de usuário só aparecem os perfis que o gestor pode conceder', async () => {
    montar()

    await userEvent.click(await screen.findByRole('button', { name: 'Novo usuário' }))
    await userEvent.click(await screen.findByRole('combobox', { name: /^Perfil/ }))

    expect(screen.getAllByRole('option').map((o) => o.textContent)).toEqual(['Gerente', 'Operador', 'Visualizador'])
  })

  it('valida os campos e o tamanho da senha antes de enviar', async () => {
    montar()
    await userEvent.click(await screen.findByRole('button', { name: 'Novo usuário' }))

    await userEvent.click(await screen.findByRole('button', { name: 'Criar usuário' }))
    expect(await screen.findByText('Preencha o nome, o e-mail e o perfil.')).toBeInTheDocument()

    await userEvent.type(screen.getByLabelText(/^Nome/), 'Carla')
    await userEvent.type(screen.getByLabelText(/^E-mail/), 'carla@pcp.com')
    await userEvent.type(screen.getByLabelText(/^Senha provisória/), 'curta')
    await userEvent.click(screen.getByRole('combobox', { name: /^Perfil/ }))
    await userEvent.click(screen.getByRole('option', { name: 'Operador' }))
    await userEvent.click(screen.getByRole('button', { name: 'Criar usuário' }))

    expect(await screen.findByText(/no mínimo 8 caracteres/i)).toBeInTheDocument()
    expect(criarUsuario).not.toHaveBeenCalled()
  })

  it('mostra o motivo quando o servidor recusa (e-mail já cadastrado)', async () => {
    vi.mocked(criarUsuario).mockRejectedValue(erroHttp(409, { detail: 'E-mail já cadastrado: carla@pcp.com' }))
    montar()
    await userEvent.click(await screen.findByRole('button', { name: 'Novo usuário' }))
    await userEvent.type(await screen.findByLabelText(/^Nome/), 'Carla')
    await userEvent.type(screen.getByLabelText(/^E-mail/), 'carla@pcp.com')
    await userEvent.type(screen.getByLabelText(/^Senha provisória/), 'provisoria1')
    await userEvent.click(screen.getByRole('combobox', { name: /^Perfil/ }))
    await userEvent.click(screen.getByRole('option', { name: 'Operador' }))

    await userEvent.click(screen.getByRole('button', { name: 'Criar usuário' }))

    expect(await screen.findByText('E-mail já cadastrado: carla@pcp.com')).toBeInTheDocument()
  })

  it('sem USUARIO_GERENCIAR só consulta: sem "Novo usuário" e sem trocar perfil', async () => {
    montar(['ACESSO_VISUALIZAR'])
    await screen.findByText('Bia')

    expect(screen.queryByRole('button', { name: 'Novo usuário' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Alterar o perfil de/ })).not.toBeInTheDocument()
  })
})
