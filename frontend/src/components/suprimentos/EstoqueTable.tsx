import { useNavigate } from 'react-router-dom'
import Chip from '@mui/material/Chip'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import { TIPO_MATERIAL_LABEL, type PosicaoEstoque } from '../../types'
import {
  diasAte,
  formatarCodigoMaterial,
  formatarData,
  formatarDataHora,
  formatarQuantidade,
} from '../../utils/formatacao'
import { CelulaDeCabecalho, type Coluna, type ControleDaTabela } from '../consulta/TabelaInterativa'

/** Colunas que a tela de resultados ordena e filtra; o texto é o que aparece na célula. */
export const colunasDeEstoque: Coluna<PosicaoEstoque>[] = [
  { chave: 'codigo', rotulo: 'Código', valor: (p) => p.codigo, texto: (p) => formatarCodigoMaterial(p.codigo) },
  { chave: 'descricao', rotulo: 'Descrição', valor: (p) => p.descricao },
  { chave: 'tipo', rotulo: 'Tipo', valor: (p) => TIPO_MATERIAL_LABEL[p.tipo] },
  { chave: 'disponivel', rotulo: 'Disponível', valor: (p) => p.saldoDisponivel, texto: (p) => formatarQuantidade(p.saldoDisponivel, p.unidadeDeMedida) },
  { chave: 'lotes', rotulo: 'Lotes', valor: (p) => p.lotesDisponiveis },
  { chave: 'indisponivel', rotulo: 'Indisponível', valor: (p) => p.saldoIndisponivel, texto: (p) => formatarQuantidade(p.saldoIndisponivel, p.unidadeDeMedida) },
  { chave: 'vencimento', rotulo: 'Próx. venc.', valor: (p) => p.proximoVencimento, texto: (p) => (p.proximoVencimento ? formatarData(p.proximoVencimento) : '') },
  { chave: 'entrada', rotulo: 'Última entrada', valor: (p) => p.ultimaEntrada, texto: (p) => (p.ultimaEntrada ? formatarDataHora(p.ultimaEntrada) : '') },
]

interface EstoqueTableProps {
  posicoes: PosicaoEstoque[]
  /** Quando informado, o cabeçalho ordena e filtra por coluna (tela de resultados). */
  controle?: ControleDaTabela<PosicaoEstoque>
}

/** Linha clicável: abre o estoque do material (/estoque/:materialId). */
export function EstoqueTable({ posicoes, controle }: EstoqueTableProps) {
  const navigate = useNavigate()

  return (
    <TableContainer>
      <Table size="small" sx={{ '& th': { whiteSpace: 'nowrap' } }}>
        <TableHead>
          <TableRow>
            <CelulaDeCabecalho chave="codigo" rotulo="Código" controle={controle} />
            <CelulaDeCabecalho chave="descricao" rotulo="Descrição" controle={controle} />
            <CelulaDeCabecalho chave="tipo" rotulo="Tipo" controle={controle} />
            <CelulaDeCabecalho chave="disponivel" rotulo="Disponível" controle={controle} align="right" />
            <CelulaDeCabecalho chave="lotes" rotulo="Lotes" controle={controle} align="center" />
            <CelulaDeCabecalho chave="indisponivel" rotulo="Indisponível" controle={controle} align="right" />
            <CelulaDeCabecalho chave="vencimento" rotulo="Próx. venc." controle={controle} />
            <CelulaDeCabecalho chave="entrada" rotulo="Última entrada" controle={controle} />
          </TableRow>
        </TableHead>
        <TableBody>
          {posicoes.map((p) => {
            const dias = p.proximoVencimento ? diasAte(p.proximoVencimento) : null
            const alerta = dias !== null && dias <= 30
            return (
              <TableRow key={p.materialId} hover onClick={() => navigate(`/estoque/${p.materialId}`)} sx={{ cursor: 'pointer' }}>
                <TableCell>
                  <Typography
                    variant="body2"
                    noWrap
                    sx={{ fontWeight: 600, fontFamily: 'monospace' }}>
                    {formatarCodigoMaterial(p.codigo)}
                  </Typography>
                </TableCell>
                <TableCell><Typography variant="body2">{p.descricao}</Typography></TableCell>
                <TableCell>
                  <Chip size="small" variant="outlined" label={TIPO_MATERIAL_LABEL[p.tipo]} sx={{ fontSize: '0.7rem' }} />
                </TableCell>
                <TableCell align="right">
                  <Typography variant="body2" noWrap sx={{ color: p.saldoDisponivel > 0 ? 'text.primary' : 'text.disabled', fontWeight: 600 }}>
                    {formatarQuantidade(p.saldoDisponivel, p.unidadeDeMedida)}
                  </Typography>
                </TableCell>
                <TableCell align="center">{p.lotesDisponiveis}</TableCell>
                <TableCell align="right">
                  <Typography variant="body2" noWrap sx={{ color: p.saldoIndisponivel > 0 ? 'warning.main' : 'text.disabled' }}>
                    {formatarQuantidade(p.saldoIndisponivel, p.unidadeDeMedida)}
                  </Typography>
                </TableCell>
                <TableCell>
                  {p.proximoVencimento ? (
                    <Tooltip title={alerta ? `Vence em ${dias} dias` : ''}>
                      <Typography variant="body2" noWrap sx={{ color: alerta ? 'warning.main' : 'text.secondary', fontWeight: alerta ? 600 : 400 }}>
                        {formatarData(p.proximoVencimento)}
                      </Typography>
                    </Tooltip>
                  ) : <Typography variant="body2" sx={{ color: 'text.disabled' }}>—</Typography>}
                </TableCell>
                <TableCell>
                  <Typography variant="body2" noWrap sx={{ color: 'text.secondary' }}>{formatarDataHora(p.ultimaEntrada)}</Typography>
                </TableCell>
              </TableRow>
            )
          })}
        </TableBody>
      </Table>
    </TableContainer>
  )
}
