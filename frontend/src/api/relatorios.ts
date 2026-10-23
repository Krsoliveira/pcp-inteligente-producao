import { apiClient } from './client'
import type { RelatorioConsumoDeOrdens } from '../types'

/** Ordens concluídas com fim planejado entre as datas (AAAA-MM-DD, inclusivas, até 366 dias). */
export const consultarConsumoDeOrdens = async (de: string, ate: string): Promise<RelatorioConsumoDeOrdens> => {
  const { data } = await apiClient.get<RelatorioConsumoDeOrdens>('/v1/relatorios/consumo-de-ordens', { params: { de, ate } })
  return data
}
