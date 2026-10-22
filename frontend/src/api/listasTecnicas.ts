import { apiClient } from './client'
import type { ListaTecnica, CadastrarListaTecnicaRequest, FiltroListasTecnicas, PaginaDe } from '../types'

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

/** Uma página de listas técnicas, já filtrada pelo servidor (das criadas mais recentemente para as mais antigas). */
export const pesquisarListasTecnicas = async (filtro: FiltroListasTecnicas): Promise<PaginaDe<ListaTecnica>> => {
  const { data } = await apiClient.get<PaginaDe<ListaTecnica>>('/v1/listas-tecnicas/pesquisa', { params: filtro })
  return data
}
