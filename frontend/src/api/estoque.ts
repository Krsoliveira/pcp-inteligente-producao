import { apiClient } from './client'
import type { PosicaoEstoque } from '../types'

export const consultarEstoque = async (): Promise<PosicaoEstoque[]> => {
  const { data } = await apiClient.get<PosicaoEstoque[]>('/v1/estoque')
  return data
}
