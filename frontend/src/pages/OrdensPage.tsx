import { useState } from 'react'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import TextField from '@mui/material/TextField'
import MenuItem from '@mui/material/MenuItem'
import Button from '@mui/material/Button'
import Skeleton from '@mui/material/Skeleton'
import Alert from '@mui/material/Alert'
import InputAdornment from '@mui/material/InputAdornment'
import AddIcon from '@mui/icons-material/Add'
import SearchIcon from '@mui/icons-material/Search'
import ListAltIcon from '@mui/icons-material/ListAlt'
import { useQuery } from '@tanstack/react-query'
import { listarOrdens } from '../api/ordens'
import { OrdemTable } from '../components/OrdemTable'
import { CriarOrdemDialog } from '../components/CriarOrdemDialog'
import { AtualizarStatusDialog } from '../components/AtualizarStatusDialog'
import { PageHeader } from '../components/PageHeader'
import { EmptyState } from '../components/EmptyState'
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
    const buscaMatch = busca === '' || o.codigo.toLowerCase().includes(busca.toLowerCase())
    const statusMatch = filtroStatus === 'TODAS' || o.status === filtroStatus
    return buscaMatch && statusMatch
  })

  return (
    <Box>
      <PageHeader
        titulo="Ordens de Produção"
        subtitulo={isLoading ? '…' : `${ordens.length} ordens cadastradas`}
        breadcrumbs={[{ label: 'Produção' }, { label: 'Ordens' }]}
        action={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialogCriarAberto(true)}>
            Nova Ordem
          </Button>
        }
      />

      {isError && <Alert severity="error" sx={{ mb: 2 }}>Não foi possível carregar as ordens.</Alert>}

      <Card>
        <Box sx={{ p: 2, display: 'flex', gap: 2, flexWrap: 'wrap', borderBottom: '1px solid', borderColor: 'divider' }}>
          <TextField
            placeholder="Buscar por código…"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
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
            sx={{ minWidth: 180 }}
          >
            {STATUS_OPCOES.map((op) => (
              <MenuItem key={op.value} value={op.value}>{op.label}</MenuItem>
            ))}
          </TextField>
        </Box>

        {isLoading ? (
          <Skeleton variant="rounded" height={320} sx={{ m: 2 }} />
        ) : ordensFiltradas.length === 0 ? (
          <EmptyState
            Icone={ListAltIcon}
            titulo={busca || filtroStatus !== 'TODAS' ? 'Nenhuma ordem encontrada com os filtros' : 'Nenhuma ordem cadastrada'}
            descricao={busca || filtroStatus !== 'TODAS' ? 'Tente ajustar os filtros de busca.' : undefined}
            acaoLabel={!busca && filtroStatus === 'TODAS' ? 'Criar primeira ordem' : undefined}
            onAcao={!busca && filtroStatus === 'TODAS' ? () => setDialogCriarAberto(true) : undefined}
          />
        ) : (
          <OrdemTable ordens={ordensFiltradas} onAtualizarStatus={setOrdemSelecionada} />
        )}
      </Card>

      <CriarOrdemDialog aberto={dialogCriarAberto} onFechar={() => setDialogCriarAberto(false)} />
      <AtualizarStatusDialog ordem={ordemSelecionada} onFechar={() => setOrdemSelecionada(null)} />
    </Box>
  )
}
