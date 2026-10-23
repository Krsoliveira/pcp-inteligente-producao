import type { ListaTecnica, Lote, Material, NotaFiscal, OrdemProducao, PaginaDe, Permissao, PosicaoEstoque, UsuarioLogado } from '../types'

/** Todas as permissões do catálogo: o usuário de teste padrão (administrador) pode tudo. */
export const TODAS_AS_PERMISSOES: Permissao[] = [
  'DASHBOARD_VISUALIZAR',
  'MATERIAL_VISUALIZAR', 'MATERIAL_CADASTRAR',
  'LISTA_VISUALIZAR_ATIVAS', 'LISTA_VISUALIZAR_TODAS', 'LISTA_COMPARAR', 'LISTA_CADASTRAR', 'LISTA_ATIVAR', 'LISTA_RETIRAR_DE_USO',
  'ORDEM_VISUALIZAR', 'ORDEM_CRIAR', 'ORDEM_ATUALIZAR_STATUS', 'ORDEM_CONCLUIR',
  'CONSUMO_VISUALIZAR', 'CONSUMO_REGISTRAR',
  'TIPO_ORDEM_VISUALIZAR', 'TIPO_ORDEM_GERENCIAR',
  'NOTA_FISCAL_VISUALIZAR', 'NOTA_FISCAL_REGISTRAR',
  'ESTOQUE_VISUALIZAR',
  'AUDITORIA_VISUALIZAR',
  'ACESSO_VISUALIZAR', 'PERFIL_GERENCIAR', 'USUARIO_GERENCIAR',
]

export function usuarioLogado(sobrescritas: Partial<UsuarioLogado> = {}): UsuarioLogado {
  return {
    nome: 'Ana Teste',
    email: 'ana@pcp.com',
    perfil: { id: 'perfil-1', nome: 'Administrador' },
    permissoes: TODAS_AS_PERMISSOES,
    ...sobrescritas,
  }
}

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
    listaId: 'lista-1',
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

export function lote(sobrescritas: Partial<Lote> = {}): Lote {
  return {
    id: 'lt-1',
    numeroLote: 'LOTE-A1',
    materialId: 'm-2',
    ordemProducaoId: null,
    origem: 'COMPRA',
    notaFiscalId: 'nf-1',
    fornecedor: 'Aços Brasil',
    notaFiscal: 'NF-001',
    quantidade: 250,
    unidadeDeMedida: 'kg',
    dataFabricacao: '2026-08-01',
    dataValidade: '2031-08-01',
    status: 'DISPONIVEL',
    saldo: 250,
    dataEmissaoNf: '2026-09-01',
    dataRecebimento: '2026-09-02',
    criadoEm: '2026-09-02T10:00:00Z',
    criadoPor: 'teste@pcp.com',
    atualizadoEm: '2026-09-02T10:00:00Z',
    atualizadoPor: 'teste@pcp.com',
    ...sobrescritas,
  }
}

export function notaFiscal(sobrescritas: Partial<NotaFiscal> = {}): NotaFiscal {
  return {
    id: 'nf-1',
    fornecedor: 'Aços Brasil',
    numero: 'NF-001',
    dataEmissao: '2026-09-01',
    dataRecebimento: '2026-09-02',
    registradaPor: 'teste@pcp.com',
    registradaEm: '2026-09-02T10:00:00Z',
    quantidadeItens: 1,
    itens: [lote()],
    ...sobrescritas,
  }
}

export function posicaoEstoque(sobrescritas: Partial<PosicaoEstoque> = {}): PosicaoEstoque {
  return {
    materialId: 'm-2',
    codigo: '110000001',
    descricao: 'Aço inox',
    tipo: 'MATERIA_PRIMA',
    unidadeDeMedida: 'kg',
    saldoDisponivel: 750,
    lotesDisponiveis: 3,
    saldoIndisponivel: 0,
    proximoVencimento: null,
    ultimaEntrada: '2026-10-05T10:00:00Z',
    ...sobrescritas,
  }
}
