import { apiClient } from './client'
import type { ConsumoMaterial, RegistrarConsumoRequest } from '../types'

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
