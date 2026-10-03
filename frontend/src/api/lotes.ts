import { apiClient } from './client'
import type { Lote, RastreabilidadeLote } from '../types'

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

/** Lotes que podem ser alocados a um consumo do material (disponíveis, com saldo, válidos), em ordem FEFO. */
export const listarLotesDisponiveis = async (materialId: string): Promise<Lote[]> => {
  const { data } = await apiClient.get<Lote[]>('/v1/lotes', {
    params: { materialId, disponiveis: true },
  })
  return data
}

/** Todos os lotes do material, de qualquer status (mais recentes primeiro). */
export const listarLotesPorMaterial = async (materialId: string): Promise<Lote[]> => {
  const { data } = await apiClient.get<Lote[]>('/v1/lotes', { params: { materialId } })
  return data
}

export const rastrearLote = async (id: string): Promise<RastreabilidadeLote> => {
  const { data } = await apiClient.get<RastreabilidadeLote>(`/v1/lotes/${id}/rastreabilidade`)
  return data
}
