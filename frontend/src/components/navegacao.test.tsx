import { describe, expect, it } from 'vitest'
import type { Permissao } from '../types'
import { primeiraRotaPermitida, secoesVisiveis } from './navegacao'

const com = (...permissoes: Permissao[]) => (...pedidas: Permissao[]) => pedidas.some((p) => permissoes.includes(p))
const rotas = (temAlguma: ReturnType<typeof com>) => secoesVisiveis(temAlguma).flatMap((s) => s.itens.map((i) => i.rota))

describe('menu por permissão', () => {
  it('quem não tem nenhuma permissão não vê item nenhum, e nenhuma seção vazia', () => {
    expect(secoesVisiveis(com())).toEqual([])
    expect(primeiraRotaPermitida(com())).toBeNull()
  })

  it('mostra só as telas que o perfil permite', () => {
    expect(rotas(com('ORDEM_VISUALIZAR', 'ESTOQUE_VISUALIZAR'))).toEqual(['/estoque', '/ordens'])
  })

  it('as listas técnicas aparecem com qualquer uma das duas permissões de visualizar', () => {
    expect(rotas(com('LISTA_VISUALIZAR_ATIVAS'))).toEqual(['/listas-tecnicas'])
    expect(rotas(com('LISTA_VISUALIZAR_TODAS'))).toEqual(['/listas-tecnicas'])
  })

  it('permissões de escrita sozinhas não abrem tela: ver exige a permissão de visualizar', () => {
    expect(rotas(com('ORDEM_CRIAR', 'MATERIAL_CADASTRAR'))).toEqual([])
  })

  it('Relatórios só aparece para quem pode ver o relatório de consumo, entre Produção e Administração', () => {
    expect(rotas(com('RELATORIO_CONSUMO_VISUALIZAR'))).toEqual(['/relatorios/consumo-de-ordens'])
    expect(secoesVisiveis(com('RELATORIO_CONSUMO_VISUALIZAR')).map((s) => s.titulo)).toEqual(['Relatórios'])
    expect(secoesVisiveis(com('ORDEM_VISUALIZAR', 'RELATORIO_CONSUMO_VISUALIZAR', 'ACESSO_VISUALIZAR')).map((s) => s.titulo))
      .toEqual(['Produção', 'Relatórios', 'Administração'])
    expect(rotas(com('CONSUMO_VISUALIZAR', 'ORDEM_VISUALIZAR'))).not.toContain('/relatorios/consumo-de-ordens') // ver consumo na ordem não dá o relatório
  })

  it('a Administração só aparece para quem pode ver acessos, com perfis e usuários', () => {
    expect(rotas(com('ACESSO_VISUALIZAR'))).toEqual(['/acessos/perfis', '/acessos/usuarios'])
    expect(secoesVisiveis(com('ACESSO_VISUALIZAR')).map((s) => s.titulo)).toEqual(['Administração'])
    expect(rotas(com('PERFIL_GERENCIAR'))).toEqual([])
  })

  it('a primeira tela permitida segue a ordem do menu', () => {
    expect(primeiraRotaPermitida(com('ORDEM_VISUALIZAR', 'MATERIAL_VISUALIZAR'))).toBe('/materiais')
    expect(primeiraRotaPermitida(com('DASHBOARD_VISUALIZAR', 'ORDEM_VISUALIZAR'))).toBe('/dashboard')
  })
})
