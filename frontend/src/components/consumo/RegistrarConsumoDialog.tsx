import { useState } from 'react'
import Dialog from '@mui/material/Dialog'
import DialogTitle from '@mui/material/DialogTitle'
import DialogContent from '@mui/material/DialogContent'
import DialogActions from '@mui/material/DialogActions'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useMutation, useQuery } from '@tanstack/react-query'
import { registrarConsumo } from '../../api/consumos'
import { listarLotesDisponiveis } from '../../api/lotes'
import { formatarCodigoMaterial, formatarQuantidade } from '../../utils/formatacao'
import { AlocacaoDeLotes } from './AlocacaoDeLotes'
import { lerQuantidade, paraUnidades } from './alocacao'
import type { ConsumoMaterial, Material, RegistrarConsumoRequest } from '../../types'
import { mensagemDeErroDaApi } from '../../utils/mensagensDeErro'

interface RegistrarConsumoDialogProps {
  consumo: ConsumoMaterial | null
  material?: Material
  ordemId: string
  onFechar: () => void
  onSucesso: () => void
}

/**
 * Registro do consumo real de um material, com os lotes de onde ele saiu (genealogia).
 * A distribuição fica em {@link AlocacaoDeLotes}; aqui só se confere o que ela devolveu.
 */
export function RegistrarConsumoDialog({ consumo, material, ordemId, onFechar, onSucesso }: RegistrarConsumoDialogProps) {
  const [qtd, setQtd] = useState('')
  const [justificativa, setJustificativa] = useState('')
  const [alocado, setAlocado] = useState<Record<string, string>>({})
  const [erro, setErro] = useState<string | null>(null)

  const quantidade = lerQuantidade(qtd)
  const desvio = consumo && qtd ? quantidade - consumo.quantidadePlanejada : 0
  const temDesvio = Math.abs(desvio) > 0.0001
  const unidade = consumo?.unidadeDeMedida

  // Mesma chave de AlocacaoDeLotes: a consulta é uma só. Aqui serve para conferir o saldo ao registrar.
  const { data: lotes = [] } = useQuery({
    queryKey: ['lotes-disponiveis', consumo?.materialId],
    queryFn: () => listarLotesDisponiveis(consumo!.materialId),
    enabled: !!consumo,
  })
  const totalAlocado = Object.values(alocado).reduce((t, v) => t + paraUnidades(lerQuantidade(v)), 0)
  const diferenca = paraUnidades(quantidade) - totalAlocado

  const limpar = () => {
    setQtd(''); setJustificativa(''); setAlocado({}); setErro(null)
  }
  const fechar = () => { limpar(); onFechar() }

  const { mutate, isPending } = useMutation({
    mutationFn: (payload: RegistrarConsumoRequest) => registrarConsumo(ordemId, consumo!.id, payload),
    onSuccess: () => { onSucesso(); fechar() },
    onError: (err: unknown) => {
      setErro(mensagemDeErroDaApi(err, 'Não foi possível registrar o consumo. Tente novamente.'))
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
      <DialogTitle>Registrar Consumo{material ? ` — ${formatarCodigoMaterial(material.codigo)}` : ''}</DialogTitle>
      <DialogContent dividers>
        {erro && <Alert severity="error" sx={{ mb: 2 }}>{erro}</Alert>}
        {consumo && (
          <Box sx={{ mb: 2, p: 1.5, bgcolor: 'grey.50', borderRadius: 1 }}>
            <Typography variant="caption" sx={{ color: 'text.secondary' }}>Quantidade planejada</Typography>
            <Typography variant="body2" sx={{ fontWeight: 600 }}>{formatarQuantidade(consumo.quantidadePlanejada, unidade)}</Typography>
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

        {consumo && quantidade > 0 && (
          <Box sx={{ mt: 3 }}>
            <AlocacaoDeLotes materialId={consumo.materialId} quantidade={quantidade} unidade={unidade ?? ''}
              alocado={alocado} onChange={setAlocado} />
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
