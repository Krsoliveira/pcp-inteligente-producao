import { useState } from 'react'
import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import TextField from '@mui/material/TextField'
import MenuItem from '@mui/material/MenuItem'
import InputAdornment from '@mui/material/InputAdornment'
import Skeleton from '@mui/material/Skeleton'
import Alert from '@mui/material/Alert'
import AddIcon from '@mui/icons-material/Add'
import SearchIcon from '@mui/icons-material/Search'
import { useQuery } from '@tanstack/react-query'
import { listarOrdens } from '../api/ordens'
import { OrdemTable } from '../components/OrdemTable'
import { CriarOrdemDialog } from '../components/CriarOrdemDialog'
import { AtualizarStatusDialog } from '../components/AtualizarStatusDialog'
import type { OrdemProducao, StatusOrdem } from '../types'

const STATUS_OPCOES: Array<{ label: string; value: StatusOrdem | 'TODAS' }> = [
  { label: 'Todos os status', value: 'TODAS' },
  { label: 'Planejada', value: 'PLANEJADA' },
  { label: 'Liberada', value: 'LIBERADA' },
  { label: 'Em Produção', value: 'EM_PRODUCAO' },
  { label: 'Concluída', value: 'CONCLUIDA' },
  { label: 'Cancelada', value: 'CANCELADA' },
]

export function OrdensPage() {
  const [dialogCriarAberto, setDialogCriarAberto] = useState(false)
  const [ordemSelecionada, setOrdemSelecionada] = useState<OrdemProducao | null>(null)
  const [busca, setBusca] = useState('')
  const [filtroStatus, setFiltroStatus] = useState<StatusOrdem | 'TODAS'>('TODAS')

  const { data: ordens = [], isLoading, isError } = useQuery({
    queryKey: ['ordens'],
    queryFn: listarOrdens,
  })

  const ordensFiltradas = ordens.filter((o) => {
    const buscaMatch =
      busca === '' ||
      o.codigo.toLowerCase().includes(busca.toLowerCase()) ||
      o.produto.toLowerCase().includes(busca.toLowerCase())

    const statusMatch = filtroStatus === 'TODAS' || o.status === filtroStatus

    return buscaMatch && statusMatch
  })

  return (
    <Box>
      {/* Cabeçalho */}
      <Box
        sx={{
          mb: 3,
          display: 'flex',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: 2,
        }}
      >
        <Box>
          <Typography variant="h5">Ordens de Produção</Typography>
          <Typography variant="body2" color="text.secondary">
            {isLoading ? '…' : `${ordens.length} ordens cadastradas`}
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setDialogCriarAberto(true)}
        >
          Nova Ordem
        </Button>
      </Box>

      {isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Não foi possível carregar as ordens.
        </Alert>
      )}

      <Card>
        <CardContent sx={{ p: 3 }}>
          {/* Filtros */}
          <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
            <TextField
              placeholder="Buscar por código ou produto…"
              value={busca}
              onChange={(e) => setBusca(e.target.value)}
              size="small"
              sx={{ flex: 1, minWidth: 220 }}
              slotProps={{
                input: {
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon fontSize="small" color="action" />
                    </InputAdornment>
                  ),
                },
              }}
            />
            <TextField
              select
              label="Status"
              value={filtroStatus}
              onChange={(e) => setFiltroStatus(e.target.value as StatusOrdem | 'TODAS')}
              size="small"
              sx={{ minWidth: 180 }}
            >
              {STATUS_OPCOES.map((op) => (
                <MenuItem key={op.value} value={op.value}>
                  {op.label}
                </MenuItem>
              ))}
            </TextField>
          </Box>

          {/* Tabela */}
          {isLoading ? (
            <Skeleton variant="rounded" height={320} />
          ) : (
            <OrdemTable
              ordens={ordensFiltradas}
              onAtualizarStatus={setOrdemSelecionada}
            />
          )}
        </CardContent>
      </Card>

      <CriarOrdemDialog
        aberto={dialogCriarAberto}
        onFechar={() => setDialogCriarAberto(false)}
      />
      <AtualizarStatusDialog
        ordem={ordemSelecionada}
        onFechar={() => setOrdemSelecionada(null)}
      />
    </Box>
  )
}
