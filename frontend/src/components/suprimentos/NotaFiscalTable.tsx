import { useNavigate } from 'react-router-dom'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Typography from '@mui/material/Typography'
import type { Material, NotaFiscal } from '../../types'
import { formatarCodigoMaterial, formatarData, formatarDataHora } from '../../utils/formatacao'

interface NotaFiscalTableProps {
  notas: NotaFiscal[]
  materialPorId: Map<string, Material>
}

/** Linha clicável: abre o detalhe da nota fiscal (/notas-fiscais/:id). */
export function NotaFiscalTable({ notas, materialPorId }: NotaFiscalTableProps) {
  const navigate = useNavigate()

  const resumoMateriais = (nota: NotaFiscal) => {
    const codigos = [...new Set(nota.itens.map((l) => formatarCodigoMaterial(materialPorId.get(l.materialId)?.codigo)))]
    return codigos.length <= 2 ? codigos.join(', ') : `${codigos.slice(0, 2).join(', ')} +${codigos.length - 2}`
  }

  return (
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
          {notas.map((nota) => (
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
  )
}
