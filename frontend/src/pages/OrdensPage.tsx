import { useEffect, useState } from 'react'
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
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { pesquisarOrdens } from '../api/ordens'
import { OrdemTable } from '../components/OrdemTable'
import { CriarOrdemDialog } from '../components/CriarOrdemDialog'
import { AtualizarStatusDialog } from '../components/AtualizarStatusDialog'
import { PageHeader } from '../components/PageHeader'
import { PaginacaoTabela } from '../components/PaginacaoTabela'
import { useValorComAtraso } from '../hooks/useValorComAtraso'
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

  const [pagina, setPagina] = useState(0)
  const [tamanho, setTamanho] = useState(25)
  const buscaAplicada = useValorComAtraso(busca.trim())

  const { data, isLoading, isError, isPlaceholderData } = useQuery({
    queryKey: ['ordens', 'pesquisa', buscaAplicada, filtroStatus, pagina, tamanho],
    queryFn: () => pesquisarOrdens({
      busca: buscaAplicada || undefined,
      status: filtroStatus === 'TODAS' ? undefined : filtroStatus,
      pagina,
      tamanho,
    }),
    placeholderData: keepPreviousData,
  })
  const ordens = data?.itens ?? []
  const total = data?.total ?? 0
  const filtrando = busca !== '' || filtroStatus !== 'TODAS'

  // Se a página pedida deixou de existir (ex.: uma ordem saiu do filtro), volta para a última.
  useEffect(() => {
    if (data && data.itens.length === 0 && data.total > 0 && pagina > 0) {
      setPagina(Math.max(0, data.totalPaginas - 1))
    }
  }, [data, pagina])

  return (
    <Box>
      <PageHeader
        titulo="Ordens de Produção"
        subtitulo={isLoading ? '…' : `${total} ${filtrando ? 'ordens encontradas' : 'ordens cadastradas'}`}
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
            onChange={(e) => { setBusca(e.target.value); setPagina(0) }}
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
            onChange={(e) => { setFiltroStatus(e.target.value as StatusOrdem | 'TODAS'); setPagina(0) }}
            sx={{ minWidth: 180 }}
          >
            {STATUS_OPCOES.map((op) => (
              <MenuItem key={op.value} value={op.value}>{op.label}</MenuItem>
            ))}
          </TextField>
        </Box>

        {isLoading ? (
          <Skeleton variant="rounded" height={320} sx={{ m: 2 }} />
        ) : ordens.length === 0 ? (
          <EmptyState
            Icone={ListAltIcon}
            titulo={filtrando ? 'Nenhuma ordem encontrada com os filtros' : 'Nenhuma ordem cadastrada'}
            descricao={filtrando ? 'Tente ajustar os filtros de busca.' : undefined}
            acaoLabel={!filtrando ? 'Criar primeira ordem' : undefined}
            onAcao={!filtrando ? () => setDialogCriarAberto(true) : undefined}
          />
        ) : (
          <Box sx={{ opacity: isPlaceholderData ? 0.6 : 1, transition: 'opacity 150ms' }}>
            <OrdemTable ordens={ordens} onAtualizarStatus={setOrdemSelecionada} />
            <PaginacaoTabela total={total} pagina={pagina} tamanho={tamanho} onPagina={setPagina}
              onTamanho={(novoTamanho) => { setTamanho(novoTamanho); setPagina(0) }} />
          </Box>
        )}
      </Card>

      <CriarOrdemDialog aberto={dialogCriarAberto} onFechar={() => setDialogCriarAberto(false)} />
      <AtualizarStatusDialog ordem={ordemSelecionada} onFechar={() => setOrdemSelecionada(null)} />
    </Box>
  )
}
