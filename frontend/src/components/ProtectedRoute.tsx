import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'

/** Redireciona para /login se o usuário não estiver autenticado, lembrando a página pedida para voltar a ela. */
export function ProtectedRoute() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const { pathname, search } = useLocation()
  return isAuthenticated() ? <Outlet /> : <Navigate to="/login" replace state={{ from: pathname + search }} />
}
