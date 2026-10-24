import { apiClient } from './client'
import type { ConsumoMaterial, LancarConsumoRequest, RegistrarConsumoRequest } from '../types'

export const listarConsumosPorOrdem = async (ordemId: string): Promise<ConsumoMaterial[]> => {
  const { data } = await apiClient.get<ConsumoMaterial[]>(
    `/v1/ordens-producao/${ordemId}/consumos`,
  )
  return data
}

export const registrarConsumo = async (
  ordemId: string,
  consumoId: string,
  payload: RegistrarConsumoRequest,
): Promise<ConsumoMaterial> => {
  const { data } = await apiClient.patch<ConsumoMaterial>(
    `/v1/ordens-producao/${ordemId}/consumos/${consumoId}`,
    payload,
  )
  return data
}

/** Lança de uma vez o consumo de linhas da lista e de itens fora dela; se qualquer linha for recusada, nada é lançado. */
export const lancarConsumo = async (ordemId: string, payload: LancarConsumoRequest): Promise<ConsumoMaterial[]> => {
  const { data } = await apiClient.post<ConsumoMaterial[]>(`/v1/ordens-producao/${ordemId}/consumos/lancamento`, payload)
  return data
}
