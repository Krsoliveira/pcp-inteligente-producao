import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { lote } from '../../test/fabricas'
import { renderizarTela } from '../../test/renderizar'
import { TabelaLotes } from './TabelaLotes'

describe('TabelaLotes', () => {
  it('cada lote abre por um link de verdade, sem depender do mouse', () => {
    renderizarTela(<TabelaLotes lotes={[lote({ id: 'l-1', numeroLote: 'ACO-1' })]} coluna="origem" />)

    expect(screen.getByRole('link', { name: 'ACO-1' })).toHaveAttribute('href', '/lotes/l-1')
  })
})
