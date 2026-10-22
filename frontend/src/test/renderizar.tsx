import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'

/** Mostra a rota atual (caminho + query) para os testes conferirem a navegação. */
export function SondaDeRota() {
  const { pathname, search } = useLocation()
  return <div data-testid="rota">{pathname + search}</div>
}

interface Opcoes {
  /** Entradas do histórico; a última é a tela atual. */
  rotas?: string[]
  /** Padrão de rota da tela renderizada (ex.: "/listas-tecnicas/:id"). */
  padrao?: string
}

/** Renderiza uma tela com React Query (sem novas tentativas) e roteador em memória. */
export function renderizarTela(tela: ReactElement, { rotas = ['/'], padrao = '*' }: Opcoes = {}) {
  const cliente = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={cliente}>
      <MemoryRouter initialEntries={rotas} initialIndex={rotas.length - 1}>
        <Routes>
          <Route path={padrao} element={<>{tela}<SondaDeRota /></>} />
          <Route path="*" element={<SondaDeRota />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}
