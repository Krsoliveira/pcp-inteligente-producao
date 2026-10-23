import { AxiosError, type AxiosAdapter, type InternalAxiosRequestConfig } from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiClient } from './client'

const adaptadorOriginal = apiClient.defaults.adapter
const localizacaoOriginal = window.location

function responder(status: number, data: unknown): AxiosAdapter {
  return (config: InternalAxiosRequestConfig) =>
    Promise.reject(new AxiosError('falha', 'ERR_BAD_RESPONSE', config, null, { status, statusText: '', data, headers: {}, config }))
}

function simularLocalizacao(pathname: string) {
  const local = { pathname, href: pathname }
  Object.defineProperty(window, 'location', { configurable: true, value: local })
  return local
}

describe('apiClient: resposta do servidor', () => {
  beforeEach(() => simularLocalizacao('/ordens'))
  afterEach(() => {
    apiClient.defaults.adapter = adaptadorOriginal
    Object.defineProperty(window, 'location', { configurable: true, value: localizacaoOriginal })
    vi.restoreAllMocks()
  })

  it('403 com codigo TROCAR_SENHA leva à tela de troca de senha e repassa o erro', async () => {
    const local = simularLocalizacao('/ordens')
    apiClient.defaults.adapter = responder(403, { codigo: 'TROCAR_SENHA', detail: 'Defina uma nova senha para continuar.' })

    await expect(apiClient.get('/v1/ordens')).rejects.toBeInstanceOf(AxiosError)

    expect(local.href).toBe('/minha-senha')
  })

  it('já na tela de troca de senha não redireciona de novo (evita laço)', async () => {
    const local = simularLocalizacao('/minha-senha')
    apiClient.defaults.adapter = responder(403, { codigo: 'TROCAR_SENHA' })

    await expect(apiClient.get('/v1/acessos/eu')).rejects.toBeInstanceOf(AxiosError)

    expect(local.href).toBe('/minha-senha')
    expect(local.pathname).toBe('/minha-senha')
  })

  it('um 403 comum (sem permissão) não redireciona', async () => {
    const local = simularLocalizacao('/ordens')
    apiClient.defaults.adapter = responder(403, { detail: 'Sem permissão.' })

    await expect(apiClient.get('/v1/materiais')).rejects.toBeInstanceOf(AxiosError)

    expect(local.href).toBe('/ordens')
  })

  it('422 de senha atual errada não desloga nem redireciona (só o 401 desloga)', async () => {
    const local = simularLocalizacao('/minha-senha')
    apiClient.defaults.adapter = responder(422, { detail: 'A senha atual não confere.' })

    await expect(apiClient.put('/v1/acessos/minha-senha', {})).rejects.toBeInstanceOf(AxiosError)

    expect(local.href).toBe('/minha-senha')
  })
})
