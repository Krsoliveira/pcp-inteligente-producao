import { apiClient } from './client'
import type { LoginRequest, RegistrarRequest, TokenResponse } from '../types'

export const login = async (payload: LoginRequest): Promise<TokenResponse> => {
  const { data } = await apiClient.post<TokenResponse>('/v1/auth/login', payload)
  return data
}

export const registrar = async (payload: RegistrarRequest): Promise<void> => {
  await apiClient.post('/v1/auth/registrar', payload)
}
