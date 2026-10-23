import { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from 'axios'

/** Erro como o Axios o entrega quando o servidor respondeu com `status`. */
export function erroHttp(status: number, data: unknown = {}, headers: Record<string, string> = {}): AxiosError {
  const config = { headers: new AxiosHeaders() } as InternalAxiosRequestConfig
  return new AxiosError('falha', 'ERR_BAD_RESPONSE', config, null, {
    status,
    statusText: '',
    data,
    headers,
    config,
  })
}

/** Erro sem resposta: servidor fora do ar, sem rede ou bloqueado pelo navegador. */
export function erroDeRede(): AxiosError {
  return new AxiosError('Network Error', 'ERR_NETWORK')
}
