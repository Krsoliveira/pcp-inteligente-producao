import { apiClient } from './client'
import type { LoginRequest, TokenResponse } from '../types'

export const login = async (payload: LoginRequest): Promise<TokenResponse> => {
  const { data } = await apiClient.post<TokenResponse>('/v1/auth/login', payload)
  return data
}
