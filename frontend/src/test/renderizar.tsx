import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { CHAVE_USUARIO_LOGADO } from '../hooks/usePermissoes'
import type { Permissao } from '../types'
import { TODAS_AS_PERMISSOES, usuarioLogado } from './fabricas'

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
  /**
   * Permissões do usuário logado; sem informar, ele tem todas (administrador). `null` não pré-carrega
   * nada: a tela busca "quem sou eu" como no uso real (o teste então mocka a API).
   */
  permissoes?: Permissao[] | null
}

/** Renderiza uma tela com React Query (sem novas tentativas) e roteador em memória. */
export function renderizarTela(tela: ReactElement, { rotas = ['/'], padrao = '*', permissoes = TODAS_AS_PERMISSOES }: Opcoes = {}) {
  const cliente = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  // O usuário logado já vem carregado: as telas decidem o que mostrar sem uma chamada a mais.
  if (permissoes !== null) cliente.setQueryData(CHAVE_USUARIO_LOGADO, usuarioLogado({ permissoes }))
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
