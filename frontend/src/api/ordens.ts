import { apiClient } from './client'
import type { OrdemProducao, CriarOrdemRequest, StatusOrdem } from '../types'

export const listarOrdens = async (): Promise<OrdemProducao[]> => {
  const { data } = await apiClient.get<OrdemProducao[]>('/v1/ordens-producao')
  return data
}

export const criarOrdem = async (payload: CriarOrdemRequest): Promise<OrdemProducao> => {
  const { data } = await apiClient.post<OrdemProducao>('/v1/ordens-producao', payload)
  return data
}

export const atualizarStatus = async (
  id: string,
  status: StatusOrdem,
): Promise<OrdemProducao> => {
  const { data } = await apiClient.patch<OrdemProducao>(
    `/v1/ordens-producao/${id}/status`,
    { status },
  )
  return data
}
