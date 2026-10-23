import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { consultarEventosAuditoria } from '../api/auditoria'
import { listarConsumosPorOrdem } from '../api/consumos'
import { buscarListaTecnicaPorId } from '../api/listasTecnicas'
import { listarLotesPorOrdem } from '../api/lotes'
import { buscarMaterialPorId, listarMateriais } from '../api/materiais'
import { buscarOrdemPorId } from '../api/ordens'
import { listaTecnica, material, ordem, pagina, TODAS_AS_PERMISSOES } from '../test/fabricas'
import { renderizarTela } from '../test/renderizar'
import type { ConsumoMaterial, Permissao } from '../types'
import { OrdemDetalhePage } from './OrdemDetalhePage'

vi.mock('../api/ordens', () => ({ buscarOrdemPorId: vi.fn(), concluirOrdem: vi.fn(), atualizarStatus: vi.fn() }))
vi.mock('../api/consumos', () => ({ listarConsumosPorOrdem: vi.fn(), registrarConsumo: vi.fn() }))
vi.mock('../api/lotes', () => ({ listarLotesPorOrdem: vi.fn() }))
vi.mock('../api/listasTecnicas', () => ({ buscarListaTecnicaPorId: vi.fn() }))
vi.mock('../api/materiais', () => ({ buscarMaterialPorId: vi.fn(), listarMateriais: vi.fn() }))
vi.mock('../api/auditoria', () => ({ consultarEventosAuditoria: vi.fn() }))

const consumosMock = vi.mocked(listarConsumosPorOrdem)

function consumo(sobrescritas: Partial<ConsumoMaterial> = {}): ConsumoMaterial {
  return {
    id: 'c-1', ordemProducaoId: 'o-1', materialId: 'm-2', quantidadePlanejada: 10, quantidadeConsumida: 10, desvio: 0,
    unidadeDeMedida: 'kg', registrado: true, justificado: true, naoPlanejado: false, justificativa: null, justificadoPor: null, justificadoEm: null,
    criadoEm: '2026-10-01T10:00:00Z', criadoPor: 'teste', atualizadoEm: '2026-10-01T10:00:00Z', atualizadoPor: 'teste',
    ...sobrescritas,
  }
}

function montar(permissoes: Permissao[] = TODAS_AS_PERMISSOES) {
  return renderizarTela(<OrdemDetalhePage />, { rotas: ['/ordens/o-1'], padrao: '/ordens/:id', permissoes })
}

describe('OrdemDetalhePage: o que cada perfil pode fazer', () => {
  beforeEach(() => {
    vi.mocked(buscarOrdemPorId).mockReset().mockResolvedValue(ordem({ id: 'o-1', codigo: 'OP-1', status: 'EM_PRODUCAO' }))
    consumosMock.mockReset().mockResolvedValue([consumo()])
    vi.mocked(listarLotesPorOrdem).mockReset().mockResolvedValue([])
    vi.mocked(buscarListaTecnicaPorId).mockReset().mockResolvedValue(listaTecnica())
    vi.mocked(buscarMaterialPorId).mockReset().mockResolvedValue(material())
    vi.mocked(listarMateriais).mockReset().mockResolvedValue([material(), material({ id: 'm-2', codigo: '110000001', descricao: 'Aço' })])
    vi.mocked(consultarEventosAuditoria).mockReset().mockResolvedValue(pagina([], { total: 0 }))
  })

  it('com todas as permissões mostra alterar status, concluir, o histórico e os consumos', async () => {
    consumosMock.mockResolvedValue([consumo({ registrado: false })])
    montar()

    expect(await screen.findByRole('button', { name: 'Alterar Status' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Registrar consumo' })).toBeInTheDocument()
    expect(await screen.findByText('Histórico da ordem')).toBeInTheDocument()
  })

  it('só oferece concluir quando todos os consumos estão registrados, e com ORDEM_CONCLUIR', async () => {
    montar()

    expect(await screen.findByRole('button', { name: 'Concluir Ordem' })).toBeInTheDocument()
  })

  it('sem ORDEM_ATUALIZAR_STATUS não mostra "Alterar Status"', async () => {
    montar(TODAS_AS_PERMISSOES.filter((p) => p !== 'ORDEM_ATUALIZAR_STATUS'))
    await screen.findByRole('button', { name: 'Concluir Ordem' })

    expect(screen.queryByRole('button', { name: 'Alterar Status' })).not.toBeInTheDocument()
  })

  it('sem ORDEM_CONCLUIR não mostra "Concluir Ordem", mesmo com tudo registrado', async () => {
    montar(TODAS_AS_PERMISSOES.filter((p) => p !== 'ORDEM_CONCLUIR'))
    await screen.findByRole('button', { name: 'Alterar Status' })

    expect(screen.queryByRole('button', { name: 'Concluir Ordem' })).not.toBeInTheDocument()
  })

  it('sem CONSUMO_REGISTRAR não mostra o botão de registrar consumo', async () => {
    consumosMock.mockResolvedValue([consumo({ registrado: false })])
    montar(TODAS_AS_PERMISSOES.filter((p) => p !== 'CONSUMO_REGISTRAR'))
    await screen.findByRole('button', { name: 'Alterar Status' })
    await screen.findAllByText('110.000.001')

    expect(screen.queryByRole('button', { name: 'Registrar consumo' })).not.toBeInTheDocument()
  })

  it('sem CONSUMO_VISUALIZAR avisa em vez de mostrar a lista, e nem consulta os consumos', async () => {
    montar(TODAS_AS_PERMISSOES.filter((p) => p !== 'CONSUMO_VISUALIZAR'))

    expect(await screen.findByText(/não inclui a consulta de consumos/i)).toBeInTheDocument()
    expect(consumosMock).not.toHaveBeenCalled()
  })

  it('sem AUDITORIA_VISUALIZAR esconde o histórico e nem o consulta', async () => {
    montar(TODAS_AS_PERMISSOES.filter((p) => p !== 'AUDITORIA_VISUALIZAR'))
    await screen.findByRole('button', { name: 'Alterar Status' })

    expect(screen.queryByText('Histórico da ordem')).not.toBeInTheDocument()
    expect(consultarEventosAuditoria).not.toHaveBeenCalled()
  })

  it('sem ESTOQUE_VISUALIZAR não pede os lotes da ordem; com a permissão pede', async () => {
    const { unmount } = montar()
    await screen.findByRole('button', { name: 'Alterar Status' })
    expect(listarLotesPorOrdem).toHaveBeenCalled()
    unmount()
    vi.mocked(listarLotesPorOrdem).mockClear()

    montar(TODAS_AS_PERMISSOES.filter((p) => p !== 'ESTOQUE_VISUALIZAR'))
    await screen.findByRole('button', { name: 'Alterar Status' })

    expect(listarLotesPorOrdem).not.toHaveBeenCalled()
  })

  it('sem MATERIAL_VISUALIZAR não pede o catálogo de materiais', async () => {
    montar(TODAS_AS_PERMISSOES.filter((p) => p !== 'MATERIAL_VISUALIZAR'))
    await screen.findByRole('button', { name: 'Alterar Status' })

    expect(listarMateriais).not.toHaveBeenCalled()
    expect(buscarMaterialPorId).not.toHaveBeenCalled()
  })

  it('um perfil só de consulta não vê nenhuma ação', async () => {
    montar(['ORDEM_VISUALIZAR', 'CONSUMO_VISUALIZAR', 'MATERIAL_VISUALIZAR'])

    expect((await screen.findAllByText('110.000.001')).length).toBeGreaterThan(0)
    expect(screen.queryByRole('button', { name: 'Alterar Status' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Concluir Ordem' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Registrar consumo' })).not.toBeInTheDocument()
  })
})

describe('OrdemDetalhePage: item não planejado (ADR-0025)', () => {
  beforeEach(() => {
    vi.mocked(buscarOrdemPorId).mockReset().mockResolvedValue(ordem({ id: 'o-1', codigo: 'OP-1', status: 'EM_PRODUCAO' }))
    vi.mocked(listarLotesPorOrdem).mockReset().mockResolvedValue([])
  })

  it('a linha adicionada fora da lista aparece com o selo "Não planejado"; as da lista não', async () => {
    consumosMock.mockReset().mockResolvedValue([
      consumo({ id: 'c-1', materialId: 'm-2' }),
      consumo({ id: 'c-2', materialId: 'm-3', quantidadePlanejada: 0, quantidadeConsumida: 4, desvio: 4, naoPlanejado: true, justificativa: 'Faltou na lista' }),
    ])
    montar()

    expect(await screen.findByText('Não planejado')).toBeInTheDocument()
    expect(screen.getAllByText('Não planejado')).toHaveLength(1)
  })

  it('sem item adicionado, nenhum selo', async () => {
    consumosMock.mockReset().mockResolvedValue([consumo()])
    montar()

    await screen.findByText(/Consumo de Materiais/)

    expect(screen.queryByText('Não planejado')).not.toBeInTheDocument()
  })
})

