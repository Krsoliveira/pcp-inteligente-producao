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
  /** Tipo de sistema Conversão: o único que pode consumir produto acabado (e só produz semiacabado). */
  conversao: boolean
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

/** O código (9 dígitos, faixa pelo tipo) é gerado pelo backend — ADR-0012. */
export interface CadastrarMaterialRequest {
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
  /** Id da versão. */
  id: string
  /** Id da lista do material: todas as versões do mesmo material compartilham este valor. */
  listaId: string
  materialId: string
  versao: string
  status: StatusListaTecnica
  itens: ItemListaTecnica[]
  criadaEm: string
  atualizadaEm: string
  criadaPor: string
  atualizadaPor: string
}

/** Componente nas duas versões comparadas; o lado em que ele não existe vem nulo. */
export interface ItemComparado {
  materialComponenteId: string
  quantidadeA: number | null
  unidadeA: string | null
  quantidadeB: number | null
  unidadeB: string | null
}

export interface VersaoResumo {
  id: string
  listaId: string
  materialId: string
  versao: string
  status: StatusListaTecnica
}

/** Diferença entre duas versões da mesma lista técnica (GET /listas-tecnicas/comparar). */
export interface ComparacaoDeVersoes {
  versaoA: VersaoResumo
  versaoB: VersaoResumo
  temDiferencas: boolean
  adicionados: ItemComparado[]
  removidos: ItemComparado[]
  alterados: ItemComparado[]
  iguais: ItemComparado[]
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
  /** Preenchidos apenas em lotes de compra (item de uma nota fiscal de entrada). */
  notaFiscalId: string | null
  fornecedor: string | null
  notaFiscal: string | null
  quantidade: number
  unidadeDeMedida: string
  dataFabricacao: string
  dataValidade: string
  status: StatusLote
  /** Quantidade ainda disponível (quantidade − alocações a consumos). */
  saldo: number
  /** Datas da nota fiscal — apenas lotes de compra. */
  dataEmissaoNf: string | null
  dataRecebimento: string | null
  criadoEm: string
  criadoPor: string
  atualizadoEm: string
  atualizadoPor: string
}

// ---- Suprimentos (ADR-0012) ----

/** Nota fiscal de entrada; os itens são os lotes de compra que ela trouxe. */
export interface NotaFiscal {
  id: string
  fornecedor: string
  numero: string
  dataEmissao: string
  dataRecebimento: string
  registradaPor: string
  registradaEm: string
  quantidadeItens: number
  itens: Lote[]
}

export interface ItemNotaFiscalRequest {
  materialId: string
  quantidade: number
  /** Lote do fornecedor — até 20 caracteres (letras, dígitos e . / -). */
  numeroLote: string
  dataFabricacao: string
  dataValidade: string
}

export interface RegistrarNotaFiscalRequest {
  fornecedor: string
  numero: string
  dataEmissao: string
  dataRecebimento: string
  itens: ItemNotaFiscalRequest[]
}

/** Posição de estoque de um material, calculada a partir dos saldos dos lotes. */
export interface PosicaoEstoque {
  materialId: string
  codigo: string
  descricao: string
  tipo: TipoMaterial
  unidadeDeMedida: string
  /** Lotes DISPONIVEIS e dentro da validade. */
  saldoDisponivel: number
  lotesDisponiveis: number
  /** Saldo parado em lotes bloqueados ou vencidos. */
  saldoIndisponivel: number
  proximoVencimento: string | null
  ultimaEntrada: string | null
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
  /** Item adicionado ao lançar o consumo, fora da lista técnica (planejado 0). */
  naoPlanejado: boolean
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
  /** De quais lotes saiu o material: a soma deve ser igual à quantidade consumida. */
  alocacoes: AlocacaoLoteRequest[]
}

export interface AlocacaoLoteRequest {
  loteId: string
  quantidade: number
}

// ---- Genealogia de lotes ----

export interface LoteOrigem {
  loteId: string
  numeroLote: string
  origem: OrigemLote
  fornecedor: string | null
  notaFiscal: string | null
  ordemProducaoId: string | null
  quantidade: number
  alocadoPor: string
  alocadoEm: string
}

export interface OrigemConsumo {
  consumoId: string
  materialId: string
  quantidadePlanejada: number
  quantidadeConsumida: number | null
  unidadeDeMedida: string
  lotes: LoteOrigem[]
}

export interface DestinoLote {
  alocacaoId: string
  quantidade: number
  unidadeDeMedida: string
  alocadoPor: string
  alocadoEm: string
  ordemProducaoId: string
  ordemCodigo: string
  ordemStatus: StatusOrdem
  materialProduzidoId: string
  loteGeradoId: string | null
  loteGeradoNumero: string | null
}

export interface RastreabilidadeLote {
  loteId: string
  numeroLote: string
  origens: OrigemConsumo[]
  destinos: DestinoLote[]
}

// ---- Autenticação ----

export interface LoginRequest {
  email: string
  senha: string
}

export interface TokenResponse {
  token: string
}

/** Autocadastro: o backend sempre cria o usuário com o perfil padrão (só consulta). */
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
  | 'NOTA_FISCAL'
  | 'USUARIO'
  | 'PERFIL_ACESSO'

export type AcaoAuditoria =
  | 'CRIADO'
  | 'ALTERADO'
  | 'STATUS_ALTERADO'
  | 'ATIVADA'
  | 'OBSOLETADA'
  | 'CONSUMO_REGISTRADO'
  | 'CONSUMO_ADICIONADO'
  | 'ORDEM_CONCLUIDA'
  | 'LOTE_GERADO'
  | 'LOTE_ALOCADO'
  | 'ENTRADA_REGISTRADA'
  | 'USUARIO_REGISTRADO'
  | 'EXCLUIDO'
  | 'PERFIL_ATRIBUIDO'
  | 'SENHA_ALTERADA'
  | 'SENHA_REDEFINIDA'

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

/** Página devolvida pelas pesquisas do servidor. `pagina` começa em 0. */
export interface PaginaDe<T> {
  itens: T[]
  pagina: number
  tamanho: number
  total: number
  totalPaginas: number
}

export interface FiltroOrdens {
  /** Trecho do código da ordem. */
  busca?: string
  status?: StatusOrdem
  /** Início planejado a partir desta data (AAAA-MM-DD, inclusive). */
  de?: string
  /** Início planejado até esta data (AAAA-MM-DD, inclusive). */
  ate?: string
  /** `true`: só as ordens abertas depois do fim planejado (as mesmas que o dashboard conta). */
  atrasadas?: boolean
  /** Nome exato do centro de trabalho. */
  centro?: string
  /** Fim planejado a partir desta data (AAAA-MM-DD, inclusive). */
  fimDe?: string
  /** Fim planejado até esta data (AAAA-MM-DD, inclusive). */
  fimAte?: string
  pagina?: number
  tamanho?: number
}

/** Consumo total de um componente numa ordem concluída (soma dos lotes); `desvio` = consumido − planejado. */
export interface LinhaDeConsumoDeOrdem {
  ordemId: string
  ordemCodigo: string
  fimPlanejado: string
  produtoCodigo: string
  produtoDescricao: string
  componenteCodigo: string
  componenteDescricao: string
  unidadeDeMedida: string
  quantidadePlanejada: number
  quantidadeConsumida: number | null
  desvio: number | null
}

/** Relatório de consumo de ordens concluídas com fim planejado entre `de` e `ate`. */
export interface RelatorioConsumoDeOrdens {
  de: string
  ate: string
  totalDeOrdens: number
  linhas: LinhaDeConsumoDeOrdem[]
}

/** Totais do dashboard, calculados no servidor. */
export interface ResumoProducao {
  totalOrdens: number
  ordensPorStatus: Record<StatusOrdem, number>
  ordensAtrasadas: number
  lotesDisponiveis: number
}

/** Como terminaram as ordens cujo fim planejado cai no mês (`mes` no formato AAAA-MM); canceladas ficam de fora. */
export interface EntregasDoMes {
  mes: string
  noPrazo: number
  comAtraso: number
  abertasAtrasadas: number
  abertasNoPrazo: number
}

/** Ordens atrasadas de um centro de trabalho e há quantos dias a mais antiga passou do fim planejado. */
export interface AtrasoDoCentro {
  centroDeTrabalho: string
  ordensAtrasadas: number
  diasDeAtrasoDaMaisAntiga: number
}

export interface FiltroMateriais {
  /** Trecho da descrição ou do código (aceita 103.000.001). */
  busca?: string
  tipo?: TipoMaterial
  pagina?: number
  tamanho?: number
}

export interface FiltroListasTecnicas {
  materialId?: string
  status?: StatusListaTecnica
  pagina?: number
  tamanho?: number
}

export interface FiltroNotasFiscais {
  /** Trecho do número da nota, do fornecedor ou do número de um lote que ela trouxe. */
  busca?: string
  /** Recebida a partir desta data ("YYYY-MM-DD", inclusive). */
  de?: string
  ate?: string
  pagina?: number
  tamanho?: number
}

export interface FiltroEstoque {
  /** Trecho do código (aceita 103.000.001) ou da descrição. */
  busca?: string
  tipo?: TipoMaterial
  somenteComSaldo?: boolean
  pagina?: number
  tamanho?: number
}

/** Alertas do estoque, calculados sobre todos os materiais. */
export interface ResumoEstoque {
  totalMateriais: number
  materiaisVencendo: number
  materiasPrimasSemSaldo: number
}

// ---- Controle de acesso (ADR-0020) ----

/** Capacidades que um perfil pode ter; os nomes são os do catálogo do servidor. */
export type Permissao =
  | 'DASHBOARD_VISUALIZAR'
  | 'MATERIAL_VISUALIZAR' | 'MATERIAL_CADASTRAR'
  | 'LISTA_VISUALIZAR_ATIVAS' | 'LISTA_VISUALIZAR_TODAS' | 'LISTA_COMPARAR' | 'LISTA_CADASTRAR'
  | 'LISTA_ATIVAR' | 'LISTA_RETIRAR_DE_USO'
  | 'ORDEM_VISUALIZAR' | 'ORDEM_CRIAR' | 'ORDEM_ATUALIZAR_STATUS' | 'ORDEM_CONCLUIR'
  | 'CONSUMO_VISUALIZAR' | 'CONSUMO_REGISTRAR' | 'CONSUMO_ADICIONAR_ITEM'
  | 'TIPO_ORDEM_VISUALIZAR' | 'TIPO_ORDEM_GERENCIAR'
  | 'NOTA_FISCAL_VISUALIZAR' | 'NOTA_FISCAL_REGISTRAR'
  | 'ESTOQUE_VISUALIZAR'
  | 'AUDITORIA_VISUALIZAR'
  | 'RELATORIO_CONSUMO_VISUALIZAR'
  | 'ACESSO_VISUALIZAR' | 'PERFIL_GERENCIAR' | 'USUARIO_GERENCIAR'

/** Quem está logado e o que pode fazer (GET /acessos/eu). */
export interface UsuarioLogado {
  nome: string
  email: string
  perfil: { id: string; nome: string }
  permissoes: Permissao[]
  /** A senha é provisória: até trocá-la o servidor só aceita consultar quem sou eu e a troca (ADR-0021). */
  deveTrocarSenha: boolean
}

/** Uma "caixinha" do catálogo, com a área em que aparece. */
export interface PermissaoDoCatalogo {
  permissao: Permissao
  area: string
  descricao: string
}

export interface PerfilAcesso {
  id: string
  nome: string
  descricao: string
  /** Perfil de sistema com todas as permissões; não se altera nem se exclui. */
  administrador: boolean
  /** Perfil que o autocadastro recebe; não se exclui. */
  padraoDoAutocadastro: boolean
  permissoes: Permissao[]
  quantidadeDeUsuarios: number
  atualizadoEm: string
  atualizadoPor: string
}

export interface SalvarPerfilRequest {
  nome: string
  descricao: string
  permissoes: Permissao[]
}

export interface UsuarioAcesso {
  id: string
  nome: string
  email: string
  perfilId: string
  perfilNome: string | null
  criadoEm: string
}

export interface CriarUsuarioRequest {
  nome: string
  email: string
  senhaProvisoria: string
  perfilId: string
}
