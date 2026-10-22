import Grid from '@mui/material/Grid2'
import Typography from '@mui/material/Typography'
import type { ReactNode } from 'react'

/** Linha "rótulo: valor" dos cards de detalhe. Deve ficar dentro de um Grid container. */
export function InfoRow({ label, children }: { label: string; children: ReactNode }) {
  return (
    <>
      <Grid size={{ xs: 12, sm: 5 }}>
        <Typography variant="caption" color="text.secondary" fontWeight={600} sx={{ textTransform: 'uppercase', letterSpacing: '0.04em' }}>
          {label}
        </Typography>
      </Grid>
      <Grid size={{ xs: 12, sm: 7 }}>
        {typeof children === 'string' ? (
          <Typography variant="body2">{children}</Typography>
        ) : children}
      </Grid>
    </>
  )
}
