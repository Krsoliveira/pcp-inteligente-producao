import Chip from '@mui/material/Chip'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Typography from '@mui/material/Typography'
import { TIPO_MATERIAL_LABEL, type Material, type TipoMaterial } from '../../types'
import { formatarCodigoMaterial } from '../../utils/formatacao'
import { CelulaDeCabecalho, LARGURA_MINIMA_DA_DESCRICAO, type Coluna, type ControleDaTabela } from '../consulta/TabelaInterativa'

export const TIPO_COR: Record<TipoMaterial, string> = {
  PRODUTO_ACABADO: '#4F46E5',
  SEMIACABADO:     '#92400E',
  MATERIA_PRIMA:   '#047857',
}

/** Colunas que a tela de resultados ordena e filtra; o texto é o que aparece na célula. */
export const colunasDeMateriais: Coluna<Material>[] = [
  { chave: 'codigo', rotulo: 'Código', valor: (m) => m.codigo, texto: (m) => formatarCodigoMaterial(m.codigo) },
  { chave: 'descricao', rotulo: 'Descrição', valor: (m) => m.descricao },
  { chave: 'tipo', rotulo: 'Tipo', valor: (m) => TIPO_MATERIAL_LABEL[m.tipo] },
  { chave: 'unidade', rotulo: 'Unidade', valor: (m) => m.unidadeDeMedida },
]

interface MaterialTableProps {
  materiais: Material[]
  /** Quando informado, o cabeçalho ordena e filtra por coluna (tela de resultados). */
  controle?: ControleDaTabela<Material>
}

export function MaterialTable({ materiais, controle }: MaterialTableProps) {
  return (
    <TableContainer>
      <Table size="small">
        <TableHead>
          <TableRow>
            <CelulaDeCabecalho chave="codigo" rotulo="Código" controle={controle} />
            <CelulaDeCabecalho chave="descricao" rotulo="Descrição" controle={controle} />
            <CelulaDeCabecalho chave="tipo" rotulo="Tipo" controle={controle} />
            <CelulaDeCabecalho chave="unidade" rotulo="Unidade" controle={controle} />
          </TableRow>
        </TableHead>
        <TableBody>
          {materiais.map((m) => (
            <TableRow key={m.id} hover>
              <TableCell><Typography
                variant="body2"
                noWrap
                sx={{ fontWeight: 600, fontFamily: 'monospace' }}>{formatarCodigoMaterial(m.codigo)}</Typography></TableCell>
              <TableCell sx={{ overflowWrap: 'anywhere', minWidth: LARGURA_MINIMA_DA_DESCRICAO }}>{m.descricao}</TableCell>
              <TableCell>
                <Chip label={TIPO_MATERIAL_LABEL[m.tipo]} size="small"
                  sx={{ bgcolor: `${TIPO_COR[m.tipo]}14`, color: TIPO_COR[m.tipo], fontWeight: 600, fontSize: '0.7rem', border: 'none' }} />
              </TableCell>
              <TableCell><Typography variant="body2" sx={{ color: 'text.secondary' }}>{m.unidadeDeMedida}</Typography></TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )
}
