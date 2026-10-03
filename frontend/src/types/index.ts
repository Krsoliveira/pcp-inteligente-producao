// ---- Ordens de Produção ----

export type StatusOrdem =
  | 'PLANEJADA'
  | 'LIBERADA'
  | 'EM_PRODUCAO'
  | 'CONCLUIDA'
  | 'CANCELADA'

export interface OrdemProducao {
  id: string
  codigo: string
  materialId: string
  listaTecnicaId: string
  tipoOrdemId: string | null
  centroDeTrabalho: string
  quantidade: number
  quantidadeProduzida: number | null
  inicioPlanejado: string  // "YYYY-MM-DD"
  fimPlanejado: string     // "YYYY-MM-DD"
  status: StatusOrdem
  atrasada: boolean
  criadaEm: string
  atualizadaEm: string
  criadaPor: string
  atualizadaPor: string
}

export interface CriarOrdemRequest {
  codigo: string
  materialId: string
  listaTecnicaId: string
  tipoOrdemId?: string | null
  centroDeTrabalho: string
  quantidade: number
  inicioPlanejado: string
  fimPlanejado: string
}

export interface ConcluirOrdemRequest {
  quantidadeProduzida: number
  dataFabricacao: string
  dataValidade: string
}

// ---- Tipo de Ordem ----

export interface TipoOrdem {
  id: string
  nome: string
  descricao: string | null
  cor: string
  criadoEm: string
  atualizadoEm: string
  criadoPor: string
  atualizadoPor: string
}

export interface CadastrarTipoOrdemRequest {
  nome: string
  descricao?: string
  cor: string
}

// ---- Material ----

export type TipoMaterial = 'PRODUTO_ACABADO' | 'SEMIACABADO' | 'MATERIA_PRIMA'

export const TIPO_MATERIAL_LABEL: Record<TipoMaterial, string> = {
  PRODUTO_ACABADO: 'Produto Acabado',
  SEMIACABADO: 'Semiacabado',
  MATERIA_PRIMA: 'Matéria-Prima',
}

export interface Material {
  id: string
  codigo: string
  descricao: string
  tipo: TipoMaterial
  unidadeDeMedida: string
  criadoEm: string
  atualizadoEm: string
  criadoPor: string
  atualizadoPor: string
}

export interface CadastrarMaterialRequest {
  codigo: string
  descricao: string
  tipo: TipoMaterial
  unidadeDeMedida: string
}

// ---- Lista Técnica ----

export type StatusListaTecnica = 'EM_REVISAO' | 'ATIVA' | 'OBSOLETA'

export interface ItemListaTecnica {
  id: string
  materialComponenteId: string
  quantidadePlanejada: number
  unidadeDeMedida: string
}

export interface ListaTecnica {
  id: string
  materialId: string
  versao: string
  status: StatusListaTecnica
  itens: ItemListaTecnica[]
  criadaEm: string
  atualizadaEm: string
  criadaPor: string
  atualizadaPor: string
}

export interface CadastrarListaTecnicaRequest {
  materialId: string
  versao: string
  itens: { materialComponenteId: string; quantidadePlanejada: number; unidadeDeMedida: string }[]
}

// ---- Lote ----

export type StatusLote = 'DISPONIVEL' | 'BLOQUEADO' | 'CONSUMIDO' | 'VENCIDO'

export type OrigemLote = 'PRODUCAO' | 'COMPRA'

export interface Lote {
  id: string
  numeroLote: string
  materialId: string
  ordemProducaoId: string | null
  origem: OrigemLote
  /** Preenchidos apenas em lotes de compra (entrada de material). */
  fornecedor: string | null
  notaFiscal: string | null
  quantidade: number
  unidadeDeMedida: string
  dataFabricacao: string
  dataValidade: string
  status: StatusLote
  /** Datas da nota fiscal — apenas lotes de compra. */
  dataEmissaoNf: string | null
  dataRecebimento: string | null
  criadoEm: string
  criadoPor: string
  atualizadoEm: string
  atualizadoPor: string
}

export interface RegistrarEntradaMaterialRequest {
  materialId: string
  fornecedor: string
  notaFiscal: string
  dataEmissaoNf: string
  dataRecebimento: string
  quantidade: number
  dataFabricacao: string
  dataValidade: string
}

// ---- Consumo de Material ----

export interface ConsumoMaterial {
  id: string
  ordemProducaoId: string
  materialId: string
  quantidadePlanejada: number
  quantidadeConsumida: number | null
  desvio: number | null
  unidadeDeMedida: string
  registrado: boolean
  justificado: boolean
  justificativa: string | null
  justificadoPor: string | null
  justificadoEm: string | null
  criadoEm: string
  criadoPor: string
  atualizadoEm: string
  atualizadoPor: string
}

export interface RegistrarConsumoRequest {
  quantidadeConsumida: number
  /** O responsável é o usuário logado — definido pelo backend. */
  justificativa?: string
}

// ---- Autenticação ----

export interface LoginRequest {
  email: string
  senha: string
}

export interface TokenResponse {
  token: string
}

/** Autocadastro: o backend sempre cria o usuário com perfil PLANEJADOR. */
export interface RegistrarRequest {
  nome: string
  email: string
  senha: string
}

// ---- Erros da API (Problem Details RFC 9457) ----

export interface ApiError {
  title: string
  status: number
  detail: string
  /** Erros de validação por campo (400). */
  erros?: Record<string, string>
}

// ---- Auditoria (ADR-0011) ----

export type TipoEntidade =
  | 'MATERIAL'
  | 'LISTA_TECNICA'
  | 'ORDEM_PRODUCAO'
  | 'LOTE'
  | 'TIPO_ORDEM'
  | 'USUARIO'

export type AcaoAuditoria =
  | 'CRIADO'
  | 'ALTERADO'
  | 'STATUS_ALTERADO'
  | 'ATIVADA'
  | 'OBSOLETADA'
  | 'CONSUMO_REGISTRADO'
  | 'ORDEM_CONCLUIDA'
  | 'LOTE_GERADO'
  | 'ENTRADA_REGISTRADA'
  | 'USUARIO_REGISTRADO'

/** Mudança de valor registrada na trilha: {"de": ..., "para": ...}. */
export interface MudancaAuditoria {
  de: string | number | null
  para: string | number | null
}

export interface EventoAuditoria {
  id: string
  tipoEntidade: TipoEntidade
  entidadeId: string
  referencia: string
  acao: AcaoAuditoria
  usuario: string
  ocorridoEm: string
  detalhes: Record<string, string | number | boolean | MudancaAuditoria>
}

export interface Pagina<T> {
  itens: T[]
  pagina: number
  tamanho: number
  total: number
  totalPaginas: number
}

export interface FiltroEventosAuditoria {
  tipoEntidade?: TipoEntidade
  entidadeId?: string
  usuario?: string
  acao?: AcaoAuditoria
  /** "YYYY-MM-DD", inclusivo. */
  de?: string
  /** "YYYY-MM-DD", inclusivo. */
  ate?: string
  pagina?: number
  tamanho?: number
}
