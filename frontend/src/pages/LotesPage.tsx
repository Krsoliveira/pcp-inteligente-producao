import { useState } from 'react'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import MenuItem from '@mui/material/MenuItem'
import Typography from '@mui/material/Typography'
import Skeleton from '@mui/material/Skeleton'
import Alert from '@mui/material/Alert'
import Tooltip from '@mui/material/Tooltip'
import Button from '@mui/material/Button'
import Snackbar from '@mui/material/Snackbar'
import AddIcon from '@mui/icons-material/Add'
import AllInboxIcon from '@mui/icons-material/AllInboxOutlined'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import { useQuery } from '@tanstack/react-query'
import { listarLotes } from '../api/lotes'
import { listarMateriais } from '../api/materiais'
import { EntradaMaterialDialog } from '../components/lotes/EntradaMaterialDialog'
import { PageHeader } from '../components/PageHeader'
import { EmptyState } from '../components/EmptyState'
import { StatusLoteBadge } from '../components/StatusBadge'
import type { Lote, StatusLote } from '../types'

export function LotesPage() {
  const [filtroStatus, setFiltroStatus] = useState<StatusLote | 'TODOS'>('TODOS')
  const [busca, setBusca] = useState('')
  const [dialogAberto, setDialogAberto] = useState(false)
  const [loteRegistrado, setLoteRegistrado] = useState<Lote | null>(null)

  const { data: lotes = [], isLoading, isError } = useQuery({
    queryKey: ['lotes'],
    queryFn: listarLotes,
  })

  const { data: materiais = [] } = useQuery({ queryKey: ['materiais'], queryFn: listarMateriais })
  const materialPorId = new Map(materiais.map((m) => [m.id, m]))

  const hoje = new Date()

  const filtrados = lotes.filter((l) => {
    const statusOk = filtroStatus === 'TODOS' || l.status === filtroStatus
    const termo = busca.toLowerCase()
    const buscaOk = busca === ''
      || l.numeroLote.toLowerCase().includes(termo)
      || (l.notaFiscal ?? '').toLowerCase().includes(termo)
      || (l.fornecedor ?? '').toLowerCase().includes(termo)
    return statusOk && buscaOk
  })

  const vencendoEm30 = lotes.filter((l) => {
    if (l.status !== 'DISPONIVEL') return false
    const validade = new Date(l.dataValidade)
    const diff = (validade.getTime() - hoje.getTime()) / (1000 * 60 * 60 * 24)
    return diff >= 0 && diff <= 30
  }).length

  return (
    <Box>
      <PageHeader
        titulo="Lotes"
        subtitulo={isLoading ? '…' : `${lotes.length} lotes de produção e de compra`}
        breadcrumbs={[{ label: 'Produção' }, { label: 'Lotes' }]}
        action={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialogAberto(true)}>
            Entrada de Material
          </Button>
        }
      />

      {vencendoEm30 > 0 && (
        <Alert severity="warning" icon={<WarningAmberIcon />} sx={{ mb: 2 }}>
          {vencendoEm30} {vencendoEm30 === 1 ? 'lote vence' : 'lotes vencem'} nos próximos 30 dias.
        </Alert>
      )}

      {isError && <Alert severity="error" sx={{ mb: 2 }}>Não foi possível carregar os lotes.</Alert>}

      <Card>
        <Box sx={{ p: 2, display: 'flex', gap: 2, flexWrap: 'wrap', borderBottom: '1px solid', borderColor: 'divider' }}>
          <TextField
            placeholder="Buscar por lote, nota fiscal ou fornecedor…"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            sx={{ flex: 1, minWidth: 220 }}
          />
          <TextField
            select
            label="Status"
            value={filtroStatus}
            onChange={(e) => setFiltroStatus(e.target.value as StatusLote | 'TODOS')}
            sx={{ minWidth: 160 }}
          >
            <MenuItem value="TODOS">Todos</MenuItem>
            <MenuItem value="DISPONIVEL">Disponível</MenuItem>
            <MenuItem value="BLOQUEADO">Bloqueado</MenuItem>
            <MenuItem value="CONSUMIDO">Consumido</MenuItem>
            <MenuItem value="VENCIDO">Vencido</MenuItem>
          </TextField>
        </Box>

        {isLoading ? (
          <Skeleton variant="rounded" height={320} sx={{ m: 2 }} />
        ) : filtrados.length === 0 ? (
          <EmptyState
            Icone={AllInboxIcon}
            titulo="Nenhum lote encontrado"
            descricao="Lotes nascem ao concluir ordens de produção ou ao registrar a entrada de matéria-prima."
            acaoLabel="Entrada de Material"
            onAcao={() => setDialogAberto(true)}
          />
        ) : (
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Número do Lote</TableCell>
                  <TableCell>Material</TableCell>
                  <TableCell>Origem</TableCell>
                  <TableCell align="right">Quantidade</TableCell>
                  <TableCell>Fabricação</TableCell>
                  <TableCell>Validade</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filtrados.map((lote) => {
                  const validade = new Date(lote.dataValidade)
                  const diasParaVencer = Math.ceil((validade.getTime() - hoje.getTime()) / (1000 * 60 * 60 * 24))
                  const vencendoBreve = lote.status === 'DISPONIVEL' && diasParaVencer <= 30 && diasParaVencer >= 0

                  return (
                    <TableRow key={lote.id} hover>
                      <TableCell>
                        <Typography variant="body2" fontWeight={600} sx={{ fontFamily: 'monospace' }}>
                          {lote.numeroLote}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">{materialPorId.get(lote.materialId)?.codigo ?? '—'}</Typography>
                      </TableCell>
                      <TableCell>
                        {lote.origem === 'COMPRA' ? (
                          <Tooltip title={`Fornecedor: ${lote.fornecedor}`}>
                            <Typography variant="body2">Compra · NF {lote.notaFiscal}</Typography>
                          </Tooltip>
                        ) : (
                          <Typography variant="body2" color="text.secondary">Produção</Typography>
                        )}
                      </TableCell>
                      <TableCell align="right">
                        <Typography variant="body2">{lote.quantidade.toLocaleString('pt-BR')} {lote.unidadeDeMedida}</Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" color="text.secondary">{formatarData(lote.dataFabricacao)}</Typography>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
                          <Typography variant="body2" color={vencendoBreve ? 'warning.main' : 'text.secondary'} fontWeight={vencendoBreve ? 600 : 400}>
                            {formatarData(lote.dataValidade)}
                          </Typography>
                          {vencendoBreve && (
                            <Tooltip title={`Vence em ${diasParaVencer} dias`}>
                              <WarningAmberIcon sx={{ fontSize: 14, color: 'warning.main' }} />
                            </Tooltip>
                          )}
                        </Box>
                      </TableCell>
                      <TableCell><StatusLoteBadge status={lote.status} /></TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Card>

      <EntradaMaterialDialog
        aberto={dialogAberto}
        onFechar={() => setDialogAberto(false)}
        onRegistrado={setLoteRegistrado}
      />
      <Snackbar
        open={!!loteRegistrado}
        autoHideDuration={6000}
        onClose={() => setLoteRegistrado(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity="success" variant="filled" onClose={() => setLoteRegistrado(null)}>
          Entrada registrada. Lote {loteRegistrado?.numeroLote} disponível.
        </Alert>
      </Snackbar>
    </Box>
  )
}

function formatarData(data: string): string {
  const [ano, mes, dia] = data.split('-')
  return `${dia}/${mes}/${ano}`
}
