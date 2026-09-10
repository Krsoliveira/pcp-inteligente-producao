import { useState } from 'react'
import Dialog from '@mui/material/Dialog'
import DialogTitle from '@mui/material/DialogTitle'
import DialogContent from '@mui/material/DialogContent'
import DialogActions from '@mui/material/DialogActions'
import Button from '@mui/material/Button'
import MenuItem from '@mui/material/MenuItem'
import TextField from '@mui/material/TextField'
import Alert from '@mui/material/Alert'
import Typography from '@mui/material/Typography'
import Box from '@mui/material/Box'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { atualizarStatus } from '../api/ordens'
import type { OrdemProducao, StatusOrdem } from '../types'

/** Transições válidas por status atual (espelha a máquina de estados do domínio). */
const TRANSICOES: Record<StatusOrdem, StatusOrdem[]> = {
  PLANEJADA: ['LIBERADA', 'CANCELADA'],
  LIBERADA: ['EM_PRODUCAO', 'CANCELADA'],
  EM_PRODUCAO: ['CONCLUIDA', 'CANCELADA'],
  CONCLUIDA: [],
  CANCELADA: [],
}

const STATUS_LABEL: Record<StatusOrdem, string> = {
  PLANEJADA: 'Planejada',
  LIBERADA: 'Liberada',
  EM_PRODUCAO: 'Em Produção',
  CONCLUIDA: 'Concluída',
  CANCELADA: 'Cancelada',
}

interface AtualizarStatusDialogProps {
  ordem: OrdemProducao | null
  onFechar: () => void
}

export function AtualizarStatusDialog({ ordem, onFechar }: AtualizarStatusDialogProps) {
  const [novoStatus, setNovoStatus] = useState<StatusOrdem | ''>('')
  const [erro, setErro] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const transicoes = ordem ? TRANSICOES[ordem.status] : []

  const { mutate, isPending } = useMutation({
    mutationFn: ({ id, status }: { id: string; status: StatusOrdem }) =>
      atualizarStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ordens'] })
      handleFechar()
    },
    onError: (error: unknown) => {
      if (axios.isAxiosError(error)) {
        const detail = error.response?.data?.detail as string | undefined
        setErro(detail ?? 'Erro ao atualizar status.')
      } else {
        setErro('Erro inesperado. Tente novamente.')
      }
    },
  })

  const handleSubmit = () => {
    if (!ordem || !novoStatus) return
    mutate({ id: ordem.id, status: novoStatus })
  }

  const handleFechar = () => {
    setNovoStatus('')
    setErro(null)
    onFechar()
  }

  return (
    <Dialog open={!!ordem} onClose={handleFechar} maxWidth="xs" fullWidth>
      <DialogTitle>Atualizar Status</DialogTitle>
      <DialogContent dividers>
        {erro && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {erro}
          </Alert>
        )}
        <Box sx={{ mb: 2 }}>
          <Typography variant="caption" color="text.secondary">
            Ordem
          </Typography>
          <Typography fontWeight={600}>{ordem?.codigo}</Typography>
        </Box>
        <Box sx={{ mb: 2 }}>
          <Typography variant="caption" color="text.secondary">
            Status atual
          </Typography>
          <Typography fontWeight={500}>
            {ordem ? STATUS_LABEL[ordem.status] : ''}
          </Typography>
        </Box>
        <TextField
          select
          label="Novo status *"
          value={novoStatus}
          onChange={(e) => {
            setErro(null)
            setNovoStatus(e.target.value as StatusOrdem)
          }}
          fullWidth
          disabled={transicoes.length === 0}
          helperText={transicoes.length === 0 ? 'Nenhuma transição disponível.' : ''}
        >
          {transicoes.map((s) => (
            <MenuItem key={s} value={s}>
              {STATUS_LABEL[s]}
            </MenuItem>
          ))}
        </TextField>
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={handleFechar} disabled={isPending}>
          Cancelar
        </Button>
        <Button
          variant="contained"
          onClick={handleSubmit}
          disabled={!novoStatus || isPending}
        >
          {isPending ? 'Salvando…' : 'Confirmar'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
