import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Chip from '@mui/material/Chip'
import IconButton from '@mui/material/IconButton'
import Tooltip from '@mui/material/Tooltip'
import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import EditIcon from '@mui/icons-material/Edit'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import type { OrdemProducao, StatusOrdem } from '../types'

const STATUS_COR: Record<StatusOrdem, 'default' | 'info' | 'warning' | 'secondary' | 'success' | 'error'> = {
  PLANEJADA: 'info',
  LIBERADA: 'warning',
  EM_PRODUCAO: 'secondary',
  CONCLUIDA: 'success',
  CANCELADA: 'error',
}

const STATUS_LABEL: Record<StatusOrdem, string> = {
  PLANEJADA: 'Planejada',
  LIBERADA: 'Liberada',
  EM_PRODUCAO: 'Em Produção',
  CONCLUIDA: 'Concluída',
  CANCELADA: 'Cancelada',
}

interface OrdemTableProps {
  ordens: OrdemProducao[]
  onAtualizarStatus: (ordem: OrdemProducao) => void
}

export function OrdemTable({ ordens, onAtualizarStatus }: OrdemTableProps) {
  if (ordens.length === 0) {
    return (
      <Box sx={{ py: 8, textAlign: 'center' }}>
        <Typography color="text.secondary">
          Nenhuma ordem encontrada. Crie a primeira usando o botão acima.
        </Typography>
      </Box>
    )
  }

  return (
    <TableContainer>
      <Table size="small">
        <TableHead>
          <TableRow sx={{ '& th': { fontWeight: 600, bgcolor: 'grey.50' } }}>
            <TableCell>Código</TableCell>
            <TableCell>Produto</TableCell>
            <TableCell align="right">Qtd</TableCell>
            <TableCell>Início Planejado</TableCell>
            <TableCell>Fim Planejado</TableCell>
            <TableCell>Status</TableCell>
            <TableCell align="center">Atraso</TableCell>
            <TableCell align="center">Ações</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {ordens.map((ordem) => (
            <TableRow
              key={ordem.id}
              hover
              sx={{ '&:last-child td': { border: 0 } }}
            >
              <TableCell>
                <Typography variant="body2" fontWeight={500}>
                  {ordem.codigo}
                </Typography>
              </TableCell>
              <TableCell>{ordem.produto}</TableCell>
              <TableCell align="right">{ordem.quantidade.toLocaleString('pt-BR')}</TableCell>
              <TableCell>{formatarData(ordem.inicioPlanejado)}</TableCell>
              <TableCell>{formatarData(ordem.fimPlanejado)}</TableCell>
              <TableCell>
                <Chip
                  label={STATUS_LABEL[ordem.status]}
                  color={STATUS_COR[ordem.status]}
                  size="small"
                  variant="outlined"
                />
              </TableCell>
              <TableCell align="center">
                {ordem.atrasada && (
                  <Tooltip title="Ordem atrasada">
                    <WarningAmberIcon color="error" fontSize="small" />
                  </Tooltip>
                )}
              </TableCell>
              <TableCell align="center">
                {!['CONCLUIDA', 'CANCELADA'].includes(ordem.status) && (
                  <Tooltip title="Atualizar status">
                    <IconButton
                      size="small"
                      onClick={() => onAtualizarStatus(ordem)}
                      color="primary"
                    >
                      <EditIcon fontSize="small" />
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

function formatarData(data: string): string {
  // "2026-08-10" → "10/08/2026"
  const [ano, mes, dia] = data.split('-')
  return `${dia}/${mes}/${ano}`
}
