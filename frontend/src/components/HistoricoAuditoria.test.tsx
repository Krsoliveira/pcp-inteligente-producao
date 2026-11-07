import { screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { consultarEventosAuditoria } from '../api/auditoria'
import { pagina } from '../test/fabricas'
import { renderizarTela } from '../test/renderizar'
import { HistoricoAuditoria } from './HistoricoAuditoria'

vi.mock('../api/auditoria', () => ({ consultarEventosAuditoria: vi.fn() }))

describe('HistoricoAuditoria', () => {
  it('mostra o código do material com a mesma máscara do resto do sistema', async () => {
    vi.mocked(consultarEventosAuditoria).mockResolvedValue(pagina([{
      id: 'e-1', tipoEntidade: 'ORDEM_PRODUCAO', entidadeId: 'o-1', referencia: 'OP-1', acao: 'CRIADO',
      usuario: 'ana', ocorridoEm: '2026-10-01T10:00:00Z', detalhes: { material: '105000003' },
    }], { total: 1 }))

    renderizarTela(<HistoricoAuditoria tipoEntidade="ORDEM_PRODUCAO" entidadeId="o-1" />)

    expect(await screen.findByText('105.000.003')).toBeInTheDocument()
  })
})
