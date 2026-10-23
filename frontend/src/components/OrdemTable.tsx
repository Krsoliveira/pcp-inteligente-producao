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
import { StatusOrdemBadge, rotuloDoStatusDeOrdem } from './StatusBadge'
import { CelulaDeCabecalho, type Coluna, type ControleDaTabela } from './consulta/TabelaInterativa'

import { EmptyState } from './EmptyState'
import type { OrdemProducao } from '../types'

interface OrdemTableProps {
  ordens: OrdemProducao[]
  onAtualizarStatus: (ordem: OrdemProducao) => void
  mostrarAcoes?: boolean
  /** Quando informado, o cabeçalho ordena e filtra por coluna (tela de resultados). */
  controle?: ControleDaTabela<OrdemProducao>
}

/** Colunas que a tela de resultados ordena e filtra; o texto é o que aparece na célula. */
export const colunasDeOrdens: Coluna<OrdemProducao>[] = [
  { chave: 'codigo', rotulo: 'Código', valor: (o) => o.codigo },
  { chave: 'centro', rotulo: 'Centro de Trabalho', valor: (o) => o.centroDeTrabalho },
  { chave: 'quantidade', rotulo: 'Qtd', valor: (o) => o.quantidade, texto: (o) => o.quantidade.toLocaleString('pt-BR') },
  { chave: 'inicio', rotulo: 'Início', valor: (o) => o.inicioPlanejado, texto: (o) => formatarData(o.inicioPlanejado) },
  { chave: 'fim', rotulo: 'Fim', valor: (o) => o.fimPlanejado, texto: (o) => formatarData(o.fimPlanejado) },
  { chave: 'status', rotulo: 'Status', valor: (o) => rotuloDoStatusDeOrdem(o.status) },
]

export function OrdemTable({ ordens, onAtualizarStatus, mostrarAcoes = true, controle }: OrdemTableProps) {
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
        <TableHead sx={{ '& th': { whiteSpace: 'nowrap' } }}>
          <TableRow>
            <CelulaDeCabecalho chave="codigo" rotulo="Código" controle={controle} />
            <CelulaDeCabecalho chave="centro" rotulo="Centro de Trabalho" controle={controle} />
            <CelulaDeCabecalho chave="quantidade" rotulo="Qtd" controle={controle} align="right" />
            <CelulaDeCabecalho chave="inicio" rotulo="Início" controle={controle} />
            <CelulaDeCabecalho chave="fim" rotulo="Fim" controle={controle} />
            <CelulaDeCabecalho chave="status" rotulo="Status" controle={controle} />
            <TableCell align="center" sx={{ width: 40 }}></TableCell>
            {mostrarAcoes && <TableCell align="center" sx={{ width: 80 }}>Ações</TableCell>}
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
                <Typography variant="body2" fontWeight={600} color="primary" noWrap>
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
                <Typography variant="body2" color="text.secondary" noWrap>
                  {formatarData(ordem.inicioPlanejado)}
                </Typography>
              </TableCell>
              <TableCell>
                <Typography variant="body2" color="text.secondary" noWrap>
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
              {mostrarAcoes && (
              <TableCell align="center" onClick={(e) => e.stopPropagation()}>
                <Box sx={{ display: 'flex', gap: 0.5, justifyContent: 'center' }}>
                  {!['CONCLUIDA', 'CANCELADA'].includes(ordem.status) && (
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
              )}
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
