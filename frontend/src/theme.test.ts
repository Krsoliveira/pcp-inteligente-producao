import { getContrastRatio } from '@mui/material/styles'
import { describe, expect, it } from 'vitest'
import { theme } from './theme'

const AA_TEXTO = 4.5
const { paper, default: fundoDaPagina } = theme.palette.background

describe('tema: contraste de texto (WCAG 1.4.3)', () => {
  it.each([
    ['primary', theme.palette.primary.main],
    ['success', theme.palette.success.main],
    ['warning', theme.palette.warning.main],
    ['error', theme.palette.error.main],
    ['info', theme.palette.info.main],
    ['texto secundário', theme.palette.text.secondary],
  ])('%s é legível como texto, nos dois fundos', (_nome, cor) => {
    expect(getContrastRatio(cor, paper)).toBeGreaterThanOrEqual(AA_TEXTO)
    expect(getContrastRatio(cor, fundoDaPagina)).toBeGreaterThanOrEqual(AA_TEXTO)
  })

  it.each(['primary', 'success', 'warning', 'error', 'info'] as const)('%s preenchido aceita texto branco', (cor) => {
    expect(getContrastRatio(theme.palette[cor].main, theme.palette[cor].contrastText)).toBeGreaterThanOrEqual(AA_TEXTO)
  })
})
