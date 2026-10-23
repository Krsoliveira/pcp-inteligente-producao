import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { atualizarPerfil, criarPerfil, excluirPerfil, listarPerfis, listarPermissoes } from '../api/acessos'
import { erroHttp } from '../test/erros'
import { TODAS_AS_PERMISSOES } from '../test/fabricas'
import { renderizarTela } from '../test/renderizar'
import type { PerfilAcesso, Permissao, PermissaoDoCatalogo } from '../types'
import { PerfisAcessoPage } from './PerfisAcessoPage'

vi.mock('../api/acessos', () => ({
  listarPerfis: vi.fn(),
  listarPermissoes: vi.fn(),
  criarPerfil: vi.fn(),
  atualizarPerfil: vi.fn(),
  excluirPerfil: vi.fn(),
}))

const CATALOGO: PermissaoDoCatalogo[] = [
  { permissao: 'MATERIAL_VISUALIZAR', area: 'Materiais', descricao: 'Consultar materiais' },
  { permissao: 'ORDEM_VISUALIZAR', area: 'Ordens de produção', descricao: 'Consultar ordens' },
  { permissao: 'ORDEM_CRIAR', area: 'Ordens de produção', descricao: 'Criar ordens' },
  { permissao: 'LISTA_ATIVAR', area: 'Listas técnicas', descricao: 'Ativar uma versão' },
  { permissao: 'PERFIL_GERENCIAR', area: 'Acessos', descricao: 'Criar, alterar e excluir perfis' },
]

function perfil(sobrescritas: Partial<PerfilAcesso>): PerfilAcesso {
  return {
    id: 'p-x', nome: 'Perfil', descricao: '', administrador: false, padraoDoAutocadastro: false,
    permissoes: ['ORDEM_VISUALIZAR'], quantidadeDeUsuarios: 0, atualizadoEm: '2026-10-20T10:00:00Z', atualizadoPor: 'admin@pcp.com',
    ...sobrescritas,
  }
}

const PERFIS: PerfilAcesso[] = [
  perfil({ id: 'p-adm', nome: 'Administrador', administrador: true, permissoes: TODAS_AS_PERMISSOES, quantidadeDeUsuarios: 1 }),
  perfil({ id: 'p-vis', nome: 'Visualizador', padraoDoAutocadastro: true, quantidadeDeUsuarios: 4 }),
  perfil({ id: 'p-ger', nome: 'Gerente', permissoes: ['ORDEM_VISUALIZAR', 'ORDEM_CRIAR', 'PERFIL_GERENCIAR'], quantidadeDeUsuarios: 2 }),
  perfil({ id: 'p-op', nome: 'Operador', descricao: 'Chão de fábrica', permissoes: ['ORDEM_VISUALIZAR'], quantidadeDeUsuarios: 0 }),
]

const TUDO_DE_PERFIS: Permissao[] = ['ACESSO_VISUALIZAR', 'PERFIL_GERENCIAR', 'AUDITORIA_VISUALIZAR', 'ORDEM_VISUALIZAR', 'ORDEM_CRIAR', 'LISTA_ATIVAR']

function montar(permissoes: Permissao[] = TODAS_AS_PERMISSOES) {
  return renderizarTela(<PerfisAcessoPage />, { permissoes })
}

describe('PerfisAcessoPage', () => {
  beforeEach(() => {
    vi.mocked(listarPerfis).mockReset().mockResolvedValue(PERFIS)
    vi.mocked(listarPermissoes).mockReset().mockResolvedValue(CATALOGO)
    vi.mocked(criarPerfil).mockReset()
    vi.mocked(atualizarPerfil).mockReset()
    vi.mocked(excluirPerfil).mockReset()
  })

  it('lista os perfis com a quantidade de usuários e marca os de sistema', async () => {
    montar()

    const linha = async (nome: string) => within((await screen.findByText(nome)).closest('tr') as HTMLElement)
    expect((await linha('Administrador')).getByText('Sistema')).toBeInTheDocument()
    expect((await linha('Administrador')).getByText('Todas')).toBeInTheDocument()
    expect((await linha('Visualizador')).getByText('Sistema')).toBeInTheDocument()
    expect((await linha('Gerente')).getByText('3')).toBeInTheDocument()
    expect((await linha('Operador')).queryByText('Sistema')).not.toBeInTheDocument()
  })

  it('o Administrador abre só para leitura, com aviso, sem botão de salvar', async () => {
    montar()

    await userEvent.click(await screen.findByRole('button', { name: 'Ver perfil Administrador' }))

    expect(await screen.findByText(/perfil de sistema: tem todas as permissões/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/^Nome/)).toBeDisabled()
    expect(screen.queryByRole('button', { name: 'Salvar perfil' })).not.toBeInTheDocument()
  })

  it('cria um perfil marcando permissões e envia nome, descrição e as marcadas', async () => {
    vi.mocked(criarPerfil).mockResolvedValue(PERFIS[3])
    montar()

    await userEvent.click(await screen.findByRole('button', { name: 'Novo perfil' }))
    await userEvent.type(await screen.findByLabelText(/^Nome/), 'Qualidade')
    await userEvent.type(screen.getByLabelText('Descrição'), 'Confere as listas')
    await userEvent.click(await screen.findByLabelText('Consultar ordens'))
    await userEvent.click(screen.getByLabelText('Ativar uma versão'))
    await userEvent.click(screen.getByRole('button', { name: 'Criar perfil' }))

    await waitFor(() => expect(criarPerfil).toHaveBeenCalled())
    const enviado = vi.mocked(criarPerfil).mock.calls[0][0]
    expect(enviado).toMatchObject({ nome: 'Qualidade', descricao: 'Confere as listas' })
    expect([...enviado.permissoes].sort()).toEqual(['LISTA_ATIVAR', 'ORDEM_VISUALIZAR'])
  })

  it('exige nome e ao menos uma permissão antes de enviar', async () => {
    montar()
    await userEvent.click(await screen.findByRole('button', { name: 'Novo perfil' }))

    await userEvent.click(await screen.findByRole('button', { name: 'Criar perfil' }))
    expect(await screen.findByText('Informe o nome do perfil.')).toBeInTheDocument()

    await userEvent.type(screen.getByLabelText(/^Nome/), 'Sem permissões')
    await userEvent.click(screen.getByRole('button', { name: 'Criar perfil' }))
    expect(await screen.findByText('Marque ao menos uma permissão.')).toBeInTheDocument()
    expect(criarPerfil).not.toHaveBeenCalled()
  })

  it('só se concede o que se tem: permissões que o usuário não possui ficam desabilitadas', async () => {
    montar(['ACESSO_VISUALIZAR', 'PERFIL_GERENCIAR', 'ORDEM_VISUALIZAR'])

    await userEvent.click(await screen.findByRole('button', { name: 'Novo perfil' }))

    expect(await screen.findByLabelText('Consultar ordens')).toBeEnabled()
    expect(screen.getByLabelText('Ativar uma versão')).toBeDisabled()
    expect(screen.getByLabelText('Criar ordens')).toBeDisabled()
  })

  it('"marcar tudo" de uma área marca só o que o usuário pode conceder', async () => {
    montar(['ACESSO_VISUALIZAR', 'PERFIL_GERENCIAR', 'ORDEM_VISUALIZAR'])
    await userEvent.click(await screen.findByRole('button', { name: 'Novo perfil' }))

    await userEvent.click(await screen.findByRole('checkbox', { name: 'Marcar tudo de Ordens de produção' }))

    expect(screen.getByLabelText('Consultar ordens')).toBeChecked()
    expect(screen.getByLabelText('Criar ordens')).not.toBeChecked()
  })

  it('um perfil com permissões acima do alcance de quem abre só pode ser consultado', async () => {
    montar(TUDO_DE_PERFIS.filter((p) => p !== 'ORDEM_CRIAR'))

    await userEvent.click(await screen.findByRole('button', { name: 'Editar perfil Gerente' }))

    expect(await screen.findByText(/você só pode consultá-lo/i)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Salvar perfil' })).not.toBeInTheDocument()
  })

  it('edita um perfil e envia o que mudou', async () => {
    vi.mocked(atualizarPerfil).mockResolvedValue(PERFIS[3])
    montar()

    await userEvent.click(await screen.findByRole('button', { name: 'Editar perfil Operador' }))
    await userEvent.click(await screen.findByLabelText('Criar ordens'))
    await userEvent.click(screen.getByRole('button', { name: 'Salvar perfil' }))

    await waitFor(() => expect(atualizarPerfil).toHaveBeenCalled())
    const [id, payload] = vi.mocked(atualizarPerfil).mock.calls[0]
    expect(id).toBe('p-op')
    expect([...payload.permissoes].sort()).toEqual(['ORDEM_CRIAR', 'ORDEM_VISUALIZAR'])
  })

  it('mostra o motivo que o servidor deu quando recusa (nome repetido) e não fecha', async () => {
    vi.mocked(criarPerfil).mockRejectedValue(erroHttp(409, { detail: 'Já existe um perfil com o nome "Gerente".' }))
    montar()
    await userEvent.click(await screen.findByRole('button', { name: 'Novo perfil' }))
    await userEvent.type(await screen.findByLabelText(/^Nome/), 'Gerente')
    await userEvent.click(screen.getByLabelText('Consultar ordens'))

    await userEvent.click(screen.getByRole('button', { name: 'Criar perfil' }))

    expect(await screen.findByText('Já existe um perfil com o nome "Gerente".')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Criar perfil' })).toBeInTheDocument()
  })

  it('só oferece excluir um perfil que não é de sistema e que ninguém usa; confirma antes', async () => {
    vi.mocked(excluirPerfil).mockResolvedValue()
    montar()
    await screen.findByText('Operador')

    expect(screen.getByRole('button', { name: 'Excluir perfil Operador' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Excluir perfil Gerente' })).not.toBeInTheDocument() // em uso
    expect(screen.queryByRole('button', { name: 'Excluir perfil Visualizador' })).not.toBeInTheDocument() // de sistema
    expect(screen.queryByRole('button', { name: 'Excluir perfil Administrador' })).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Excluir perfil Operador' }))
    expect(excluirPerfil).not.toHaveBeenCalled()
    await userEvent.click(await screen.findByRole('button', { name: 'Excluir perfil' }))

    await waitFor(() => expect(excluirPerfil).toHaveBeenCalled())
    expect(vi.mocked(excluirPerfil).mock.calls[0][0]).toBe('p-op')
  })

  it('sem PERFIL_GERENCIAR só consulta: sem "Novo perfil", sem editar e sem excluir', async () => {
    montar(['ACESSO_VISUALIZAR', 'ORDEM_VISUALIZAR'])
    await screen.findByText('Operador')

    expect(screen.queryByRole('button', { name: 'Novo perfil' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Editar perfil/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Excluir perfil/ })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ver perfil Operador' })).toBeInTheDocument()
  })

  it('avisa que sem "Consultar materiais" as telas mostram só códigos internos, e some ao marcar', async () => {
    montar()
    await userEvent.click(await screen.findByRole('button', { name: 'Novo perfil' }))
    await screen.findByLabelText('Consultar ordens')
    expect(screen.queryByText(/só códigos internos/i)).not.toBeInTheDocument()

    await userEvent.click(screen.getByLabelText('Consultar ordens'))
    expect(await screen.findByText(/só códigos internos/i)).toBeInTheDocument()

    await userEvent.click(screen.getByLabelText('Consultar materiais'))
    await waitFor(() => expect(screen.queryByText(/só códigos internos/i)).not.toBeInTheDocument())
  })

  it('não avisa quando o perfil não tem nada que dependa dos nomes dos materiais', async () => {
    montar()
    await userEvent.click(await screen.findByRole('button', { name: 'Novo perfil' }))

    await userEvent.click(await screen.findByLabelText('Ativar uma versão'))

    expect(screen.queryByText(/só códigos internos/i)).not.toBeInTheDocument()
  })

  it('mostra o motivo quando não consegue carregar os perfis', async () => {
    vi.mocked(listarPerfis).mockRejectedValue(erroHttp(403, { detail: 'Você não tem permissão para fazer isso.' }))
    montar()

    expect(await screen.findByText(/não foi possível carregar os perfis|permissão/i)).toBeInTheDocument()
  })
})
