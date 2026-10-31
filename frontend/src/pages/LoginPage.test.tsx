import { act, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { login } from '../api/auth'
import { useAuthStore } from '../store/authStore'
import { erroDeRede, erroHttp } from '../test/erros'
import { renderizarTela } from '../test/renderizar'
import { LoginPage } from './LoginPage'

vi.mock('../api/auth', () => ({ login: vi.fn(), registrar: vi.fn() }))

const loginMock = vi.mocked(login)

/** `origem` é a página que a pessoa tentou abrir antes de ser mandada ao login (estado da rota). */
function montar(origem?: unknown) {
  // Com os temporizadores simulados, o relógio do usuário simulado precisa avançar junto.
  const usuario = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
  const rota = origem === undefined ? '/login' : { pathname: '/login', state: { from: origem } }
  renderizarTela(<LoginPage />, { rotas: [rota], padrao: '/login' })
  return usuario
}

async function entrar(usuario: ReturnType<typeof userEvent.setup>) {
  await usuario.type(screen.getByLabelText('E-mail'), 'ana@pcp.com')
  await usuario.type(screen.getByLabelText('Senha'), 'Senha@123')
  await usuario.click(screen.getByRole('button', { name: 'Entrar' }))
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    loginMock.mockReset()
    useAuthStore.setState({ token: null })
  })
  afterEach(() => vi.useRealTimers())

  it('pede para preencher e-mail e senha sem chamar o sistema', async () => {
    const usuario = montar()

    await usuario.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(screen.getByText('Preencha e-mail e senha.')).toBeInTheDocument()
    expect(loginMock).not.toHaveBeenCalled()
  })

  it('guarda o token e vai para o dashboard quando o login dá certo', async () => {
    loginMock.mockResolvedValue({ token: 'jwt-123' } as Awaited<ReturnType<typeof login>>)
    const usuario = montar()

    await entrar(usuario)

    await waitFor(() => expect(screen.getByTestId('rota')).toHaveTextContent('/dashboard'))
    expect(useAuthStore.getState().token).toBe('jwt-123')
  })

  it('volta para a página que a pessoa tentou abrir (ex.: o QR code de um lote)', async () => {
    loginMock.mockResolvedValue({ token: 'jwt-123' } as Awaited<ReturnType<typeof login>>)
    const usuario = montar('/lotes/l-1?aba=consumos')

    await entrar(usuario)

    await waitFor(() => expect(screen.getByTestId('rota')).toHaveTextContent('/lotes/l-1?aba=consumos'))
  })

  it.each([
    ['endereço de outro site', 'https://malicioso.example/login'],
    ['caminho sem barra no início', 'lotes/l-1'],
    ['endereço com duas barras (outro site)', '//malicioso.example'],
    ['barra invertida (outro site em alguns navegadores)', '/\\malicioso.example'],
    ['a própria tela de login', '/login'],
    ['valor que não é texto', { pathname: '/lotes/l-1' }],
  ])('origem inválida (%s) vai para o dashboard', async (_caso, origem) => {
    loginMock.mockResolvedValue({ token: 'jwt-123' } as Awaited<ReturnType<typeof login>>)
    const usuario = montar(origem)

    await entrar(usuario)

    await waitFor(() => expect(screen.getByTestId('rota')).toHaveTextContent(/^\/dashboard$/))
  })

  it('avisa e-mail ou senha incorretos no 401', async () => {
    loginMock.mockRejectedValue(erroHttp(401))
    const usuario = montar()

    await entrar(usuario)

    expect(await screen.findByText('E-mail ou senha incorretos. Tente novamente.')).toBeInTheDocument()
  })

  it('explica a falta de conexão sem citar termos técnicos', async () => {
    loginMock.mockRejectedValue(erroDeRede())
    const usuario = montar()

    await entrar(usuario)

    const alerta = await screen.findByRole('alert')
    expect(alerta).toHaveTextContent(/não foi possível conectar ao sistema/i)
    expect(alerta).not.toHaveTextContent(/backend|servidor/i)
  })

  it('avisa a indisponibilidade do sistema no erro 5xx', async () => {
    loginMock.mockRejectedValue(erroHttp(503))
    const usuario = montar()

    await entrar(usuario)

    expect(await screen.findByText(/temporariamente indisponível/i)).toBeInTheDocument()
  })

  it('no excesso de tentativas mostra a contagem regressiva e bloqueia o botão até zerar', async () => {
    loginMock.mockRejectedValue(erroHttp(429, {}, { 'retry-after': '3' }))
    const usuario = montar()

    await entrar(usuario)

    expect(await screen.findByText(/aguarde 3 segundos/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeDisabled()

    act(() => { vi.advanceTimersByTime(1000) })
    expect(await screen.findByText(/aguarde 2 segundos/i)).toBeInTheDocument()

    act(() => { vi.advanceTimersByTime(1000) })
    expect(await screen.findByText(/aguarde 1 segundo para/i)).toBeInTheDocument()

    act(() => { vi.advanceTimersByTime(1000) })
    await waitFor(() => expect(screen.queryByText(/aguarde/i)).not.toBeInTheDocument())
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeEnabled()
  })

  it('não envia o formulário enquanto a espera não terminar', async () => {
    loginMock.mockRejectedValue(erroHttp(429, {}, { 'retry-after': '30' }))
    const usuario = montar()
    await entrar(usuario)
    await screen.findByText(/aguarde 30 segundos/i)
    loginMock.mockClear()

    await usuario.type(screen.getByLabelText('Senha'), '{Enter}')

    expect(loginMock).not.toHaveBeenCalled()
  })
})
