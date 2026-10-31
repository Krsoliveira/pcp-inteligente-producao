import { useState } from 'react'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import TableCell from '@mui/material/TableCell'
import IconButton from '@mui/material/IconButton'
import Popover from '@mui/material/Popover'
import TextField from '@mui/material/TextField'
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward'
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward'
import FilterAltIcon from '@mui/icons-material/FilterAlt'
import FilterAltOutlinedIcon from '@mui/icons-material/FilterAltOutlined'
import UnfoldMoreIcon from '@mui/icons-material/UnfoldMore'

/**
 * Uma coluna da tabela de resultados. `valor` é o que se compara ao ordenar; `texto` é o que o
 * usuário vê na célula (e onde o filtro da coluna procura). Sem `texto`, usa o próprio `valor`.
 */
export interface Coluna<T> {
  chave: string
  rotulo: string
  valor: (item: T) => string | number | null | undefined
  texto?: (item: T) => string
  /** Sem ordenação (ex.: coluna de resumo). */
  semOrdenacao?: boolean
  /** Sem filtro (ex.: coluna de ações). */
  semFiltro?: boolean
}

export interface ControleDaTabela<T> {
  colunas: Coluna<T>[]
  chaveOrdenada: string | null
  crescente: boolean
  alternarOrdem: (chave: string) => void
  filtros: Record<string, string>
  setFiltro: (chave: string, texto: string) => void
  limparFiltros: () => void
  /** Há algum filtro de coluna preenchido? */
  filtrando: boolean
}

const comparador = new Intl.Collator('pt-BR', { numeric: true, sensitivity: 'base' })
const vazio = (v: unknown) => v == null || v === ''
const textoDa = <T,>(coluna: Coluna<T>, item: T) => coluna.texto?.(item) ?? String(coluna.valor(item) ?? '')

/**
 * Filtra e classifica, no próprio navegador, as linhas que já estão na tela (a página atual do
 * resultado). Não consulta o servidor. Cada clique na coluna alterna crescente, decrescente e
 * ordem original. Valores vazios ficam sempre no fim.
 */
export function useTabelaInterativa<T>(itens: T[], colunas: Coluna<T>[]) {
  const [chaveOrdenada, setChaveOrdenada] = useState<string | null>(null)
  const [crescente, setCrescente] = useState(true)
  const [filtros, setFiltros] = useState<Record<string, string>>({})

  const alternarOrdem = (chave: string) => {
    if (chaveOrdenada !== chave) { setChaveOrdenada(chave); setCrescente(true) }
    else if (crescente) setCrescente(false)
    else { setChaveOrdenada(null); setCrescente(true) }
  }
  const setFiltro = (chave: string, texto: string) => setFiltros((atual) => ({ ...atual, [chave]: texto }))
  const limparFiltros = () => setFiltros({})

  const termos = colunas
    .map((coluna) => ({ coluna, termo: (filtros[coluna.chave] ?? '').trim().toLowerCase() }))
    .filter((t) => t.termo !== '')
  const filtrados = termos.length === 0
    ? itens
    : itens.filter((item) => termos.every(({ coluna, termo }) => textoDa(coluna, item).toLowerCase().includes(termo)))

  const ordenacao = colunas.find((c) => c.chave === chaveOrdenada)
  const linhas = ordenacao === undefined ? filtrados : [...filtrados].sort((a, b) => {
    const va = ordenacao.valor(a)
    const vb = ordenacao.valor(b)
    if (vazio(va)) return vazio(vb) ? 0 : 1
    if (vazio(vb)) return -1
    const resultado = typeof va === 'number' && typeof vb === 'number' ? va - vb : comparador.compare(String(va), String(vb))
    return crescente ? resultado : -resultado
  })

  const controle: ControleDaTabela<T> = {
    colunas, chaveOrdenada, crescente, alternarOrdem, filtros, setFiltro, limparFiltros, filtrando: termos.length > 0,
  }
  return { linhas, controle }
}

interface CelulaDeCabecalhoProps<T> {
  chave: string
  /** Sem controle (ex.: tabela do dashboard), mostra só o rótulo. */
  controle?: ControleDaTabela<T>
  rotulo: string
  align?: 'left' | 'center' | 'right'
}

/**
 * Célula de título no estilo planilha: o rótulo e, ao lado, um ícone para ordenar (alterna
 * crescente, decrescente e original) e um funil que abre o filtro da coluna.
 */
export function CelulaDeCabecalho<T>({ chave, controle, rotulo, align }: CelulaDeCabecalhoProps<T>) {
  const [ancora, setAncora] = useState<HTMLElement | null>(null)
  const coluna = controle?.colunas.find((c) => c.chave === chave)
  if (!controle || !coluna) return <TableCell align={align}>{rotulo}</TableCell>

  const ativa = controle.chaveOrdenada === chave
  const filtro = controle.filtros[chave] ?? ''
  const filtroAtivo = filtro.trim() !== ''
  const IconeDeOrdem = !ativa ? UnfoldMoreIcon : controle.crescente ? ArrowUpwardIcon : ArrowDownwardIcon

  return (
    <TableCell align={align} sortDirection={ativa ? (controle.crescente ? 'asc' : 'desc') : false}>
      <Box sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.25 }}>
        {rotulo}
        {!coluna.semOrdenacao && (
          <Tooltip title={ativa ? (controle.crescente ? 'Crescente — clique para inverter' : 'Decrescente — clique para remover') : 'Ordenar'}>
            <IconButton size="small" sx={{ p: 0.25 }} aria-label={`Ordenar ${rotulo}`} color={ativa ? 'primary' : 'default'} onClick={() => controle.alternarOrdem(chave)}>
              <IconeDeOrdem sx={{ fontSize: 16 }} />
            </IconButton>
          </Tooltip>
        )}
        {!coluna.semFiltro && (
          <Tooltip title="Filtrar">
            <IconButton size="small" sx={{ p: 0.25 }} aria-label={`Filtrar ${rotulo}`} color={filtroAtivo ? 'primary' : 'default'} onClick={(e) => setAncora(e.currentTarget)}>
              {filtroAtivo ? <FilterAltIcon sx={{ fontSize: 16 }} /> : <FilterAltOutlinedIcon sx={{ fontSize: 16 }} />}
            </IconButton>
          </Tooltip>
        )}
      </Box>
      <Popover
        open={ancora !== null}
        anchorEl={ancora}
        onClose={() => setAncora(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
      >
        <Box sx={{ p: 1.5, display: 'flex', gap: 1, alignItems: 'center' }}>
          <TextField
            autoFocus
            size="small"
            placeholder={`Filtrar ${rotulo.toLowerCase()}…`}
            value={filtro}
            onChange={(e) => controle.setFiltro(chave, e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') setAncora(null) }}
            slotProps={{ htmlInput: { 'aria-label': `Texto do filtro de ${rotulo}` } }}
          />
          <Button size="small" disabled={!filtroAtivo} onClick={() => controle.setFiltro(chave, '')}>Limpar</Button>
        </Box>
      </Popover>
    </TableCell>
  )
}

interface ResumoDaTabelaProps {
  exibidas: number
  naPagina: number
  filtrando: boolean
  onLimpar: () => void
}

/** Faixa acima da tabela: quantas linhas da página aparecem e como limpar os filtros das colunas. */
export function ResumoDaTabela({ exibidas, naPagina, filtrando, onLimpar }: ResumoDaTabelaProps) {
  return (
    <Box sx={{ px: 2, py: 1, display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap', borderBottom: '1px solid', borderColor: 'divider' }}>
      <Typography variant="caption" sx={{ color: 'text.secondary' }}>
        {filtrando ? `Mostrando ${exibidas} de ${naPagina} linhas desta página` : `${naPagina} linhas nesta página`}
        {' · use os ícones do título para ordenar e filtrar'}
      </Typography>
      {filtrando && <Button size="small" onClick={onLimpar}>Limpar filtros das colunas</Button>}
    </Box>
  )
}
