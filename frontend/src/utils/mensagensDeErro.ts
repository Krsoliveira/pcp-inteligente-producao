import axios from 'axios'

const SEM_CONEXAO =
  'Não foi possível conectar ao sistema. Verifique sua conexão com a internet e tente novamente em instantes.'

/** Segundos que a pessoa precisa aguardar quando o sistema limitou as tentativas (HTTP 429); null nos demais erros. */
export function segundosDeEspera(error: unknown): number | null {
  if (!axios.isAxiosError(error) || error.response?.status !== 429) return null
  const segundos = Number(error.response.headers?.['retry-after'])
  return Number.isFinite(segundos) && segundos > 0 ? Math.ceil(segundos) : 60
}

export function mensagemDeEspera(segundos: number): string {
  const unidade = segundos === 1 ? 'segundo' : 'segundos'
  return `Muitas tentativas em pouco tempo. Por segurança, aguarde ${segundos} ${unidade} para tentar novamente.`
}

/**
 * Traduz um erro de chamada à API em uma frase que uma pessoa sem conhecimento
 * técnico entenda e saiba o que fazer. Mensagens técnicas (backend, servidor,
 * status HTTP) nunca vão para a tela.
 *
 * Ordem: sem resposta (conexão) -> excesso de tentativas -> corpo grande demais
 * -> indisponibilidade (5xx) -> texto de negócio enviado pelo sistema (campo
 * `detail`, já escrito em português para o usuário) -> `padrao`.
 *
 * @param padrao frase usada quando o erro não tem uma causa conhecida.
 */
export function mensagemDeErroDaApi(error: unknown, padrao: string): string {
  if (!axios.isAxiosError(error) || !error.response) return SEM_CONEXAO

  const { status, data } = error.response

  const espera = segundosDeEspera(error)
  if (espera !== null) return mensagemDeEspera(espera)
  if (status === 413) return 'Os dados enviados são grandes demais. Reduza o conteúdo e tente novamente.'
  if (status >= 500) {
    return 'O sistema está temporariamente indisponível. Tente novamente em alguns instantes; se continuar, avise a equipe responsável.'
  }

  const detalhe = (data as { detail?: unknown } | undefined)?.detail
  return typeof detalhe === 'string' && detalhe.trim() ? detalhe : padrao
}

/**
 * Mensagem para telas de detalhe que não conseguiram carregar. "Não encontrado" só
 * aparece quando o sistema realmente respondeu 404 (ou a consulta deu certo e o item
 * não existe); falha de conexão ou do sistema ganha a frase correspondente.
 */
export function mensagemDeErroAoCarregar(error: unknown, naoEncontrado: string): string {
  if (error == null || (axios.isAxiosError(error) && error.response?.status === 404)) return naoEncontrado
  return mensagemDeErroDaApi(error, 'Não foi possível carregar os dados. Tente novamente em instantes.')
}
