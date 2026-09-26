import { apiClient } from './client'
import type { TipoOrdem, CadastrarTipoOrdemRequest } from '../types'

export const listarTiposOrdem = async (): Promise<TipoOrdem[]> => {
  const { data } = await apiClient.get<TipoOrdem[]>('/v1/tipos-ordem')
  return data
}

export const cadastrarTipoOrdem = async (payload: CadastrarTipoOrdemRequest): Promise<TipoOrdem> => {
  const { data } = await apiClient.post<TipoOrdem>('/v1/tipos-ordem', payload)
  return data
}

export const atualizarTipoOrdem = async (
  id: string,
  payload: CadastrarTipoOrdemRequest,
): Promise<TipoOrdem> => {
  const { data } = await apiClient.put<TipoOrdem>(`/v1/tipos-ordem/${id}`, payload)
  return data
}
