import { useNavigate } from 'react-router-dom'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import IconButton from '@mui/material/IconButton'
import Tooltip from '@mui/material/Tooltip'
import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import EditIcon from '@mui/icons-material/Edit'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import OpenInNewIcon from '@mui/icons-material/OpenInNew'
import ListAltIcon from '@mui/icons-material/ListAlt'
import { StatusOrdemBadge } from './StatusBadge'
import { EmptyState } from './EmptyState'
import type { OrdemProducao } from '../types'

interface OrdemTableProps {
  ordens: OrdemProducao[]
  onAtualizarStatus: (ordem: OrdemProducao) => void
  mostrarAcoes?: boolean
}

export function OrdemTable({ ordens, onAtualizarStatus, mostrarAcoes = true }: OrdemTableProps) {
  const navigate = useNavigate()

  if (ordens.length === 0) {
    return (
      <EmptyState
        Icone={ListAltIcon}
        titulo="Nenhuma ordem encontrada"
        descricao="Crie uma nova ordem de produção usando o botão acima."
      />
    )
  }

  return (
    <TableContainer>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Código</TableCell>
            <TableCell>Centro de Trabalho</TableCell>
            <TableCell align="right">Qtd</TableCell>
            <TableCell>Início</TableCell>
            <TableCell>Fim</TableCell>
            <TableCell>Status</TableCell>
            <TableCell align="center" sx={{ width: 40 }}></TableCell>
            <TableCell align="center" sx={{ width: 80 }}>Ações</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {ordens.map((ordem) => (
            <TableRow
              key={ordem.id}
              hover
              onClick={() => navigate(`/ordens/${ordem.id}`)}
              sx={{ cursor: 'pointer' }}
            >
              <TableCell>
                <Typography variant="body2" fontWeight={600} color="primary">
                  {ordem.codigo}
                </Typography>
              </TableCell>
              <TableCell>
                <Typography variant="body2">{ordem.centroDeTrabalho}</Typography>
              </TableCell>
              <TableCell align="right">
                <Typography variant="body2">{ordem.quantidade.toLocaleString('pt-BR')}</Typography>
              </TableCell>
              <TableCell>
                <Typography variant="body2" color="text.secondary">
                  {formatarData(ordem.inicioPlanejado)}
                </Typography>
              </TableCell>
              <TableCell>
                <Typography variant="body2" color="text.secondary">
                  {formatarData(ordem.fimPlanejado)}
                </Typography>
              </TableCell>
              <TableCell>
                <StatusOrdemBadge status={ordem.status} />
              </TableCell>
              <TableCell align="center">
                {ordem.atrasada && (
                  <Tooltip title="Ordem atrasada">
                    <WarningAmberIcon color="error" sx={{ fontSize: 16 }} />
                  </Tooltip>
                )}
              </TableCell>
              <TableCell align="center" onClick={(e) => e.stopPropagation()}>
                <Box sx={{ display: 'flex', gap: 0.5, justifyContent: 'center' }}>
                  {mostrarAcoes && !['CONCLUIDA', 'CANCELADA'].includes(ordem.status) && (
                    <Tooltip title="Atualizar status">
                      <IconButton
                        size="small"
                        onClick={(e) => { e.stopPropagation(); onAtualizarStatus(ordem) }}
                        color="primary"
                      >
                        <EditIcon sx={{ fontSize: 16 }} />
                      </IconButton>
                    </Tooltip>
                  )}
                  <Tooltip title="Ver detalhes">
                    <IconButton
                      size="small"
                      onClick={() => navigate(`/ordens/${ordem.id}`)}
                      color="default"
                    >
                      <OpenInNewIcon sx={{ fontSize: 16 }} />
                    </IconButton>
                  </Tooltip>
                </Box>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )
}

function formatarData(data: string): string {
  const [ano, mes, dia] = data.split('-')
  return `${dia}/${mes}/${ano}`
}
