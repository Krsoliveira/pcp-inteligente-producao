import { apiClient } from './client'
import type { ResumoProducao } from '../types'

export const consultarResumoProducao = async (): Promise<ResumoProducao> => {
  const { data } = await apiClient.get<ResumoProducao>('/v1/dashboard/resumo')
  return data
}
