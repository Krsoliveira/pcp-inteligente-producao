import { Link as RouterLink } from 'react-router-dom'
import Card from '@mui/material/Card'
import CardActionArea from '@mui/material/CardActionArea'
import CardContent from '@mui/material/CardContent'
import Typography from '@mui/material/Typography'
import Box from '@mui/material/Box'
import { useTheme, type Palette } from '@mui/material/styles'
import ArrowForwardIcon from '@mui/icons-material/ArrowForward'

/** Tons do tema para sinalizar o número do cartão; nunca uma cor solta (design-system.md). */
export type TomDoKpi = 'sucesso' | 'atencao' | 'erro' | 'neutro'

const COR_DO_TOM: Record<TomDoKpi, (paleta: Palette) => string> = {
  sucesso: (p) => p.success.main,
  atencao: (p) => p.warning.main,
  erro: (p) => p.error.main,
  neutro: (p) => p.text.secondary,
}

interface KpiCardProps {
  titulo: string
  valor: number | string
  subtitulo?: string
  /** O número fica em text.primary; o tom só sinaliza (ex.: atrasos, saldo indisponível). */
  tom?: TomDoKpi
  /** Quando informado, o cartão inteiro vira um link para a tela já filtrada. */
  link?: { para: string; rotulo: string }
}

export function KpiCard({ titulo, valor, subtitulo, tom, link }: KpiCardProps) {
  const { palette } = useTheme()
  const corDoValor = tom && COR_DO_TOM[tom](palette)

  const conteudo = (
    <CardContent sx={{ p: 3, height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ minWidth: 0 }}>
        <Typography variant="body2" gutterBottom sx={{ color: 'text.secondary' }}>
          {titulo}
        </Typography>
        <Typography variant="h4" component="p" sx={{ color: corDoValor ?? 'text.primary', fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}>
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
