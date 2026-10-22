import { useNavigate } from 'react-router-dom'
import Box from '@mui/material/Box'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import { StatusLoteBadge } from '../StatusBadge'
import { diasAte, formatarCodigoMaterial, formatarData, formatarQuantidade } from '../../utils/formatacao'
import type { Lote, Material } from '../../types'

interface TabelaLotesProps {
  lotes: Lote[]
  /** Mostra a coluna de material (itens de uma nota) ou a de origem (lotes de um material). */
  coluna: 'material' | 'origem'
  materialPorId?: Map<string, Material>
}

/** Lotes clicáveis (→ detalhe do lote), com saldo, validade e status. */
export function TabelaLotes({ lotes, coluna, materialPorId }: TabelaLotesProps) {
  const navigate = useNavigate()
  return (
    <TableContainer>
      <Table size="small" sx={{ '& th': { whiteSpace: 'nowrap' } }}>
        <TableHead>
          <TableRow>
            <TableCell>Lote</TableCell>
            <TableCell>{coluna === 'material' ? 'Material' : 'Origem'}</TableCell>
            <TableCell align="right">Saldo / Qtd</TableCell>
            <TableCell>Fabricação</TableCell>
            <TableCell>Validade</TableCell>
            <TableCell>Status</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {lotes.map((lote) => {
            const dias = diasAte(lote.dataValidade)
            const vencendo = lote.status === 'DISPONIVEL' && dias >= 0 && dias <= 30
            const material = materialPorId?.get(lote.materialId)
            return (
              <TableRow key={lote.id} hover onClick={() => navigate(`/lotes/${lote.id}`)} sx={{ cursor: 'pointer' }}>
                <TableCell>
                  <Typography variant="body2" fontWeight={600} sx={{ fontFamily: 'monospace' }} noWrap>{lote.numeroLote}</Typography>
                </TableCell>
                <TableCell>
                  {coluna === 'material' ? (
                    <Typography variant="body2" noWrap>
                      {formatarCodigoMaterial(material?.codigo)}
                      <Typography component="span" variant="caption" color="text.secondary">
                        {material ? ` · ${material.descricao}` : ''}
                      </Typography>
                    </Typography>
                  ) : lote.origem === 'COMPRA' ? (
                    <Tooltip title={`Fornecedor: ${lote.fornecedor}`}>
                      <Typography variant="body2" noWrap>Compra · NF {lote.notaFiscal}</Typography>
                    </Tooltip>
                  ) : (
                    <Typography variant="body2" color="text.secondary">Produção</Typography>
                  )}
                </TableCell>
                <TableCell align="right">
                  <Typography variant="body2" noWrap>
                    <strong>{formatarQuantidade(lote.saldo)}</strong>
                    {` / ${formatarQuantidade(lote.quantidade, lote.unidadeDeMedida)}`}
                  </Typography>
                </TableCell>
                <TableCell>
                  <Typography variant="body2" color="text.secondary">{formatarData(lote.dataFabricacao)}</Typography>
                </TableCell>
                <TableCell>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
                    <Typography variant="body2" color={vencendo ? 'warning.main' : 'text.secondary'} fontWeight={vencendo ? 600 : 400}>
                      {formatarData(lote.dataValidade)}
                    </Typography>
                    {vencendo && (
                      <Tooltip title={`Vence em ${dias} dias`}>
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
  )
}
