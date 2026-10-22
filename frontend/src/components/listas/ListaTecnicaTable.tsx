import { useNavigate } from 'react-router-dom'
import Chip from '@mui/material/Chip'
import IconButton from '@mui/material/IconButton'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import { StatusListaBadge } from '../StatusBadge'
import type { ListaTecnica } from '../../types'

interface ListaTecnicaTableProps {
  listas: ListaTecnica[]
  nomeDoMaterial: (materialId: string) => string
  onAtivar: (id: string) => void
}

/** Linha clicável: abre o detalhe da lista técnica (/listas-tecnicas/:id). */
export function ListaTecnicaTable({ listas, nomeDoMaterial, onAtivar }: ListaTecnicaTableProps) {
  const navigate = useNavigate()

  return (
    <TableContainer>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Material</TableCell>
            <TableCell>Versão</TableCell>
            <TableCell align="center">Componentes</TableCell>
            <TableCell>Status</TableCell>
            <TableCell align="center" sx={{ width: 80 }}>Ações</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {listas.map((l) => (
            <TableRow key={l.id} hover onClick={() => navigate(`/listas-tecnicas/${l.id}`)} sx={{ cursor: 'pointer' }}>
              <TableCell>
                <Typography variant="body2" fontWeight={500}>{nomeDoMaterial(l.materialId)}</Typography>
              </TableCell>
              <TableCell>
                <Chip label={l.versao} size="small" variant="outlined" sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }} />
              </TableCell>
              <TableCell align="center"><Typography variant="body2">{l.itens.length}</Typography></TableCell>
              <TableCell><StatusListaBadge status={l.status} /></TableCell>
              <TableCell align="center">
                {l.status === 'EM_REVISAO' && (
                  <Tooltip title="Ativar esta versão">
                    <IconButton size="small" color="success" onClick={(e) => { e.stopPropagation(); onAtivar(l.id) }}>
                      <CheckCircleOutlineIcon sx={{ fontSize: 16 }} />
                    </IconButton>
                  </Tooltip>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )
}
