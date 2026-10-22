import Alert from '@mui/material/Alert'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import { useQuery } from '@tanstack/react-query'
import { consultarResumoEstoque } from '../../api/estoque'

/** Aviso de materiais vencendo e matérias-primas sem saldo; vem do resumo calculado no servidor. */
export function AlertaDeEstoque() {
  const { data } = useQuery({ queryKey: ['estoque', 'resumo'], queryFn: consultarResumoEstoque })

  if (!data || (data.materiaisVencendo === 0 && data.materiasPrimasSemSaldo === 0)) return null

  const { materiaisVencendo: vencendo, materiasPrimasSemSaldo: semSaldo } = data
  return (
    <Alert severity="warning" icon={<WarningAmberIcon />} sx={{ mb: 2 }}>
      {vencendo > 0 && `${vencendo} ${vencendo === 1 ? 'material tem lote' : 'materiais têm lotes'} vencendo em até 30 dias. `}
      {semSaldo > 0 && `${semSaldo} ${semSaldo === 1 ? 'matéria-prima está' : 'matérias-primas estão'} sem saldo disponível.`}
    </Alert>
  )
}
