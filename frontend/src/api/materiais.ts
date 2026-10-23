import { apiClient } from './client'
import type { Material, CadastrarMaterialRequest, FiltroMateriais, PaginaDe } from '../types'

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

/** Uma página de materiais, já filtrada pelo servidor (dos cadastrados mais recentemente para os mais antigos). */
export const pesquisarMateriais = async (filtro: FiltroMateriais): Promise<PaginaDe<Material>> => {
  const { data } = await apiClient.get<PaginaDe<Material>>('/v1/materiais/pesquisa', { params: filtro })
  return data
}
