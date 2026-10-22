import { apiClient } from './client'
import type { FiltroNotasFiscais, NotaFiscal, PaginaDe, RegistrarNotaFiscalRequest } from '../types'

/** Notas recebidas no período (datas inclusivas, "YYYY-MM-DD"; vazias = sem limite). */
export const listarNotasFiscais = async (de?: string, ate?: string): Promise<NotaFiscal[]> => {
  const { data } = await apiClient.get<NotaFiscal[]>('/v1/notas-fiscais', {
    params: { de: de || undefined, ate: ate || undefined },
  })
  return data
}

export const buscarNotaFiscal = async (id: string): Promise<NotaFiscal> => {
  const { data } = await apiClient.get<NotaFiscal>(`/v1/notas-fiscais/${id}`)
  return data
}

export const registrarNotaFiscal = async (payload: RegistrarNotaFiscalRequest): Promise<NotaFiscal> => {
  const { data } = await apiClient.post<NotaFiscal>('/v1/notas-fiscais', payload)
  return data
}

/** Uma página de notas, já filtrada pelo servidor (da recebida mais recentemente para a mais antiga). */
export const pesquisarNotasFiscais = async (filtro: FiltroNotasFiscais): Promise<PaginaDe<NotaFiscal>> => {
  const { data } = await apiClient.get<PaginaDe<NotaFiscal>>('/v1/notas-fiscais/pesquisa', { params: filtro })
  return data
}
