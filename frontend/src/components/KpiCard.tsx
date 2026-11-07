import { Link as RouterLink } from 'react-router-dom'
import Card from '@mui/material/Card'
import CardActionArea from '@mui/material/CardActionArea'
import CardContent from '@mui/material/CardContent'
import Typography from '@mui/material/Typography'
import Box from '@mui/material/Box'
import { alpha, useTheme, type Palette } from '@mui/material/styles'
import ArrowForwardIcon from '@mui/icons-material/ArrowForward'
import type { SvgIconComponent } from '@mui/icons-material'

/** Tons do tema para o número e o ícone do cartão; nunca uma cor solta (design-system.md). */
export type TomDoKpi = 'primario' | 'sucesso' | 'atencao' | 'erro' | 'neutro'

const COR_DO_TOM: Record<TomDoKpi, (paleta: Palette) => string> = {
  primario: (p) => p.primary.main,
  sucesso: (p) => p.success.main,
  atencao: (p) => p.warning.main,
  erro: (p) => p.error.main,
  neutro: (p) => p.text.secondary,
}

interface KpiCardProps {
  titulo: string
  valor: number | string
  subtitulo?: string
  /** Sem ícone o cartão fica só com o número; use cor apenas para sinalizar (ex.: atrasos). */
  Icone?: SvgIconComponent
  tom?: TomDoKpi
  /** Quando informado, o cartão inteiro vira um link para a tela já filtrada. */
  link?: { para: string; rotulo: string }
}

export function KpiCard({ titulo, valor, subtitulo, Icone, tom, link }: KpiCardProps) {
  const { palette } = useTheme()
  const tomEfetivo = tom ?? (Icone ? 'primario' : undefined)
  const corDoValor = tomEfetivo && COR_DO_TOM[tomEfetivo](palette)

  const conteudo = (
    <CardContent sx={{ p: 3, height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 2 }}>
        <Box sx={{ minWidth: 0 }}>
          <Typography variant="body2" gutterBottom sx={{ color: 'text.secondary' }}>
            {titulo}
          </Typography>
          <Typography variant="h4" component="p" sx={{ color: corDoValor ?? 'text.primary', fontWeight: 700 }}>
            {valor}
          </Typography>
          {subtitulo && (
            <Typography
              variant="caption"
              sx={{ color: 'text.secondary', display: 'block' }}>
              {subtitulo}
            </Typography>
          )}
        </Box>
        {Icone && (
          <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: alpha(corDoValor ?? palette.primary.main, 0.09), display: 'flex', alignItems: 'center', flexShrink: 0 }}>
            <Icone sx={{ color: corDoValor, fontSize: 28 }} />
          </Box>
        )}
      </Box>
      {link && (
        <Box className="kpi-acao" sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mt: 'auto', pt: 1.5, color: 'text.secondary' }}>
          <Typography variant="caption" sx={{ fontWeight: 600 }}>{link.rotulo}</Typography>
          <ArrowForwardIcon sx={{ fontSize: 14 }} />
        </Box>
      )}
    </CardContent>
  )

  return (
    <Card sx={{ height: '100%' }}>
      {link ? (
        <CardActionArea
          component={RouterLink}
          to={link.para}
          sx={{ height: '100%', '&.Mui-focusVisible': { outlineOffset: -2 }, '&:hover .kpi-acao, &:focus-visible .kpi-acao': { color: 'primary.main' } }}
        >
          {conteudo}
        </CardActionArea>
      ) : (
        conteudo
      )}
    </Card>
  )
}
