import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import Skeleton from '@mui/material/Skeleton'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Snackbar from '@mui/material/Snackbar'
import AddIcon from '@mui/icons-material/Add'
import ReceiptLongIcon from '@mui/icons-material/ReceiptLongOutlined'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { listarNotasFiscais } from '../api/notasFiscais'
import { listarMateriais } from '../api/materiais'
import { EntradaNotaFiscalDialog } from '../components/suprimentos/EntradaNotaFiscalDialog'
import { PageHeader } from '../components/PageHeader'
import { EmptyState } from '../components/EmptyState'
import { formatarCodigoMaterial, formatarData, formatarDataHora } from '../utils/formatacao'
import type { NotaFiscal } from '../types'

/**
 * Entrada de notas (ADR-0012): as notas fiscais de compra recebidas e o lançamento de
 * novas. Cada item lançado vira um lote de matéria-prima no estoque.
 */
export function NotasFiscaisPage() {
  const navigate = useNavigate()
  const [busca, setBusca] = useState('')
  const [periodoDe, setPeriodoDe] = useState('')
  const [periodoAte, setPeriodoAte] = useState('')
  const [dialogAberto, setDialogAberto] = useState(false)
  const [registrada, setRegistrada] = useState<NotaFiscal | null>(null)

  const periodoInvalido = !!periodoDe && !!periodoAte && periodoAte < periodoDe
  const { data: notas = [], isLoading, isError } = useQuery({
    queryKey: ['notas-fiscais', periodoDe, periodoAte],
    queryFn: () => listarNotasFiscais(periodoDe, periodoAte),
    enabled: !periodoInvalido,
    placeholderData: keepPreviousData,
  })
  const { data: materiais = [] } = useQuery({ queryKey: ['materiais'], queryFn: listarMateriais })
  const materialPorId = new Map(materiais.map((m) => [m.id, m]))

  const termo = busca.trim().toLowerCase()
  const filtradas = notas.filter((n) => termo === ''
    || n.numero.toLowerCase().includes(termo)
    || n.fornecedor.toLowerCase().includes(termo)
    || n.itens.some((l) => l.numeroLote.toLowerCase().includes(termo)))

  const resumoMateriais = (nota: NotaFiscal) => {
    const codigos = [...new Set(nota.itens.map((l) => formatarCodigoMaterial(materialPorId.get(l.materialId)?.codigo)))]
    return codigos.length <= 2 ? codigos.join(', ') : `${codigos.slice(0, 2).join(', ')} +${codigos.length - 2}`
  }

  return (
    <Box>
      <PageHeader
        titulo="Entrada de notas"
        subtitulo={isLoading ? '…' : `${notas.length} ${notas.length === 1 ? 'nota fiscal' : 'notas fiscais'} no período`}
        breadcrumbs={[{ label: 'Suprimentos' }, { label: 'Entrada de notas' }]}
        action={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialogAberto(true)}>
            Nova entrada
          </Button>
        }
      />

      {isError && <Alert severity="error" sx={{ mb: 2 }}>Não foi possível carregar as notas fiscais.</Alert>}

      <Card>
        <Box sx={{ p: 2, display: 'flex', gap: 2, flexWrap: 'wrap', borderBottom: '1px solid', borderColor: 'divider' }}>
          <TextField
            placeholder="Buscar por nota, fornecedor ou lote…"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            sx={{ flex: 1, minWidth: 220 }}
          />
          <TextField
            label="Recebida de"
            type="date"
            value={periodoDe}
            onChange={(e) => setPeriodoDe(e.target.value)}
            slotProps={{ inputLabel: { shrink: true }, htmlInput: { max: periodoAte || undefined } }}
            sx={{ minWidth: 160 }}
          />
          <TextField
            label="até"
            type="date"
            value={periodoAte}
            onChange={(e) => setPeriodoAte(e.target.value)}
            error={periodoInvalido}
            helperText={periodoInvalido ? 'Anterior ao início.' : undefined}
            slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: periodoDe || undefined } }}
            sx={{ minWidth: 160 }}
          />
        </Box>

        {isLoading ? (
          <Skeleton variant="rounded" height={320} sx={{ m: 2 }} />
        ) : filtradas.length === 0 ? (
          <EmptyState
            Icone={ReceiptLongIcon}
            titulo="Nenhuma nota fiscal encontrada"
            descricao={notas.length > 0
              ? 'Nenhuma nota atende à busca. Ajuste o termo ou o período.'
              : 'Lance a primeira nota de compra: cada item vira um lote de matéria-prima no estoque.'}
            acaoLabel="Nova entrada"
            onAcao={() => setDialogAberto(true)}
          />
        ) : (
          <TableContainer>
            <Table size="small" sx={{ '& th': { whiteSpace: 'nowrap' } }}>
              <TableHead>
                <TableRow>
                  <TableCell>Nota fiscal</TableCell>
                  <TableCell>Fornecedor</TableCell>
                  <TableCell>Emissão</TableCell>
                  <TableCell>Recebimento</TableCell>
                  <TableCell align="center">Itens</TableCell>
                  <TableCell>Materiais</TableCell>
                  <TableCell>Lançada</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filtradas.map((nota) => (
                  <TableRow key={nota.id} hover onClick={() => navigate(`/notas-fiscais/${nota.id}`)} sx={{ cursor: 'pointer' }}>
                    <TableCell>
                      <Typography variant="body2" fontWeight={600} sx={{ fontFamily: 'monospace' }}>{nota.numero}</Typography>
                    </TableCell>
                    <TableCell><Typography variant="body2">{nota.fornecedor}</Typography></TableCell>
                    <TableCell><Typography variant="body2" color="text.secondary">{formatarData(nota.dataEmissao)}</Typography></TableCell>
                    <TableCell><Typography variant="body2">{formatarData(nota.dataRecebimento)}</Typography></TableCell>
                    <TableCell align="center">{nota.quantidadeItens}</TableCell>
                    <TableCell><Typography variant="body2" color="text.secondary" noWrap>{resumoMateriais(nota)}</Typography></TableCell>
                    <TableCell>
                      <Typography variant="body2" noWrap>{formatarDataHora(nota.registradaEm)}</Typography>
                      <Typography variant="caption" color="text.secondary">{nota.registradaPor}</Typography>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Card>

      <EntradaNotaFiscalDialog aberto={dialogAberto} onFechar={() => setDialogAberto(false)} onRegistrada={setRegistrada} />
      <Snackbar
        open={!!registrada}
        autoHideDuration={6000}
        onClose={() => setRegistrada(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity="success" variant="filled" onClose={() => setRegistrada(null)}
          action={registrada && (
            <Button color="inherit" size="small" onClick={() => navigate(`/notas-fiscais/${registrada.id}`)}>Ver nota</Button>
          )}>
          Nota {registrada?.numero} registrada: {registrada?.quantidadeItens}{' '}
          {registrada?.quantidadeItens === 1 ? 'lote entrou' : 'lotes entraram'} no estoque.
        </Alert>
      </Snackbar>
    </Box>
  )
}
