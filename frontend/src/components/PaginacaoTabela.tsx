import TablePagination from '@mui/material/TablePagination'

interface PaginacaoTabelaProps {
  total: number
  /** Página atual, começando em 0. */
  pagina: number
  tamanho: number
  onPagina: (pagina: number) => void
  onTamanho: (tamanho: number) => void
}

/** Quantas páginas existem para `total` linhas; sempre ao menos 1, mesmo sem resultados. */
export const totalDePaginas = (total: number, tamanho: number) => Math.max(1, Math.ceil(total / tamanho))

/** Rodapé de paginação das tabelas, com os textos em português e a página atual explícita ("página 2 de 7"). */
export function PaginacaoTabela({ total, pagina, tamanho, onPagina, onTamanho }: PaginacaoTabelaProps) {
  return (
    <TablePagination
      component="div"
      count={total}
      page={pagina}
      rowsPerPage={tamanho}
      rowsPerPageOptions={[10, 25, 50, 100]}
      showFirstButton
      showLastButton
      onPageChange={(_, novaPagina) => onPagina(novaPagina)}
      onRowsPerPageChange={(e) => onTamanho(Number(e.target.value))}
      labelRowsPerPage="Linhas por página"
      labelDisplayedRows={({ from, to, count, page }) =>
        `${from}–${to} de ${count} · página ${page + 1} de ${totalDePaginas(count, tamanho)}`}
      getItemAriaLabel={(tipo) => ({ first: 'Primeira página', last: 'Última página', next: 'Próxima página', previous: 'Página anterior' })[tipo]}
    />
  )
}
