import { apiClient } from './client'
import type { Material, ListaTecnica } from '../types'

export const listarMateriais = async (): Promise<Material[]> => {
  const { data } = await apiClient.get<Material[]>('/v1/materiais')
  return data
}

export const listarListasTecnicas = async (materialId: string): Promise<ListaTecnica[]> => {
  const { data } = await apiClient.get<ListaTecnica[]>('/v1/listas-tecnicas', {
    params: { materialId },
  })
  return data
}
