import { screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { renderizarTela } from '../../test/renderizar'
import { RotaDoAutocadastro } from './RotaDoAutocadastro'

vi.mock('../../api/auth', () => ({ login: vi.fn(), registrar: vi.fn() }))

describe('RotaDoAutocadastro', () => {
  afterEach(() => vi.unstubAllEnvs())

  it('mostra o cadastro quando o autocadastro está ligado', () => {
    renderizarTela(<RotaDoAutocadastro />, { rotas: ['/cadastro'], padrao: '/cadastro' })

    expect(screen.getByRole('button', { name: /criar conta/i })).toBeInTheDocument()
  })

  it('não mostra o cadastro quando o autocadastro está desligado', () => {
    vi.stubEnv('VITE_AUTOCADASTRO', 'false')
    renderizarTela(<RotaDoAutocadastro />, { rotas: ['/cadastro'], padrao: '/cadastro' })

    expect(screen.queryByRole('button', { name: /criar conta/i })).not.toBeInTheDocument()
  })
})
