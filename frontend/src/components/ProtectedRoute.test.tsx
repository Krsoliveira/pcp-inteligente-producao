import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '../store/authStore'
import { ProtectedRoute } from './ProtectedRoute'

function TelaDeLogin() {
  const { state } = useLocation()
  return <div data-testid="origem">{(state as { from?: string } | null)?.from ?? 'sem origem'}</div>
}

function montar(rota: string) {
  render(
    <MemoryRouter initialEntries={[rota]}>
      <Routes>
        <Route path="/login" element={<TelaDeLogin />} />
        <Route element={<ProtectedRoute />}>
          <Route path="/lotes/:id" element={<div>Tela do lote</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  )
}

describe('ProtectedRoute', () => {
  beforeEach(() => useAuthStore.setState({ token: null }))

  it('sem login manda para o login lembrando a página pedida, com a busca', () => {
    montar('/lotes/l-1?aba=consumos')

    expect(screen.getByTestId('origem')).toHaveTextContent('/lotes/l-1?aba=consumos')
    expect(screen.queryByText('Tela do lote')).not.toBeInTheDocument()
  })

  it('com login abre a página pedida', () => {
    useAuthStore.setState({ token: 'jwt-123' })

    montar('/lotes/l-1')

    expect(screen.getByText('Tela do lote')).toBeInTheDocument()
  })
})
