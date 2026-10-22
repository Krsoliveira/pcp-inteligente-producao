import { apiClient } from './client'
import type { EventoAuditoria, FiltroEventosAuditoria, Pagina } from '../types'

export const consultarEventosAuditoria = async (
  filtro: FiltroEventosAuditoria,
): Promise<Pagina<EventoAuditoria>> => {
  const { data } = await apiClient.get<Pagina<EventoAuditoria>>('/v1/auditoria/eventos', {
    params: filtro,
  })
  return data
}
