import { useState } from 'react'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Link from '@mui/material/Link'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'

export interface LinhaDoGrafico {
  rotulo: string
  valores: number[]
  /** Leva à mesma lista que o clique na barra; ausente quando o perfil não pode ver as ordens. */
  onSelecionar?: () => void
}

interface DadosDoGraficoProps {
  titulo: string
  /** Título da primeira coluna, a dos rótulos. */
  rotuloDaColuna: string
  colunas: string[]
  linhas: LinhaDoGrafico[]
}

/**
 * Os números de um gráfico em forma de tabela. O ECharts desenha em canvas, que o teclado e o leitor
 * de tela não alcançam: é por aqui que quem não usa o mouse lê os valores e abre a lista de cada barra.
 */
export function DadosDoGrafico({ titulo, rotuloDaColuna, colunas, linhas }: DadosDoGraficoProps) {
  const [aberto, setAberto] = useState(false)
  const acao = aberto ? 'Ocultar' : 'Ver'

  return (
    <Box sx={{ mt: 1 }}>
      <Button size="small" aria-expanded={aberto} aria-label={`${acao} os números de ${titulo}`} onClick={() => setAberto(!aberto)}>
        {acao} os números
      </Button>
      {aberto && (
        <TableContainer sx={{ mt: 1 }}>
          <Table size="small" aria-label={titulo}>
            <TableHead>
              <TableRow>
                <TableCell>{rotuloDaColuna}</TableCell>
                {colunas.map((coluna) => <TableCell key={coluna} align="right">{coluna}</TableCell>)}
              </TableRow>
            </TableHead>
            <TableBody>
              {linhas.map((linha) => (
                <TableRow key={linha.rotulo}>
                  <TableCell>
                    {linha.onSelecionar
                      ? <Link component="button" variant="body2" onClick={linha.onSelecionar}>{linha.rotulo}</Link>
                      : linha.rotulo}
                  </TableCell>
                  {linha.valores.map((valor, i) => <TableCell key={colunas[i]} align="right">{valor}</TableCell>)}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  )
}
