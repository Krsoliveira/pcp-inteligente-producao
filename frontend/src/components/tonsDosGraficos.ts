/**
 * Tons vivos de ponto de status e de barra de gráfico. Não são texto, então não precisam do contraste 4,5:1 dos tons do
 * tema (design-system.md). Ficam num lugar só para o mesmo status ter a mesma cor no selo e no gráfico; os gráficos
 * ECharts não leem o tema.
 */
export const TOM_VIVO = {
  neutro: '#94A3B8',
  informacao: '#2563EB',
  atencao: '#D97706',
  sucesso: '#059669',
  erro: '#DC2626',
} as const

/** Linhas de grade dos gráficos ECharts. */
export const COR_DA_GRADE_DO_GRAFICO = '#E2E8F0'
