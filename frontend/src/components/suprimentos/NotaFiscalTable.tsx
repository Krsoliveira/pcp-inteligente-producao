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
import { CelulaDeCabecalho, type Coluna, type ControleDaTabela } from '../consulta/TabelaInterativa'

interface NotaFiscalTableProps {
  notas: NotaFiscal[]
  materialPorId: Map<string, Material>
  /** Quando informado, o cabeçalho ordena e filtra por coluna (tela de resultados). */
  controle?: ControleDaTabela<NotaFiscal>
}

const codigosDosMateriais = (nota: NotaFiscal, materialPorId: Map<string, Material>) =>
  [...new Set(nota.itens.map((l) => formatarCodigoMaterial(materialPorId.get(l.materialId)?.codigo)))]

/** Colunas que a tela de resultados ordena e filtra; o texto é o que aparece na célula. */
export function colunasDeNotas(materialPorId: Map<string, Material>): Coluna<NotaFiscal>[] {
  return [
    { chave: 'numero', rotulo: 'Nota fiscal', valor: (n) => n.numero },
    { chave: 'fornecedor', rotulo: 'Fornecedor', valor: (n) => n.fornecedor },
    { chave: 'emissao', rotulo: 'Emissão', valor: (n) => n.dataEmissao, texto: (n) => formatarData(n.dataEmissao) },
    { chave: 'recebimento', rotulo: 'Recebimento', valor: (n) => n.dataRecebimento, texto: (n) => formatarData(n.dataRecebimento) },
    { chave: 'itens', rotulo: 'Itens', valor: (n) => n.quantidadeItens },
    { chave: 'materiais', rotulo: 'Materiais', semOrdenacao: true, valor: () => null, texto: (n) => codigosDosMateriais(n, materialPorId).join(' ') },
    { chave: 'lancada', rotulo: 'Lançada', valor: (n) => n.registradaEm, texto: (n) => `${formatarDataHora(n.registradaEm)} ${n.registradaPor}` },
  ]
}

/** Linha clicável: abre o detalhe da nota fiscal (/notas-fiscais/:id). */
export function NotaFiscalTable({ notas, materialPorId, controle }: NotaFiscalTableProps) {
  const navigate = useNavigate()

  const resumoMateriais = (nota: NotaFiscal) => {
    const codigos = codigosDosMateriais(nota, materialPorId)
    return codigos.length <= 2 ? codigos.join(', ') : `${codigos.slice(0, 2).join(', ')} +${codigos.length - 2}`
  }

  return (
    <TableContainer>
      <Table size="small" sx={{ '& th': { whiteSpace: 'nowrap' } }}>
        <TableHead>
          <TableRow>
            <CelulaDeCabecalho chave="numero" rotulo="Nota fiscal" controle={controle} />
            <CelulaDeCabecalho chave="fornecedor" rotulo="Fornecedor" controle={controle} />
            <CelulaDeCabecalho chave="emissao" rotulo="Emissão" controle={controle} />
            <CelulaDeCabecalho chave="recebimento" rotulo="Recebimento" controle={controle} />
            <CelulaDeCabecalho chave="itens" rotulo="Itens" controle={controle} align="center" />
            <CelulaDeCabecalho chave="materiais" rotulo="Materiais" controle={controle} />
            <CelulaDeCabecalho chave="lancada" rotulo="Lançada" controle={controle} />
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
