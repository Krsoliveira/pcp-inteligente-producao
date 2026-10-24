import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { erroHttp } from '../test/erros'
import { renderizarTela } from '../test/renderizar'
import { TrocarSenhaPage, validarNovaSenha } from './TrocarSenhaPage'

// Função simples (não vi.fn) para a falha: no Vitest 4 a promessa rejeitada de um vi.fn escapa e derruba o teste.
let rejeitarCom: unknown = null
const trocarMock = vi.fn()
vi.mock('../api/acessos', () => ({
  trocarMinhaSenha: (...args: unknown[]) => {
    trocarMock(...args)
    return rejeitarCom ? Promise.reject(rejeitarCom) : Promise.resolve()
  },
  consultarUsuarioLogado: vi.fn(),
}))

function montar(obrigatoria: boolean) {
  return renderizarTela(<TrocarSenhaPage />, { rotas: ['/minha-senha'], padrao: '/minha-senha', usuario: { deveTrocarSenha: obrigatoria } })
}

async function preencher(atual: string, nova: string, confirmacao: string) {
  await userEvent.type(screen.getByLabelText(/^Senha (atual|provisória)/), atual)
  await userEvent.type(screen.getByLabelText('Nova senha'), nova)
  await userEvent.type(screen.getByLabelText('Confirmar nova senha'), confirmacao)
}

describe('validarNovaSenha', () => {
  it('aceita uma senha válida', () => {
    expect(validarNovaSenha('atual1234', 'novaSenha1', 'novaSenha1')).toBeNull()
  })

  it('recusa na ordem: atual vazia, nova curta, longa demais, igual à atual e confirmação diferente', () => {
    expect(validarNovaSenha('', 'novaSenha1', 'novaSenha1')).toMatch(/senha atual/i)
    expect(validarNovaSenha('atual1234', 'curta', 'curta')).toMatch(/no mínimo 8/)
    expect(validarNovaSenha('atual1234', 'a'.repeat(73), 'a'.repeat(73))).toMatch(/longa demais/i)
    expect(validarNovaSenha('atual1234', 'atual1234', 'atual1234')).toMatch(/diferente da atual/)
    expect(validarNovaSenha('atual1234', 'novaSenha1', 'outraSenha1')).toMatch(/confirmação/i)
  })

  it('conta bytes, não caracteres: 37 letras acentuadas passam de 72 bytes', () => {
    expect(validarNovaSenha('atual1234', 'é'.repeat(37), 'é'.repeat(37))).toMatch(/longa demais/i)
    expect(validarNovaSenha('atual1234', 'é'.repeat(36), 'é'.repeat(36))).toBeNull()
  })
})

describe('TrocarSenhaPage', () => {
  beforeEach(() => { trocarMock.mockReset(); rejeitarCom = null })

  it('no primeiro acesso explica que a senha é provisória e pede a provisória como senha atual', () => {
    montar(true)

    expect(screen.getByRole('heading', { name: 'Defina a sua senha' })).toBeInTheDocument()
    expect(screen.getByText(/provisória e é conhecida por quem criou a sua conta/i)).toBeInTheDocument()
    expect(screen.getByLabelText('Senha provisória (a que você recebeu)')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Definir senha' })).toBeInTheDocument()
  })

  it('depois do primeiro acesso é "Alterar minha senha", sem o aviso da provisória', () => {
    montar(false)

    expect(screen.getByRole('heading', { name: 'Alterar minha senha' })).toBeInTheDocument()
    expect(screen.queryByText(/provisória e é conhecida/i)).not.toBeInTheDocument()
    expect(screen.getByLabelText('Senha atual')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Alterar senha' })).toBeInTheDocument()
  })

  it('envia a senha atual e a nova; no primeiro acesso volta ao início', async () => {
    montar(true)

    await preencher('provisoria1', 'minhaNovaSenha', 'minhaNovaSenha')
    await userEvent.click(screen.getByRole('button', { name: 'Definir senha' }))

    await waitFor(() => expect(trocarMock).toHaveBeenCalled())
    expect(trocarMock.mock.calls[0].slice(0, 2)).toEqual(['provisoria1', 'minhaNovaSenha'])
    await waitFor(() => expect(screen.getByTestId('rota')).toHaveTextContent(/^\/$/))
  })

  it('alterar voluntariamente fica na tela, avisa "Senha alterada" e limpa os campos', async () => {
    montar(false)

    await preencher('senhaAtual1', 'minhaNovaSenha', 'minhaNovaSenha')
    await userEvent.click(screen.getByRole('button', { name: 'Alterar senha' }))

    expect(await screen.findByText('Senha alterada.')).toBeInTheDocument()
    expect(screen.getByTestId('rota')).toHaveTextContent('/minha-senha')
    expect(screen.getByLabelText('Nova senha')).toHaveValue('')
  })

  it('não envia com dados inválidos e diz o motivo', async () => {
    montar(false)

    await preencher('senhaAtual1', 'curta', 'curta')
    await userEvent.click(screen.getByRole('button', { name: 'Alterar senha' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/no mínimo 8 caracteres/i)
    expect(trocarMock).not.toHaveBeenCalled()
  })

  it('mostra o motivo do servidor quando a senha atual não confere e mantém o que foi digitado', async () => {
    rejeitarCom = erroHttp(422, { detail: 'A senha atual não confere.' })
    montar(false)

    await preencher('errada12345', 'minhaNovaSenha', 'minhaNovaSenha')
    await userEvent.click(screen.getByRole('button', { name: 'Alterar senha' }))

    expect(await screen.findByText('A senha atual não confere.')).toBeInTheDocument()
    expect(screen.getByLabelText('Nova senha')).toHaveValue('minhaNovaSenha')
  })
})
