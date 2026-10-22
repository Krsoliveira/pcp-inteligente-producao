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

/** Linha clicável: abre o estoque do material (/estoque/:materialId). */
export function EstoqueTable({ posicoes }: { posicoes: PosicaoEstoque[] }) {
  const navigate = useNavigate()

  return (
    <TableContainer>
      <Table size="small" sx={{ '& th': { whiteSpace: 'nowrap' } }}>
        <TableHead>
          <TableRow>
            <TableCell>Código</TableCell>
            <TableCell>Descrição</TableCell>
            <TableCell>Tipo</TableCell>
            <TableCell align="right">Disponível</TableCell>
            <TableCell align="center">Lotes</TableCell>
            <TableCell align="right">Indisponível</TableCell>
            <TableCell>Próx. vencimento</TableCell>
            <TableCell>Última entrada</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {posicoes.map((p) => {
            const dias = p.proximoVencimento ? diasAte(p.proximoVencimento) : null
            const alerta = dias !== null && dias <= 30
            return (
              <TableRow key={p.materialId} hover onClick={() => navigate(`/estoque/${p.materialId}`)} sx={{ cursor: 'pointer' }}>
                <TableCell>
                  <Typography variant="body2" fontWeight={600} sx={{ fontFamily: 'monospace' }} noWrap>
                    {formatarCodigoMaterial(p.codigo)}
                  </Typography>
                </TableCell>
                <TableCell><Typography variant="body2">{p.descricao}</Typography></TableCell>
                <TableCell>
                  <Chip size="small" variant="outlined" label={TIPO_MATERIAL_LABEL[p.tipo]} sx={{ fontSize: '0.7rem' }} />
                </TableCell>
                <TableCell align="right">
                  <Typography variant="body2" fontWeight={600} noWrap
                    color={p.saldoDisponivel > 0 ? 'text.primary' : 'text.disabled'}>
                    {formatarQuantidade(p.saldoDisponivel, p.unidadeDeMedida)}
                  </Typography>
                </TableCell>
                <TableCell align="center">{p.lotesDisponiveis}</TableCell>
                <TableCell align="right">
                  <Typography variant="body2" noWrap color={p.saldoIndisponivel > 0 ? 'warning.main' : 'text.disabled'}>
                    {formatarQuantidade(p.saldoIndisponivel, p.unidadeDeMedida)}
                  </Typography>
                </TableCell>
                <TableCell>
                  {p.proximoVencimento ? (
                    <Tooltip title={alerta ? `Vence em ${dias} dias` : ''}>
                      <Typography variant="body2" noWrap color={alerta ? 'warning.main' : 'text.secondary'}
                        fontWeight={alerta ? 600 : 400}>
                        {formatarData(p.proximoVencimento)}
                      </Typography>
                    </Tooltip>
                  ) : <Typography variant="body2" color="text.disabled">—</Typography>}
                </TableCell>
                <TableCell>
                  <Typography variant="body2" color="text.secondary" noWrap>{formatarDataHora(p.ultimaEntrada)}</Typography>
                </TableCell>
              </TableRow>
            )
          })}
        </TableBody>
      </Table>
    </TableContainer>
  )
}
