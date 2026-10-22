import type { ListaTecnica, Material, OrdemProducao, PaginaDe } from '../types'

export function ordem(sobrescritas: Partial<OrdemProducao> = {}): OrdemProducao {
  return {
    id: 'o-1',
    codigo: 'OP-2026-0001',
    materialId: 'm-1',
    listaTecnicaId: 'l-1',
    tipoOrdemId: null,
    centroDeTrabalho: 'Usinagem CNC',
    quantidade: 10,
    quantidadeProduzida: null,
    inicioPlanejado: '2026-10-01',
    fimPlanejado: '2026-10-10',
    status: 'PLANEJADA',
    atrasada: false,
    criadaEm: '2026-10-01T10:00:00Z',
    atualizadaEm: '2026-10-01T10:00:00Z',
    criadaPor: 'teste@pcp.com',
    atualizadaPor: 'teste@pcp.com',
    ...sobrescritas,
  }
}

export function material(sobrescritas: Partial<Material> = {}): Material {
  return {
    id: 'm-1',
    codigo: '103000001',
    descricao: 'Viga metálica',
    tipo: 'PRODUTO_ACABADO',
    unidadeDeMedida: 'un',
    criadoEm: '2026-10-01T10:00:00Z',
    atualizadoEm: '2026-10-01T10:00:00Z',
    criadoPor: 'teste@pcp.com',
    atualizadoPor: 'teste@pcp.com',
    ...sobrescritas,
  }
}

export function listaTecnica(sobrescritas: Partial<ListaTecnica> = {}): ListaTecnica {
  return {
    id: 'l-1',
    materialId: 'm-1',
    versao: 'v1',
    status: 'EM_REVISAO',
    itens: [{ id: 'i-1', materialComponenteId: 'm-2', quantidadePlanejada: 2.5, unidadeDeMedida: 'kg' }],
    criadaEm: '2026-10-01T10:00:00Z',
    atualizadaEm: '2026-10-01T10:00:00Z',
    criadaPor: 'teste@pcp.com',
    atualizadaPor: 'teste@pcp.com',
    ...sobrescritas,
  }
}

export function pagina<T>(itens: T[], extras: Partial<PaginaDe<T>> = {}): PaginaDe<T> {
  return { itens, pagina: 0, tamanho: 25, total: itens.length, totalPaginas: 1, ...extras }
}
