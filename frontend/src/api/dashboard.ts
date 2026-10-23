import { apiClient } from './client'
import type { AtrasoDoCentro, EntregasDoMes, ResumoProducao } from '../types'

export const consultarResumoProducao = async (): Promise<ResumoProducao> => {
  const { data } = await apiClient.get<ResumoProducao>('/v1/dashboard/resumo')
  return data
}

/** Um item por mês do fim planejado, do mais antigo ao atual (1 a 24 meses). */
export const consultarEntregasPorMes = async (meses: number): Promise<EntregasDoMes[]> => {
  const { data } = await apiClient.get<EntregasDoMes[]>('/v1/dashboard/entregas-por-mes', { params: { meses } })
  return data
}

export const consultarAtrasosPorCentro = async (): Promise<AtrasoDoCentro[]> => {
  const { data } = await apiClient.get<AtrasoDoCentro[]>('/v1/dashboard/atrasos-por-centro')
  return data
}
