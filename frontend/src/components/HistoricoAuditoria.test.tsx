import { screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { consultarEventosAuditoria } from '../api/auditoria'
import { pagina } from '../test/fabricas'
import { renderizarTela } from '../test/renderizar'
import type { AcaoAuditoria, EventoAuditoria, TipoEntidade } from '../types'
import { HistoricoAuditoria } from './HistoricoAuditoria'

vi.mock('../api/auditoria', () => ({ consultarEventosAuditoria: vi.fn() }))

function evento(acao: AcaoAuditoria, detalhes: EventoAuditoria['detalhes'], tipoEntidade: TipoEntidade = 'ORDEM_PRODUCAO'): EventoAuditoria {
  return {
    id: `e-${acao}`, tipoEntidade, entidadeId: 'o-1', referencia: 'OP-1', acao,
    usuario: 'ana', ocorridoEm: '2026-10-01T10:00:00Z', detalhes,
  }
}

async function montarCom(...eventos: EventoAuditoria[]) {
  vi.mocked(consultarEventosAuditoria).mockResolvedValue(pagina(eventos, { total: eventos.length }))
  renderizarTela(<HistoricoAuditoria tipoEntidade="ORDEM_PRODUCAO" entidadeId="o-1" />)
  return screen.findAllByRole('listitem')
}

describe('HistoricoAuditoria', () => {
  it('mostra o código do material com a mesma máscara do resto do sistema', async () => {
    await montarCom(evento('CRIADO', { material: '105000003' }))

    expect(await screen.findByText('105.000.003')).toBeInTheDocument()
  })

  it('campo sem rótulo cadastrado aparece em palavras, não no nome técnico', async () => {
    await montarCom(evento('CRIADO', { consumosProjetados: 3 }))

    expect(await screen.findByText('Consumos projetados')).toBeInTheDocument()
    expect(screen.queryByText('consumosProjetados')).not.toBeInTheDocument()
  })

  it('mudança de status mostra os dois status como selos, sem texto riscado', async () => {
    const [item] = await montarCom(evento('STATUS_ALTERADO', { status: { de: 'EM_PRODUCAO', para: 'CANCELADA' } }))

    expect(within(item).getByText('Em produção')).toBeInTheDocument()
    expect(within(item).getByText('Cancelada')).toBeInTheDocument()
    expect(item.querySelector('s')).toBeNull()
  })

  it('a unidade de medida vai junto das quantidades, sem uma linha própria', async () => {
    const [item] = await montarCom(evento('CONSUMO_REGISTRADO', {
      unidadeDeMedida: 'kg', quantidadePlanejada: '80', quantidadeConsumida: '80', desvio: '0',
    }))

    expect(within(item).getAllByText('80 kg')).toHaveLength(2)
    expect(within(item).queryByText('Unidade')).not.toBeInTheDocument()
  })

  it('sem quantidade no evento, a unidade continua aparecendo', async () => {
    const [item] = await montarCom(evento('CRIADO', { descricao: 'Aço', unidadeDeMedida: 'kg' }, 'MATERIAL'))

    expect(within(item).getByText('Unidade')).toBeInTheDocument()
  })

  it('os lotes do consumo viram uma etiqueta por lote, com a quantidade e a unidade', async () => {
    const [item] = await montarCom(evento('CONSUMO_REGISTRADO', { unidadeDeMedida: 'kg', quantidadeConsumida: '100', lotes: '14: 80; 7: 20' }))

    expect(within(item).getByText('Lote 14 · 80 kg')).toBeInTheDocument()
    expect(within(item).getByText('Lote 7 · 20 kg')).toBeInTheDocument()
  })

  it.each([
    ['ORDEM_CONCLUIDA', { status: { de: 'EM_PRODUCAO', para: 'CONCLUIDA' } }, 'sucesso'],
    ['STATUS_ALTERADO', { status: { de: 'EM_PRODUCAO', para: 'CANCELADA' } }, 'erro'],
    ['STATUS_ALTERADO', { status: { de: 'PLANEJADA', para: 'LIBERADA' } }, 'informacao'],
    ['CONSUMO_REGISTRADO', { quantidadeConsumida: '80', desvio: '0' }, 'informacao'],
    ['CONSUMO_REGISTRADO', { quantidadeConsumida: '85', desvio: '5' }, 'atencao'],
    ['EXCLUIDO', { codigo: 'X' }, 'erro'],
  ] as const)('o evento %s %j recebe o tom %s', async (acao, detalhes, tom) => {
    const [item] = await montarCom(evento(acao, detalhes))

    expect(item).toHaveAttribute('data-tom', tom)
  })
})
