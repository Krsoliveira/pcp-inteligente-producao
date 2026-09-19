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
}

export interface CadastrarListaTecnicaRequest {
  materialId: string
  versao: string
  itens: { materialComponenteId: string; quantidadePlanejada: number; unidadeDeMedida: string }[]
}

// ---- Lote ----

export type StatusLote = 'DISPONIVEL' | 'BLOQUEADO' | 'CONSUMIDO' | 'VENCIDO'

export interface Lote {
  id: string
  numeroLote: string
  materialId: string
  ordemProducaoId: string | null
  quantidade: number
  unidadeDeMedida: string
  dataFabricacao: string
  dataValidade: string
  status: StatusLote
  criadoEm: string
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
}

export interface RegistrarConsumoRequest {
  quantidadeConsumida: number
  justificativa?: string
  justificadoPor?: string
}

// ---- Autenticação ----

export interface LoginRequest {
  email: string
  senha: string
}

export interface TokenResponse {
  token: string
}

// ---- Erros da API (Problem Details RFC 9457) ----

export interface ApiError {
  title: string
  status: number
  detail: string
}
