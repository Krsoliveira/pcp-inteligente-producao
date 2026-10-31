import { useEffect, useMemo, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Skeleton from '@mui/material/Skeleton'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh'
import { useQuery } from '@tanstack/react-query'
import { listarLotesDisponiveis } from '../../api/lotes'
import { formatarData, formatarQuantidade } from '../../utils/formatacao'
import { deUnidades, lerQuantidade, paraUnidades, sugerirFefo } from './alocacao'
import type { Lote } from '../../types'

/** Referência estável: um `[]` novo a cada render dispararia o efeito de sugestão sem parar. */
const SEM_LOTES: Lote[] = []

interface AlocacaoDeLotesProps {
  materialId: string
  /** Quantidade consumida da linha; com zero não há o que distribuir. */
  quantidade: number
  unidade: string
  alocado: Record<string, string>
  onChange: (alocado: Record<string, string>) => void
}

/**
 * De quais lotes saiu o material (genealogia, ADR-0011), dentro de uma linha do lançamento. Enquanto o usuário não
 * mexe nos lotes, a distribuição acompanha a quantidade pela regra FEFO (vence primeiro, sai primeiro); a soma precisa
 * bater com a quantidade consumida.
 */
export function AlocacaoDeLotes({ materialId, quantidade, unidade, alocado, onChange }: AlocacaoDeLotesProps) {
  const [manual, setManual] = useState(false)
  const { data, isLoading, isError } = useQuery({
    queryKey: ['lotes-disponiveis', materialId],
    queryFn: () => listarLotesDisponiveis(materialId),
    enabled: !!materialId && quantidade > 0,
  })
  const lotes = data ?? SEM_LOTES

  const saldoDisponivel = useMemo(() => lotes.reduce((t, l) => t + paraUnidades(l.saldo), 0), [lotes])
  const totalAlocado = Object.values(alocado).reduce((t, v) => t + paraUnidades(lerQuantidade(v)), 0)
  const diferenca = paraUnidades(quantidade) - totalAlocado

  useEffect(() => {
    if (manual) return
    const sugestao = sugerirFefo(lotes, quantidade)
    onChange(Object.fromEntries(Object.entries(sugestao).map(([id, v]) => [id, String(v)])))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [quantidade, lotes, manual])

  if (quantidade <= 0) return null

  return (
    <Box sx={{ pl: 2, py: 1 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 1, mb: 0.5 }}>
        <Typography
          variant="caption"
          sx={{ color: 'text.secondary', fontWeight: 600 }}>De quais lotes saiu?</Typography>
        <Button size="small" startIcon={<AutoFixHighIcon />} disabled={lotes.length === 0} onClick={() => setManual(false)}>
          Distribuir (FEFO)
        </Button>
      </Box>
      {isLoading ? (
        <Skeleton variant="rounded" height={60} />
      ) : isError ? (
        <Alert severity="error">Não foi possível carregar os lotes disponíveis.</Alert>
      ) : lotes.length === 0 ? (
        <Alert severity="warning">Não há lote disponível deste material.</Alert>
      ) : (
        <>
          <Table size="small" sx={{ '& th, & td': { py: 0.5 }, '& th': { whiteSpace: 'nowrap' } }}>
            <TableHead>
              <TableRow>
                <TableCell>Lote</TableCell>
                <TableCell>Validade</TableCell>
                <TableCell align="right">Saldo</TableCell>
                <TableCell align="right" sx={{ width: 120 }}>Usar</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {lotes.map((lote) => (
                <TableRow key={lote.id}>
                  <TableCell><Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.78rem' }}>{lote.numeroLote}</Typography></TableCell>
                  <TableCell><Typography variant="body2" noWrap>{formatarData(lote.dataValidade)}</Typography></TableCell>
                  <TableCell align="right"><Typography variant="body2" noWrap>{formatarQuantidade(lote.saldo)}</Typography></TableCell>
                  <TableCell align="right">
                    <TextField size="small" value={alocado[lote.id] ?? ''} placeholder="0"
                      onChange={(e) => { setManual(true); onChange({ ...alocado, [lote.id]: e.target.value }) }}
                      slotProps={{ htmlInput: { inputMode: 'decimal', 'aria-label': `Quantidade do lote ${lote.numeroLote}`, style: { textAlign: 'right' } } }} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <Box sx={{ mt: 0.5, display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 1 }}>
            <Typography variant="caption" sx={{ color: diferenca === 0 ? 'success.main' : 'warning.main', fontWeight: 600 }}>
              Alocado {formatarQuantidade(deUnidades(totalAlocado))} de {formatarQuantidade(quantidade, unidade)}
            </Typography>
            {paraUnidades(quantidade) > saldoDisponivel && (
              <Typography variant="caption" sx={{ color: 'error.main' }}>Saldo disponível: {formatarQuantidade(deUnidades(saldoDisponivel), unidade)}</Typography>
            )}
          </Box>
        </>
      )}
    </Box>
  )
}
