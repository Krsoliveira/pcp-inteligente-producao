import Link from '@mui/material/Link'
import type { ReactNode } from 'react'
import { Link as RouterLink } from 'react-router-dom'

interface LinkDaLinhaProps {
  para: string
  /** Para códigos e números, que alinham melhor em fonte de largura fixa. */
  monoespacado?: boolean
  children: ReactNode
}

/**
 * O identificador de uma linha clicável. O clique na linha é só um atalho para o mouse: é este link
 * que abre o registro por teclado e leitor de tela, e que permite abrir em outra aba.
 */
export function LinkDaLinha({ para, monoespacado = false, children }: LinkDaLinhaProps) {
  return (
    <Link
      component={RouterLink}
      to={para}
      variant="body2"
      underline="hover"
      noWrap
      onClick={(e) => e.stopPropagation()}
      sx={{ display: 'block', fontWeight: 600, fontFamily: monoespacado ? 'monospace' : undefined }}
    >
      {children}
    </Link>
  )
}
