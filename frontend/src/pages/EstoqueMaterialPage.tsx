import { useState } from 'react'
import { useParams } from 'react-router-dom'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Grid from '@mui/material/Grid2'
import TextField from '@mui/material/TextField'
import MenuItem from '@mui/material/MenuItem'
import Skeleton from '@mui/material/Skeleton'
import Alert from '@mui/material/Alert'
import Typography from '@mui/material/Typography'
import AllInboxIcon from '@mui/icons-material/AllInboxOutlined'
import CheckCircleIcon from '@mui/icons-material/CheckCircleOutline'
import BlockIcon from '@mui/icons-material/Block'
import EventIcon from '@mui/icons-material/EventOutlined'
import { useQuery } from '@tanstack/react-query'
import { consultarEstoque } from '../api/estoque'
import { listarLotesPorMaterial } from '../api/lotes'
import { PageHeader } from '../components/PageHeader'
import { EmptyState } from '../components/EmptyState'
import { KpiCard } from '../components/KpiCard'
import { TabelaLotes } from '../components/suprimentos/TabelaLotes'
import { formatarCodigoMaterial, formatarData, formatarQuantidade } from '../utils/formatacao'
import { TIPO_MATERIAL_LABEL, type StatusLote } from '../types'

/** Estoque de um material: posição e todos os lotes (de compra e de produção). */
export function EstoqueMaterialPage() {
  const { materialId } = useParams<{ materialId: string }>()
  const [status, setStatus] = useState<StatusLote | 'TODOS'>('DISPONIVEL')
  const [busca, setBusca] = useState('')

  const estoqueQuery = useQuery({ queryKey: ['estoque'], queryFn: consultarEstoque })
  const lotesQuery = useQuery({
    queryKey: ['lotes', 'material', materialId],
    queryFn: () => listarLotesPorMaterial(materialId!),
    enabled: !!materialId,
  })
  const posicao = estoqueQuery.data?.find((p) => p.materialId === materialId)
  const lotes = lotesQuery.data ?? []
  const termo = busca.trim().toLowerCase()
  const filtrados = lotes.filter((l) => (status === 'TODOS' || l.status === status)
    && (termo === '' || l.numeroLote.toLowerCase().includes(termo) || (l.notaFiscal ?? '').toLowerCase().includes(termo)
      || (l.fornecedor ?? '').toLowerCase().includes(termo)))

  if (estoqueQuery.isLoading) return <Skeleton variant="rounded" height={320} />
  if (estoqueQuery.isError || !posicao) return <Alert severity="error">Material não encontrado.</Alert>

  const codigo = formatarCodigoMaterial(posicao.codigo)
  const unidade = posicao.unidadeDeMedida

  return (
    <Box>
      <PageHeader
        titulo={`${codigo} — ${posicao.descricao}`}
        subtitulo={`${TIPO_MATERIAL_LABEL[posicao.tipo]} · ${unidade}`}
        breadcrumbs={[{ label: 'Suprimentos' }, { label: 'Estoque', href: '/estoque' }, { label: codigo }]}
      />

      <Grid container spacing={2} sx={{ mb: 2 }}>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <KpiCard titulo="Disponível" valor={formatarQuantidade(posicao.saldoDisponivel, unidade)}
            subtitulo={`${posicao.lotesDisponiveis} ${posicao.lotesDisponiveis === 1 ? 'lote' : 'lotes'}`}
            Icone={CheckCircleIcon} cor="#2e7d32" />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <KpiCard titulo="Indisponível" valor={formatarQuantidade(posicao.saldoIndisponivel, unidade)}
            subtitulo="Bloqueado ou vencido" Icone={BlockIcon} cor="#ed6c02" />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <KpiCard titulo="Próximo vencimento" valor={formatarData(posicao.proximoVencimento)}
            subtitulo="Entre os lotes disponíveis" Icone={EventIcon} />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <KpiCard titulo="Lotes" valor={String(lotes.length)} subtitulo="De qualquer status" Icone={AllInboxIcon} cor="#455a64" />
        </Grid>
      </Grid>

      <Card>
        <CardContent sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', borderBottom: '1px solid', borderColor: 'divider' }}>
          <TextField placeholder="Buscar por lote, nota fiscal ou fornecedor…" value={busca}
            onChange={(e) => setBusca(e.target.value)} sx={{ flex: 1, minWidth: 220 }} />
          <TextField select label="Status" value={status} onChange={(e) => setStatus(e.target.value as StatusLote | 'TODOS')}
            sx={{ minWidth: 160 }}>
            <MenuItem value="TODOS">Todos</MenuItem>
            <MenuItem value="DISPONIVEL">Disponível</MenuItem>
            <MenuItem value="BLOQUEADO">Bloqueado</MenuItem>
            <MenuItem value="CONSUMIDO">Consumido</MenuItem>
            <MenuItem value="VENCIDO">Vencido</MenuItem>
          </TextField>
        </CardContent>
        {lotesQuery.isLoading ? (
          <Skeleton variant="rounded" height={240} sx={{ m: 2 }} />
        ) : lotesQuery.isError ? (
          <Alert severity="error" sx={{ m: 2 }}>Não foi possível carregar os lotes.</Alert>
        ) : filtrados.length === 0 ? (
          <EmptyState Icone={AllInboxIcon} titulo="Nenhum lote"
            descricao={lotes.length > 0 ? 'Nenhum lote atende aos filtros.' : 'Este material ainda não teve entrada.'} />
        ) : (
          <>
            <TabelaLotes lotes={filtrados} coluna="origem" />
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', px: 2, py: 1 }}>
              {filtrados.length} de {lotes.length} lotes
            </Typography>
          </>
        )}
      </Card>
    </Box>
  )
}
