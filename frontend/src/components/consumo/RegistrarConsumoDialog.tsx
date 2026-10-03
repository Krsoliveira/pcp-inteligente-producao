import { useEffect, useMemo, useState } from 'react'
import Dialog from '@mui/material/Dialog'
import DialogTitle from '@mui/material/DialogTitle'
import DialogContent from '@mui/material/DialogContent'
import DialogActions from '@mui/material/DialogActions'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid2'
import Skeleton from '@mui/material/Skeleton'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh'
import { useMutation, useQuery } from '@tanstack/react-query'
import axios from 'axios'
import { registrarConsumo } from '../../api/consumos'
import { listarLotesDisponiveis } from '../../api/lotes'
import { formatarData, formatarQuantidade } from '../../utils/formatacao'
import { deUnidades, lerQuantidade, paraUnidades, sugerirFefo } from './alocacao'
import type { ApiError, ConsumoMaterial, Lote, Material, RegistrarConsumoRequest } from '../../types'

/** Referência estável: um `[]` novo a cada render dispararia o efeito de sugestão sem parar. */
const SEM_LOTES: Lote[] = []

interface RegistrarConsumoDialogProps {
  consumo: ConsumoMaterial | null
  material?: Material
  ordemId: string
  onFechar: () => void
  onSucesso: () => void
}

/**
 * Registro do consumo real de um material, com os lotes de onde ele saiu (genealogia).
 * A distribuição entre lotes é sugerida pela regra FEFO (vence primeiro, sai primeiro)
 * e pode ser ajustada; a soma precisa bater com a quantidade consumida.
 */
export function RegistrarConsumoDialog({ consumo, material, ordemId, onFechar, onSucesso }: RegistrarConsumoDialogProps) {
  const [qtd, setQtd] = useState('')
  const [justificativa, setJustificativa] = useState('')
  const [alocado, setAlocado] = useState<Record<string, string>>({})
  const [ajusteManual, setAjusteManual] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  const quantidade = lerQuantidade(qtd)
  const desvio = consumo && qtd ? quantidade - consumo.quantidadePlanejada : 0
  const temDesvio = Math.abs(desvio) > 0.0001
  const unidade = consumo?.unidadeDeMedida

  const { data, isLoading: carregandoLotes, isError: erroLotes } = useQuery({
    queryKey: ['lotes-disponiveis', consumo?.materialId],
    queryFn: () => listarLotesDisponiveis(consumo!.materialId),
    enabled: !!consumo,
  })
  const lotes = data ?? SEM_LOTES

  const saldoDisponivel = useMemo(() => lotes.reduce((t, l) => t + paraUnidades(l.saldo), 0), [lotes])
  const totalAlocado = Object.values(alocado).reduce((t, v) => t + paraUnidades(lerQuantidade(v)), 0)
  const diferenca = paraUnidades(quantidade) - totalAlocado
  const saldoInsuficiente = paraUnidades(quantidade) > saldoDisponivel

  // Enquanto o usuário não mexe nos lotes, a distribuição acompanha a quantidade (FEFO).
  useEffect(() => {
    if (ajusteManual) return
    const sugestao = sugerirFefo(lotes, quantidade)
    setAlocado(Object.fromEntries(Object.entries(sugestao).map(([id, v]) => [id, String(v)])))
  }, [quantidade, lotes, ajusteManual])

  const limpar = () => {
    setQtd(''); setJustificativa(''); setAlocado({}); setAjusteManual(false); setErro(null)
  }
  const fechar = () => { limpar(); onFechar() }

  const { mutate, isPending } = useMutation({
    mutationFn: (payload: RegistrarConsumoRequest) => registrarConsumo(ordemId, consumo!.id, payload),
    onSuccess: () => { onSucesso(); fechar() },
    onError: (err: unknown) => {
      if (axios.isAxiosError<ApiError>(err)) setErro(err.response?.data?.detail ?? 'Não foi possível registrar o consumo.')
      else setErro('Erro inesperado.')
    },
  })

  const registrar = () => {
    if (!qtd || !Number.isFinite(Number(qtd.replace(',', '.'))) || Number(qtd.replace(',', '.')) < 0) {
      setErro('Informe a quantidade consumida.'); return
    }
    if (temDesvio && !justificativa.trim()) { setErro('Há desvio: informe a justificativa.'); return }
    const alocacoes = Object.entries(alocado)
      .map(([loteId, v]) => ({ loteId, quantidade: lerQuantidade(v) }))
      .filter((a) => a.quantidade > 0)
    const acimaDoSaldo = lotes.find((l) => paraUnidades(lerQuantidade(alocado[l.id] ?? '')) > paraUnidades(l.saldo))
    if (acimaDoSaldo) { setErro(`O lote ${acimaDoSaldo.numeroLote} não tem esse saldo.`); return }
    if (quantidade > 0 && diferenca !== 0) {
      setErro('A soma dos lotes deve ser igual à quantidade consumida.'); return
    }
    mutate({ quantidadeConsumida: quantidade, justificativa: justificativa.trim() || undefined, alocacoes: quantidade > 0 ? alocacoes : [] })
  }

  return (
    <Dialog open={!!consumo} onClose={fechar} fullWidth maxWidth="sm">
      <DialogTitle>Registrar Consumo{material ? ` — ${material.codigo}` : ''}</DialogTitle>
      <DialogContent dividers>
        {erro && <Alert severity="error" sx={{ mb: 2 }}>{erro}</Alert>}
        {consumo && (
          <Box sx={{ mb: 2, p: 1.5, bgcolor: 'grey.50', borderRadius: 1 }}>
            <Typography variant="caption" color="text.secondary">Quantidade planejada</Typography>
            <Typography variant="body2" fontWeight={600}>{formatarQuantidade(consumo.quantidadePlanejada, unidade)}</Typography>
          </Box>
        )}
        <Grid container spacing={2}>
          <Grid size={12}>
            <TextField label="Quantidade consumida *" value={qtd} onChange={(e) => setQtd(e.target.value)} fullWidth
              slotProps={{ htmlInput: { inputMode: 'decimal' } }}
              helperText={qtd && temDesvio ? `Desvio: ${desvio > 0 ? '+' : ''}${formatarQuantidade(desvio, unidade)}` : ''} />
          </Grid>
          {temDesvio && (
            <Grid size={12}>
              <TextField label="Justificativa do desvio *" value={justificativa} onChange={(e) => setJustificativa(e.target.value)}
                fullWidth multiline rows={2} helperText="Fica registrada em seu nome, com data e hora." />
            </Grid>
          )}
        </Grid>

        {quantidade > 0 && (
          <Box sx={{ mt: 3 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 1, mb: 1 }}>
              <Typography variant="subtitle2">De quais lotes saiu?</Typography>
              <Button size="small" startIcon={<AutoFixHighIcon />} disabled={lotes.length === 0}
                onClick={() => { setAjusteManual(false) }}>
                Distribuir (FEFO)
              </Button>
            </Box>

            {carregandoLotes ? (
              <Skeleton variant="rounded" height={100} />
            ) : erroLotes ? (
              <Alert severity="error">Não foi possível carregar os lotes disponíveis.</Alert>
            ) : lotes.length === 0 ? (
              <Alert severity="warning">
                Não há lote disponível deste material. Registre uma entrada de material (matéria-prima)
                ou conclua uma ordem que o produza (semiacabado).
              </Alert>
            ) : (
              <>
                <TableContainer sx={{ maxHeight: 260 }}>
                  <Table size="small" stickyHeader sx={{ '& th': { whiteSpace: 'nowrap' } }}>
                    <TableHead>
                      <TableRow>
                        <TableCell>Lote</TableCell>
                        <TableCell>Validade</TableCell>
                        <TableCell align="right">Saldo</TableCell>
                        <TableCell align="right" sx={{ width: 130 }}>Usar</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {lotes.map((lote) => (
                        <TableRow key={lote.id}>
                          <TableCell>
                            <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.78rem' }}>{lote.numeroLote}</Typography>
                            <Typography variant="caption" color="text.secondary">
                              {lote.origem === 'COMPRA' ? `NF ${lote.notaFiscal}` : 'Produção'}
                            </Typography>
                          </TableCell>
                          <TableCell><Typography variant="body2" noWrap>{formatarData(lote.dataValidade)}</Typography></TableCell>
                          <TableCell align="right"><Typography variant="body2" noWrap>{formatarQuantidade(lote.saldo)}</Typography></TableCell>
                          <TableCell align="right">
                            <TextField size="small" value={alocado[lote.id] ?? ''} placeholder="0"
                              onChange={(e) => { setAjusteManual(true); setAlocado((a) => ({ ...a, [lote.id]: e.target.value })) }}
                              slotProps={{ htmlInput: { inputMode: 'decimal', 'aria-label': `Quantidade do lote ${lote.numeroLote}`, style: { textAlign: 'right' } } }} />
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
                <Box sx={{ mt: 1, display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 1 }}>
                  <Typography variant="body2" color={diferenca === 0 ? 'success.main' : 'warning.main'} fontWeight={600}>
                    Alocado {formatarQuantidade(deUnidades(totalAlocado))} de {formatarQuantidade(quantidade, unidade)}
                  </Typography>
                  {saldoInsuficiente && (
                    <Typography variant="body2" color="error.main">
                      Saldo disponível: {formatarQuantidade(deUnidades(saldoDisponivel), unidade)}
                    </Typography>
                  )}
                </Box>
              </>
            )}
          </Box>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={fechar} disabled={isPending}>Cancelar</Button>
        <Button variant="contained" onClick={registrar} disabled={isPending}>{isPending ? 'Salvando…' : 'Registrar'}</Button>
      </DialogActions>
    </Dialog>
  )
}
