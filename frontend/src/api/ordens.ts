import { apiClient } from './client'
import type {
  OrdemProducao,
  CriarOrdemRequest,
  ConcluirOrdemRequest,
  StatusOrdem,
  Lote,
} from '../types'

interface ConcluirOrdemResponse {
  ordem: OrdemProducao
  lote: Lote
}

export const listarOrdens = async (): Promise<OrdemProducao[]> => {
  const { data } = await apiClient.get<OrdemProducao[]>('/v1/ordens-producao')
  return data
}

export const buscarOrdemPorId = async (id: string): Promise<OrdemProducao> => {
  const { data } = await apiClient.get<OrdemProducao>(`/v1/ordens-producao/${id}`)
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

export const concluirOrdem = async (
  id: string,
  payload: ConcluirOrdemRequest,
): Promise<ConcluirOrdemResponse> => {
  const { data } = await apiClient.post<ConcluirOrdemResponse>(
    `/v1/ordens-producao/${id}/concluir`,
    payload,
  )
  return data
}
