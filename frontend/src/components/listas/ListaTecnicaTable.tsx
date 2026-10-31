import { useNavigate } from 'react-router-dom'
import { usePermissoes } from '../../hooks/usePermissoes'
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
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutlined'
import BlockIcon from '@mui/icons-material/Block'
import { StatusListaBadge, rotuloDoStatusDeLista } from '../StatusBadge'
import { CelulaDeCabecalho, type Coluna, type ControleDaTabela } from '../consulta/TabelaInterativa'
import type { ListaTecnica, Material } from '../../types'
import { formatarCodigoMaterial } from '../../utils/formatacao'

interface ListaTecnicaTableProps {
  listas: ListaTecnica[]
  materialPorId: Map<string, Material>
  onAtivar: (id: string) => void
  /** Pede para retirar a versão de uso (a tela confirma antes). */
  onRetirarDeUso: (lista: ListaTecnica) => void
  /** Quando informado, o cabeçalho ordena e filtra por coluna (consulta e resultados). */
  controle?: ControleDaTabela<ListaTecnica>
}

/** Colunas que as telas ordenam e filtram; o texto é o que aparece na célula. */
export function colunasDeListas(materialPorId: Map<string, Material>): Coluna<ListaTecnica>[] {
  return [
    {
      chave: 'codigo', rotulo: 'Código',
      valor: (l) => materialPorId.get(l.materialId)?.codigo,
      texto: (l) => formatarCodigoMaterial(materialPorId.get(l.materialId)?.codigo),
    },
    { chave: 'descricao', rotulo: 'Descrição', valor: (l) => materialPorId.get(l.materialId)?.descricao },
    { chave: 'versao', rotulo: 'Versão', valor: (l) => l.versao },
    { chave: 'componentes', rotulo: 'Componentes', valor: (l) => l.itens.length },
    { chave: 'status', rotulo: 'Status', valor: (l) => rotuloDoStatusDeLista(l.status) },
  ]
}

/** Linha clicável: abre o detalhe da lista técnica (/listas-tecnicas/:id). */
export function ListaTecnicaTable({ listas, materialPorId, onAtivar, onRetirarDeUso, controle }: ListaTecnicaTableProps) {
  const navigate = useNavigate()
  const { tem } = usePermissoes()

  return (
    <TableContainer>
      <Table size="small">
        <TableHead>
          <TableRow>
            <CelulaDeCabecalho chave="codigo" rotulo="Código" controle={controle} />
            <CelulaDeCabecalho chave="descricao" rotulo="Descrição" controle={controle} />
            <CelulaDeCabecalho chave="versao" rotulo="Versão" controle={controle} />
            <CelulaDeCabecalho chave="componentes" rotulo="Componentes" controle={controle} align="center" />
            <CelulaDeCabecalho chave="status" rotulo="Status" controle={controle} />
            <TableCell align="center" sx={{ width: 90 }}>Ações</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {listas.map((l) => {
            const material = materialPorId.get(l.materialId)
            return (
              <TableRow key={l.id} hover onClick={() => navigate(`/listas-tecnicas/${l.id}`)} sx={{ cursor: 'pointer' }}>
                <TableCell>
                  <Typography
                    variant="body2"
                    noWrap
                    sx={{ fontWeight: 600, fontFamily: 'monospace' }}>
                    {material ? formatarCodigoMaterial(material.codigo) : '—'}
                  </Typography>
                </TableCell>
                <TableCell><Typography variant="body2">{material?.descricao ?? `${l.materialId.substring(0, 8)}…`}</Typography></TableCell>
                <TableCell>
                  <Chip label={l.versao} size="small" variant="outlined" sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }} />
                </TableCell>
                <TableCell align="center"><Typography variant="body2">{l.itens.length}</Typography></TableCell>
                <TableCell><StatusListaBadge status={l.status} /></TableCell>
                <TableCell align="center">
                  {l.status === 'EM_REVISAO' && tem('LISTA_ATIVAR') && (
                    <Tooltip title="Ativar esta versão">
                      <IconButton size="small" color="success" aria-label={`Ativar versão ${l.versao}`}
                        onClick={(e) => { e.stopPropagation(); onAtivar(l.id) }}>
                        <CheckCircleOutlineIcon sx={{ fontSize: 16 }} />
                      </IconButton>
                    </Tooltip>
                  )}
                  {l.status === 'ATIVA' && tem('LISTA_RETIRAR_DE_USO') && (
                    <Tooltip title="Retirar de uso">
                      <IconButton size="small" color="warning" aria-label={`Retirar versão ${l.versao} de uso`}
                        onClick={(e) => { e.stopPropagation(); onRetirarDeUso(l) }}>
                        <BlockIcon sx={{ fontSize: 16 }} />
                      </IconButton>
                    </Tooltip>
                  )}
                </TableCell>
              </TableRow>
            )
          })}
        </TableBody>
      </Table>
    </TableContainer>
  )
}
