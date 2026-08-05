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
  produto: string
  centroDeTrabalho: string
  quantidade: number
  inicioPlanejado: string  // "YYYY-MM-DD"
  fimPlanejado: string     // "YYYY-MM-DD"
  status: StatusOrdem
  atrasada: boolean        // calculado no servidor
  criadaEm: string         // ISO 8601
  atualizadaEm: string     // ISO 8601
}

export interface CriarOrdemRequest {
  codigo: string
  produto: string
  centroDeTrabalho: string
  quantidade: number
  inicioPlanejado: string  // "YYYY-MM-DD"
  fimPlanejado: string     // "YYYY-MM-DD"
}

// ---- Autenticação ----

export interface LoginRequest {
  email: string
  senha: string
}

export interface RegistrarRequest {
  nome: string
  email: string
  senha: string
  perfil: 'PLANEJADOR' | 'GERENTE'
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
