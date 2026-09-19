import Box from '@mui/material/Box'
import Chip from '@mui/material/Chip'
import type { StatusOrdem } from '../types'

// ---- Ordem de Produção ----

const ORDEM_CONFIG: Record<StatusOrdem, { label: string; color: string; bg: string }> = {
  PLANEJADA:   { label: 'Planejada',   color: '#1565c0', bg: '#e3f2fd' },
  LIBERADA:    { label: 'Liberada',    color: '#ed6c02', bg: '#fff3e0' },
  EM_PRODUCAO: { label: 'Em Produção', color: '#7b1fa2', bg: '#f3e5f5' },
  CONCLUIDA:   { label: 'Concluída',   color: '#2e7d32', bg: '#e8f5e9' },
  CANCELADA:   { label: 'Cancelada',   color: '#c62828', bg: '#ffebee' },
}

interface StatusOrdemBadgeProps {
  status: StatusOrdem
  size?: 'small' | 'medium'
}

export function StatusOrdemBadge({ status, size = 'small' }: StatusOrdemBadgeProps) {
  const cfg = ORDEM_CONFIG[status]
  return (
    <Box
      component="span"
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 0.6,
        px: size === 'small' ? 1 : 1.5,
        py: size === 'small' ? 0.25 : 0.5,
        borderRadius: 99,
        bgcolor: cfg.bg,
        color: cfg.color,
        fontSize: size === 'small' ? '0.7rem' : '0.8rem',
        fontWeight: 600,
        letterSpacing: '0.02em',
        whiteSpace: 'nowrap',
      }}
    >
      <Box
        component="span"
        sx={{ width: 6, height: 6, borderRadius: '50%', bgcolor: cfg.color, flexShrink: 0 }}
      />
      {cfg.label}
    </Box>
  )
}

// ---- Lista Técnica ----

type StatusLista = 'EM_REVISAO' | 'ATIVA' | 'OBSOLETA'

const LISTA_CONFIG: Record<StatusLista, { label: string; color: string; bg: string }> = {
  EM_REVISAO: { label: 'Em Revisão', color: '#ed6c02', bg: '#fff3e0' },
  ATIVA:      { label: 'Ativa',      color: '#2e7d32', bg: '#e8f5e9' },
  OBSOLETA:   { label: 'Obsoleta',   color: '#757575', bg: '#f5f5f5' },
}

export function StatusListaBadge({ status }: { status: StatusLista }) {
  const cfg = LISTA_CONFIG[status]
  return (
    <Chip
      label={cfg.label}
      size="small"
      sx={{ bgcolor: cfg.bg, color: cfg.color, fontWeight: 600, border: 'none', fontSize: '0.72rem' }}
    />
  )
}

// ---- Lote ----

type StatusLote = 'DISPONIVEL' | 'BLOQUEADO' | 'CONSUMIDO' | 'VENCIDO'

const LOTE_CONFIG: Record<StatusLote, { label: string; color: string; bg: string }> = {
  DISPONIVEL: { label: 'Disponível', color: '#2e7d32', bg: '#e8f5e9' },
  BLOQUEADO:  { label: 'Bloqueado',  color: '#ed6c02', bg: '#fff3e0' },
  CONSUMIDO:  { label: 'Consumido',  color: '#757575', bg: '#f5f5f5' },
  VENCIDO:    { label: 'Vencido',    color: '#c62828', bg: '#ffebee' },
}

export function StatusLoteBadge({ status }: { status: StatusLote }) {
  const cfg = LOTE_CONFIG[status]
  return (
    <Box
      component="span"
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 0.6,
        px: 1,
        py: 0.25,
        borderRadius: 99,
        bgcolor: cfg.bg,
        color: cfg.color,
        fontSize: '0.7rem',
        fontWeight: 600,
        whiteSpace: 'nowrap',
      }}
    >
      <Box
        component="span"
        sx={{ width: 6, height: 6, borderRadius: '50%', bgcolor: cfg.color, flexShrink: 0 }}
      />
      {cfg.label}
    </Box>
  )
}

// ---- Tipo de Ordem (badge colorido conforme cadastro) ----

interface TipoOrdemBadgeProps {
  nome: string
  cor: string
}

export function TipoOrdemBadge({ nome, cor }: TipoOrdemBadgeProps) {
  return (
    <Box
      component="span"
      sx={{
        display: 'inline-block',
        px: 1.25,
        py: 0.25,
        borderRadius: 99,
        bgcolor: `${cor}18`,
        color: cor,
        fontSize: '0.7rem',
        fontWeight: 600,
        border: `1px solid ${cor}40`,
        whiteSpace: 'nowrap',
      }}
    >
      {nome}
    </Box>
  )
}
