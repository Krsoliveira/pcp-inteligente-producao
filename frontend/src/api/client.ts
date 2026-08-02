import axios from 'axios'
import { useAuthStore } from '../store/authStore'

/**
 * Instância Axios compartilhada.
 * - baseURL '/api' é redirecionada para o backend pelo proxy do Vite (dev)
 *   ou pelo reverse proxy em produção.
 * - Interceptor de request: injeta o token JWT em cada chamada autenticada.
 * - Interceptor de response: em caso de 401, desloga e redireciona para /login.
 */
export const apiClient = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      useAuthStore.getState().logout()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  },
)
