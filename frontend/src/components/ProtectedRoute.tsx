import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'

/** Redireciona para /login se o usuário não estiver autenticado. */
export function ProtectedRoute() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  return isAuthenticated() ? <Outlet /> : <Navigate to="/login" replace />
}
