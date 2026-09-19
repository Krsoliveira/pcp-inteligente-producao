import { apiClient } from './client'
import type { ListaTecnica, CadastrarListaTecnicaRequest } from '../types'

export const listarListasTecnicas = async (materialId?: string): Promise<ListaTecnica[]> => {
  const { data } = await apiClient.get<ListaTecnica[]>('/v1/listas-tecnicas', {
    params: materialId ? { materialId } : undefined,
  })
  return data
}

export const buscarListaTecnicaPorId = async (id: string): Promise<ListaTecnica> => {
  const { data } = await apiClient.get<ListaTecnica>(`/v1/listas-tecnicas/${id}`)
  return data
}

export const cadastrarListaTecnica = async (
  payload: CadastrarListaTecnicaRequest,
): Promise<void> => {
  await apiClient.post('/v1/listas-tecnicas', payload)
}

export const ativarListaTecnica = async (id: string): Promise<void> => {
  await apiClient.patch(`/v1/listas-tecnicas/${id}/ativar`)
}
