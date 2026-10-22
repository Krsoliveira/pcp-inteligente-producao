import TablePagination from '@mui/material/TablePagination'

interface PaginacaoTabelaProps {
  total: number
  /** Página atual, começando em 0. */
  pagina: number
  tamanho: number
  onPagina: (pagina: number) => void
  onTamanho: (tamanho: number) => void
}

/** Rodapé de paginação das tabelas, com os textos em português. */
export function PaginacaoTabela({ total, pagina, tamanho, onPagina, onTamanho }: PaginacaoTabelaProps) {
  return (
    <TablePagination
      component="div"
      count={total}
      page={pagina}
      rowsPerPage={tamanho}
      rowsPerPageOptions={[10, 25, 50, 100]}
      onPageChange={(_, novaPagina) => onPagina(novaPagina)}
      onRowsPerPageChange={(e) => onTamanho(Number(e.target.value))}
      labelRowsPerPage="Linhas por página"
      labelDisplayedRows={({ from, to, count }) => `${from}–${to} de ${count}`}
      getItemAriaLabel={(tipo) => ({ first: 'Primeira página', last: 'Última página', next: 'Próxima página', previous: 'Página anterior' })[tipo]}
    />
  )
}
