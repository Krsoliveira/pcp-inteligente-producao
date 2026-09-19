import { apiClient } from './client'
import type { Lote } from '../types'

export const listarLotes = async (): Promise<Lote[]> => {
  const { data } = await apiClient.get<Lote[]>('/v1/lotes')
  return data
}

export const buscarLotePorId = async (id: string): Promise<Lote> => {
  const { data } = await apiClient.get<Lote>(`/v1/lotes/${id}`)
  return data
}

export const listarLotesPorOrdem = async (ordemId: string): Promise<Lote[]> => {
  const { data } = await apiClient.get<Lote[]>('/v1/lotes', {
    params: { ordemProducaoId: ordemId },
  })
  return data
}
