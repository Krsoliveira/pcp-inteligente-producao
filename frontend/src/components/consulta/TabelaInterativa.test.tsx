import { act, render, renderHook, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import Table from '@mui/material/Table'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import { describe, expect, it } from 'vitest'
import { CelulaDeCabecalho, useTabelaInterativa, type Coluna } from './TabelaInterativa'

interface Linha { codigo: string; quantidade: number | null; data: string }

const itens: Linha[] = [
  { codigo: 'OP-10', quantidade: 5, data: '2026-09-15' },
  { codigo: 'OP-2', quantidade: null, data: '2026-10-02' },
  { codigo: 'OP-1', quantidade: 30, data: '2026-09-01' },
]
const colunas: Coluna<Linha>[] = [
  { chave: 'codigo', rotulo: 'Código', valor: (l) => l.codigo },
  { chave: 'quantidade', rotulo: 'Quantidade', valor: (l) => l.quantidade },
  { chave: 'data', rotulo: 'Data', valor: (l) => l.data, texto: (l) => l.data.split('-').reverse().join('/') },
]
const codigos = (linhas: Linha[]) => linhas.map((l) => l.codigo)

function montar() {
  return renderHook(() => useTabelaInterativa(itens, colunas))
}

describe('useTabelaInterativa', () => {
  it('sem ordenação nem filtro mantém a ordem do servidor', () => {
    const { result } = montar()

    expect(codigos(result.current.linhas)).toEqual(['OP-10', 'OP-2', 'OP-1'])
    expect(result.current.controle.filtrando).toBe(false)
  })

  it('cada clique na coluna alterna crescente, decrescente e ordem original', () => {
    const { result } = montar()

    act(() => result.current.controle.alternarOrdem('codigo'))
    expect(codigos(result.current.linhas)).toEqual(['OP-1', 'OP-2', 'OP-10'])

    act(() => result.current.controle.alternarOrdem('codigo'))
    expect(codigos(result.current.linhas)).toEqual(['OP-10', 'OP-2', 'OP-1'])

    act(() => result.current.controle.alternarOrdem('codigo'))
    expect(result.current.controle.chaveOrdenada).toBeNull()
    expect(codigos(result.current.linhas)).toEqual(['OP-10', 'OP-2', 'OP-1'])
  })

  it('ao trocar de coluna recomeça no crescente', () => {
    const { result } = montar()

    act(() => result.current.controle.alternarOrdem('codigo'))
    act(() => result.current.controle.alternarOrdem('codigo'))
    act(() => result.current.controle.alternarOrdem('quantidade'))

    expect(result.current.controle.crescente).toBe(true)
    expect(codigos(result.current.linhas)).toEqual(['OP-10', 'OP-1', 'OP-2'])
  })

  it('deixa valores vazios no fim nas duas direções', () => {
    const { result } = montar()

    act(() => result.current.controle.alternarOrdem('quantidade'))
    act(() => result.current.controle.alternarOrdem('quantidade'))

    expect(codigos(result.current.linhas)).toEqual(['OP-1', 'OP-10', 'OP-2'])
  })

  it('filtra pelo texto exibido, sem diferenciar maiúsculas', () => {
    const { result } = montar()

    act(() => result.current.controle.setFiltro('data', '15/09'))
    expect(codigos(result.current.linhas)).toEqual(['OP-10'])

    act(() => { result.current.controle.setFiltro('data', ''); result.current.controle.setFiltro('codigo', 'op-2') })
    expect(codigos(result.current.linhas)).toEqual(['OP-2'])
  })

  it('combina filtros de várias colunas (todos precisam casar)', () => {
    const { result } = montar()

    act(() => { result.current.controle.setFiltro('codigo', 'OP-1'); result.current.controle.setFiltro('quantidade', '30') })

    expect(codigos(result.current.linhas)).toEqual(['OP-1'])
  })

  it('limparFiltros devolve todas as linhas', () => {
    const { result } = montar()
    act(() => result.current.controle.setFiltro('codigo', 'zzz'))
    expect(result.current.linhas).toEqual([])

    act(() => result.current.controle.limparFiltros())

    expect(result.current.linhas).toHaveLength(3)
    expect(result.current.controle.filtrando).toBe(false)
  })

  it('não altera a lista original', () => {
    const { result } = montar()

    act(() => result.current.controle.alternarOrdem('codigo'))

    expect(codigos(itens)).toEqual(['OP-10', 'OP-2', 'OP-1'])
  })
})

describe('CelulaDeCabecalho', () => {
  function Tabela() {
    const { controle } = useTabelaInterativa(itens, colunas)
    return (
      <Table><TableHead><TableRow>
        <CelulaDeCabecalho chave="codigo" rotulo="Código" controle={controle} />
        <CelulaDeCabecalho chave="quantidade" rotulo="Quantidade" controle={{ ...controle, colunas: colunas.map((c) => (c.chave === 'quantidade' ? { ...c, semOrdenacao: true } : c)) }} />
      </TableRow></TableHead></Table>
    )
  }

  it('o ícone de ordenar alterna a direção', async () => {
    render(<Tabela />)

    await userEvent.click(screen.getByRole('button', { name: 'Ordenar Código' }))
    expect(screen.getByRole('columnheader', { name: /Código/ })).toHaveAttribute('aria-sort', 'ascending')

    await userEvent.click(screen.getByRole('button', { name: 'Ordenar Código' }))
    expect(screen.getByRole('columnheader', { name: /Código/ })).toHaveAttribute('aria-sort', 'descending')
  })

  it('o funil abre o campo do filtro e o botão Limpar só vale com texto', async () => {
    render(<Tabela />)

    await userEvent.click(screen.getByRole('button', { name: 'Filtrar Código' }))
    expect(screen.getByRole('button', { name: 'Limpar' })).toBeDisabled()

    await userEvent.type(screen.getByRole('textbox', { name: 'Texto do filtro de Código' }), 'op')
    expect(screen.getByRole('button', { name: 'Limpar' })).toBeEnabled()

    await userEvent.click(screen.getByRole('button', { name: 'Limpar' }))
    expect(screen.getByRole('textbox', { name: 'Texto do filtro de Código' })).toHaveValue('')
  })

  it('coluna sem ordenação não mostra o ícone de ordenar', () => {
    render(<Tabela />)

    expect(screen.queryByRole('button', { name: 'Ordenar Quantidade' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Filtrar Quantidade' })).toBeInTheDocument()
  })
})
