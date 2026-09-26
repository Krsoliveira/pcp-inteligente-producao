import { apiClient } from './client'
import type { Material, CadastrarMaterialRequest } from '../types'

export const listarMateriais = async (): Promise<Material[]> => {
  const { data } = await apiClient.get<Material[]>('/v1/materiais')
  return data
}

export const buscarMaterialPorId = async (id: string): Promise<Material> => {
  const { data } = await apiClient.get<Material>(`/v1/materiais/${id}`)
  return data
}

export const cadastrarMaterial = async (payload: CadastrarMaterialRequest): Promise<void> => {
  await apiClient.post('/v1/materiais', payload)
}
