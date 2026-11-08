import Box from '@mui/material/Box'
import Chip from '@mui/material/Chip'
import type { StatusOrdem } from '../types'
import { TOM_VIVO } from './tonsDosGraficos'

// ---- Ordem de produção ----

const ORDEM_CONFIG: Record<StatusOrdem, { label: string; dot: string; bg: string; color: string }> = {
  PLANEJADA:   { label: 'Planejada',   dot: TOM_VIVO.neutro, bg: 'rgba(148,163,184,0.12)', color: '#475569' },
  LIBERADA:    { label: 'Liberada',    dot: TOM_VIVO.informacao, bg: 'rgba(37,99,235,0.10)',   color: '#1D4ED8' },
  EM_PRODUCAO: { label: 'Em produção', dot: TOM_VIVO.atencao, bg: 'rgba(217,119,6,0.10)',   color: '#B45309' },
  CONCLUIDA:   { label: 'Concluída',   dot: TOM_VIVO.sucesso, bg: 'rgba(5,150,105,0.10)',   color: '#047857' },
  CANCELADA:   { label: 'Cancelada',   dot: TOM_VIVO.erro, bg: 'rgba(220,38,38,0.10)',   color: '#B91C1C' },
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
        sx={{ width: 6, height: 6, borderRadius: '50%', bgcolor: cfg.dot, flexShrink: 0 }}
      />
      {cfg.label}
    </Box>
  )
}

// ---- Lista técnica ----

type StatusLista = 'EM_REVISAO' | 'ATIVA' | 'OBSOLETA'

const LISTA_CONFIG: Record<StatusLista, { label: string; dot: string; bg: string; color: string }> = {
  EM_REVISAO: { label: 'Em revisão', dot: TOM_VIVO.atencao, bg: 'rgba(217,119,6,0.10)',  color: '#B45309' },
  ATIVA:      { label: 'Ativa',      dot: TOM_VIVO.sucesso, bg: 'rgba(5,150,105,0.10)', color: '#047857' },
  OBSOLETA:   { label: 'Obsoleta',   dot: TOM_VIVO.neutro, bg: 'rgba(148,163,184,0.12)', color: '#475569' },
}

export function StatusListaBadge({ status }: { status: StatusLista }) {
  const cfg = LISTA_CONFIG[status]
  return (
    <Chip
      label={cfg.label}
      size="small"
      sx={{
        bgcolor: cfg.bg,
        color: cfg.color,
        fontWeight: 600,
        border: 'none',
        fontSize: '0.72rem',
      }}
    />
  )
}

// ---- Lote ----

type StatusLote = 'DISPONIVEL' | 'BLOQUEADO' | 'CONSUMIDO' | 'VENCIDO'

const LOTE_CONFIG: Record<StatusLote, { label: string; dot: string; bg: string; color: string }> = {
  DISPONIVEL: { label: 'Disponível', dot: TOM_VIVO.sucesso, bg: 'rgba(5,150,105,0.10)',   color: '#047857' },
  BLOQUEADO:  { label: 'Bloqueado',  dot: TOM_VIVO.atencao, bg: 'rgba(217,119,6,0.10)',   color: '#B45309' },
  CONSUMIDO:  { label: 'Consumido',  dot: TOM_VIVO.neutro, bg: 'rgba(148,163,184,0.12)', color: '#475569' },
  VENCIDO:    { label: 'Vencido',    dot: TOM_VIVO.erro, bg: 'rgba(220,38,38,0.10)',   color: '#B91C1C' },
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
        sx={{ width: 6, height: 6, borderRadius: '50%', bgcolor: cfg.dot, flexShrink: 0 }}
      />
      {cfg.label}
    </Box>
  )
}

// ---- Situação do usuário ----

const SITUACAO_USUARIO_CONFIG: Record<'ATIVO' | 'DESATIVADO', { label: string; dot: string; bg: string; color: string }> = {
  ATIVO:      { label: 'Ativo',      dot: TOM_VIVO.sucesso, bg: 'rgba(5,150,105,0.10)',   color: '#047857' },
  DESATIVADO: { label: 'Desativado', dot: TOM_VIVO.neutro,  bg: 'rgba(148,163,184,0.12)', color: '#475569' },
}

export function SituacaoUsuarioBadge({ ativo }: { ativo: boolean }) {
  const cfg = SITUACAO_USUARIO_CONFIG[ativo ? 'ATIVO' : 'DESATIVADO']
  return (
    <Box component="span" sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.6, px: 1, py: 0.25, borderRadius: 99,
      bgcolor: cfg.bg, color: cfg.color, fontSize: '0.7rem', fontWeight: 600, whiteSpace: 'nowrap' }}>
      <Box component="span" sx={{ width: 6, height: 6, borderRadius: '50%', bgcolor: cfg.dot, flexShrink: 0 }} />
      {cfg.label}
    </Box>
  )
}

// ---- Tipo de ordem (badge colorido conforme cadastro) ----

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
        // A cor do tipo é escolhida pelo usuário e nem toda dá contraste como texto: ela fica no fundo e na borda.
        color: 'text.primary',
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

/** Rótulo de qualquer status pela constante que vem do servidor. */
export const ROTULOS_DE_STATUS: Record<string, string> = Object.fromEntries(
  [ORDEM_CONFIG, LISTA_CONFIG, LOTE_CONFIG].flatMap((config) => Object.entries(config).map(([status, { label }]) => [status, label])),
)

/** Texto do status como aparece na tela (usado pelo filtro por coluna). */
export const rotuloDoStatusDeOrdem = (status: StatusOrdem) => ORDEM_CONFIG[status].label
export const rotuloDoStatusDeLista = (status: StatusLista) => LISTA_CONFIG[status].label
