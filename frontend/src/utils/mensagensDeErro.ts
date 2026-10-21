import axios from 'axios'

/**
 * Traduz um erro de chamada à API em uma frase que uma pessoa sem conhecimento
 * técnico entenda e saiba o que fazer. Mensagens técnicas (backend, servidor,
 * status HTTP) nunca vão para a tela.
 *
 * @param padrao frase usada quando o erro não tem uma causa conhecida.
 */
export function mensagemDeErroDaApi(error: unknown, padrao: string): string {
  if (!axios.isAxiosError(error) || !error.response) {
    return 'Não foi possível conectar ao sistema. Verifique sua conexão com a internet e tente novamente em instantes.'
  }

  const { status, headers } = error.response

  if (status === 429) {
    const segundos = Number(headers?.['retry-after'])
    const espera = Number.isFinite(segundos) && segundos > 0 ? `${Math.ceil(segundos)} segundos` : 'um minuto'
    return `Muitas tentativas em pouco tempo. Por segurança, aguarde ${espera} e tente novamente.`
  }
  if (status === 413) {
    return 'Os dados enviados são grandes demais. Reduza o conteúdo e tente novamente.'
  }
  if (status >= 500) {
    return 'O sistema está temporariamente indisponível. Tente novamente em alguns instantes; se continuar, avise a equipe responsável.'
  }
  return padrao
}
