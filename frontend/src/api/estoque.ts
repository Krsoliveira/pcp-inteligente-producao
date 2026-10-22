import { apiClient } from './client'
import type { FiltroEstoque, PaginaDe, PosicaoEstoque, ResumoEstoque } from '../types'

export const consultarEstoque = async (): Promise<PosicaoEstoque[]> => {
  const { data } = await apiClient.get<PosicaoEstoque[]>('/v1/estoque')
  return data
}

/** Uma página do estoque, já filtrada pelo servidor (entrada mais recente primeiro). */
export const pesquisarEstoque = async (filtro: FiltroEstoque): Promise<PaginaDe<PosicaoEstoque>> => {
  const { data } = await apiClient.get<PaginaDe<PosicaoEstoque>>('/v1/estoque/pesquisa', { params: filtro })
  return data
}

export const consultarResumoEstoque = async (): Promise<ResumoEstoque> => {
  const { data } = await apiClient.get<ResumoEstoque>('/v1/estoque/resumo')
  return data
}
