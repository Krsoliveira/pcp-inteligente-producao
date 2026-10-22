import { useParams } from 'react-router-dom'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Divider from '@mui/material/Divider'
import Grid from '@mui/material/Grid2'
import Skeleton from '@mui/material/Skeleton'
import Alert from '@mui/material/Alert'
import Typography from '@mui/material/Typography'
import ReceiptLongIcon from '@mui/icons-material/ReceiptLongOutlined'
import { useQuery } from '@tanstack/react-query'
import { buscarNotaFiscal } from '../api/notasFiscais'
import { listarMateriais } from '../api/materiais'
import { PageHeader } from '../components/PageHeader'
import { InfoRow } from '../components/InfoRow'
import { HistoricoAuditoria } from '../components/HistoricoAuditoria'
import { TabelaLotes } from '../components/suprimentos/TabelaLotes'
import { formatarData, formatarDataHora } from '../utils/formatacao'

/** Detalhe da nota fiscal de entrada: cabeçalho, itens (lotes que trouxe) e histórico. */
export function NotaFiscalDetalhePage() {
  const { id } = useParams<{ id: string }>()
  const notaQuery = useQuery({ queryKey: ['nota-fiscal', id], queryFn: () => buscarNotaFiscal(id!), enabled: !!id })
  const { data: materiais = [] } = useQuery({ queryKey: ['materiais'], queryFn: listarMateriais })
  const materialPorId = new Map(materiais.map((m) => [m.id, m]))
  const nota = notaQuery.data

  if (notaQuery.isLoading) return <Skeleton variant="rounded" height={320} />
  if (notaQuery.isError || !nota) return <Alert severity="error">Nota fiscal não encontrada.</Alert>

  return (
    <Box>
      <PageHeader
        titulo={`NF ${nota.numero}`}
        subtitulo={nota.fornecedor}
        breadcrumbs={[
          { label: 'Suprimentos' },
          { label: 'Entrada de notas', href: '/notas-fiscais' },
          { label: `NF ${nota.numero}` },
        ]}
      />
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, lg: 4 }}>
          <Card sx={{ height: '100%' }}>
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                <ReceiptLongIcon color="action" fontSize="small" />
                <Typography variant="subtitle2">Nota fiscal</Typography>
              </Box>
              <Divider sx={{ mb: 2 }} />
              <Grid container spacing={1.5}>
                <InfoRow label="Fornecedor">{nota.fornecedor}</InfoRow>
                <InfoRow label="Número">
                  <Typography variant="body2" fontWeight={600} sx={{ fontFamily: 'monospace', overflowWrap: 'anywhere' }}>
                    {nota.numero}
                  </Typography>
                </InfoRow>
                <InfoRow label="Emissão">{formatarData(nota.dataEmissao)}</InfoRow>
                <InfoRow label="Recebimento">{formatarData(nota.dataRecebimento)}</InfoRow>
                <InfoRow label="Itens">{String(nota.quantidadeItens)}</InfoRow>
                <InfoRow label="Lançada">
                  <Box>
                    <Typography variant="body2">{nota.registradaPor}</Typography>
                    <Typography variant="caption" color="text.secondary">{formatarDataHora(nota.registradaEm)}</Typography>
                  </Box>
                </InfoRow>
              </Grid>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, lg: 8 }}>
          <Card sx={{ height: '100%' }}>
            <CardContent sx={{ p: 3, pb: 1 }}>
              <Typography variant="subtitle2">Itens — lotes recebidos</Typography>
              <Typography variant="caption" color="text.secondary">
                Clique em um lote para ver o saldo e onde ele foi consumido.
              </Typography>
            </CardContent>
            <TabelaLotes lotes={nota.itens} coluna="material" materialPorId={materialPorId} />
          </Card>
        </Grid>
        <Grid size={12}>
          <HistoricoAuditoria tipoEntidade="NOTA_FISCAL" entidadeId={nota.id} titulo="Histórico da nota" />
        </Grid>
      </Grid>
    </Box>
  )
}
