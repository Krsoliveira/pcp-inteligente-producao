import { describe, expect, it } from 'vitest'
import { erroDeRede, erroHttp } from '../test/erros'
import {
  mensagemDeEspera,
  mensagemDeErroAoCarregar,
  mensagemDeErroDaApi,
  segundosDeEspera,
} from './mensagensDeErro'

const TERMOS_TECNICOS = /backend|servidor|http|status|erro inesperado/i

describe('segundosDeEspera', () => {
  it('lê o Retry-After de uma resposta 429', () => {
    expect(segundosDeEspera(erroHttp(429, {}, { 'retry-after': '36' }))).toBe(36)
  })

  it('arredonda para cima', () => {
    expect(segundosDeEspera(erroHttp(429, {}, { 'retry-after': '2.2' }))).toBe(3)
  })

  it('assume 60 segundos quando o 429 não informa o tempo, ou informa algo inválido', () => {
    expect(segundosDeEspera(erroHttp(429))).toBe(60)
    expect(segundosDeEspera(erroHttp(429, {}, { 'retry-after': 'abc' }))).toBe(60)
    expect(segundosDeEspera(erroHttp(429, {}, { 'retry-after': '0' }))).toBe(60)
  })

  it('devolve null para qualquer outro erro', () => {
    expect(segundosDeEspera(erroHttp(401))).toBeNull()
    expect(segundosDeEspera(erroHttp(500))).toBeNull()
    expect(segundosDeEspera(erroDeRede())).toBeNull()
    expect(segundosDeEspera(new Error('x'))).toBeNull()
  })
})

describe('mensagemDeEspera', () => {
  it('usa o plural', () => {
    expect(mensagemDeEspera(53)).toContain('aguarde 53 segundos')
  })

  it('usa o singular para 1 segundo', () => {
    expect(mensagemDeEspera(1)).toContain('aguarde 1 segundo para')
  })
})

describe('mensagemDeErroDaApi', () => {
  it('explica falta de conexão quando não há resposta (rede ou erro desconhecido)', () => {
    expect(mensagemDeErroDaApi(erroDeRede(), 'padrão')).toMatch(/conectar ao sistema/i)
    expect(mensagemDeErroDaApi(new Error('x'), 'padrão')).toMatch(/conectar ao sistema/i)
  })

  it('informa quanto aguardar no excesso de tentativas', () => {
    expect(mensagemDeErroDaApi(erroHttp(429, {}, { 'retry-after': '12' }), 'padrão')).toContain('aguarde 12 segundos')
  })

  it('avisa que os dados enviados são grandes demais', () => {
    expect(mensagemDeErroDaApi(erroHttp(413), 'padrão')).toMatch(/grandes demais/i)
  })

  it('avisa indisponibilidade em qualquer erro 5xx', () => {
    expect(mensagemDeErroDaApi(erroHttp(500), 'padrão')).toMatch(/temporariamente indisponível/i)
    expect(mensagemDeErroDaApi(erroHttp(503), 'padrão')).toMatch(/temporariamente indisponível/i)
  })

  it('mostra o motivo de negócio enviado pelo sistema (detail)', () => {
    expect(mensagemDeErroDaApi(erroHttp(422, { detail: 'Lista técnica já está ativa.' }), 'padrão'))
      .toBe('Lista técnica já está ativa.')
  })

  it('usa a frase padrão quando não há detail útil', () => {
    expect(mensagemDeErroDaApi(erroHttp(400, { detail: '   ' }), 'padrão')).toBe('padrão')
    expect(mensagemDeErroDaApi(erroHttp(404, {}), 'padrão')).toBe('padrão')
    expect(mensagemDeErroDaApi(erroHttp(400, { detail: 42 }), 'padrão')).toBe('padrão')
  })

  it('não ignora o 5xx por haver um detail (a mensagem técnica não vai para a tela)', () => {
    expect(mensagemDeErroDaApi(erroHttp(500, { detail: 'NullPointerException em X' }), 'padrão'))
      .toMatch(/temporariamente indisponível/i)
  })

  it('nunca usa termos técnicos nas frases fixas', () => {
    const erros = [erroDeRede(), erroHttp(429), erroHttp(413), erroHttp(500)]
    for (const erro of erros) {
      expect(mensagemDeErroDaApi(erro, 'padrão')).not.toMatch(TERMOS_TECNICOS)
    }
  })
})

describe('mensagemDeErroAoCarregar', () => {
  it('diz "não encontrado" quando o sistema responde 404', () => {
    expect(mensagemDeErroAoCarregar(erroHttp(404), 'Lote não encontrado.')).toBe('Lote não encontrado.')
  })

  it('diz "não encontrado" quando a consulta deu certo e o item não existe (sem erro)', () => {
    expect(mensagemDeErroAoCarregar(null, 'Lote não encontrado.')).toBe('Lote não encontrado.')
  })

  it('não diz "não encontrado" quando o problema é de conexão ou do sistema', () => {
    expect(mensagemDeErroAoCarregar(erroDeRede(), 'Lote não encontrado.')).toMatch(/conectar ao sistema/i)
    expect(mensagemDeErroAoCarregar(erroHttp(500), 'Lote não encontrado.')).toMatch(/indisponível/i)
  })

  it('usa uma frase genérica de carregamento para outros erros', () => {
    expect(mensagemDeErroAoCarregar(erroHttp(403), 'Lote não encontrado.')).toMatch(/carregar os dados/i)
  })
})
