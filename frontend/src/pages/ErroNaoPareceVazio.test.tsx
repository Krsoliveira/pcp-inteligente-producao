import { screen, within } from '@testing-library/react'
import type { ReactElement } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as acessos from '../api/acessos'
import * as estoque from '../api/estoque'
import * as listasTecnicas from '../api/listasTecnicas'
import * as materiais from '../api/materiais'
import * as notasFiscais from '../api/notasFiscais'
import * as ordens from '../api/ordens'
import * as relatorios from '../api/relatorios'
import * as tiposOrdem from '../api/tiposOrdem'
import { erroHttp } from '../test/erros'
import { renderizarTela } from '../test/renderizar'
import { EstoqueResultadosPage } from './EstoqueResultadosPage'
import { ListasTecnicasResultadosPage } from './ListasTecnicasResultadosPage'
import { MateriaisResultadosPage } from './MateriaisResultadosPage'
import { NotasFiscaisResultadosPage } from './NotasFiscaisResultadosPage'
import { OrdensResultadosPage } from './OrdensResultadosPage'
import { PerfisAcessoPage } from './PerfisAcessoPage'
import { RelatorioConsumoOrdensPage } from './RelatorioConsumoOrdensPage'
import { TiposOrdemPage } from './TiposOrdemPage'
import { UsuariosAcessoPage } from './UsuariosAcessoPage'

vi.mock('../api/acessos')
vi.mock('../api/estoque')
vi.mock('../api/listasTecnicas')
vi.mock('../api/materiais')
vi.mock('../api/notasFiscais')
vi.mock('../api/ordens')
vi.mock('../api/relatorios')
vi.mock('../api/tiposOrdem')

const MODULOS = [acessos, estoque, listasTecnicas, materiais, notasFiscais, ordens, relatorios, tiposOrdem]

const TELAS: Array<[string, ReactElement, string]> = [
  ['ordens', <OrdensResultadosPage />, '/ordens/resultados'],
  ['materiais', <MateriaisResultadosPage />, '/materiais/resultados'],
  ['listas técnicas', <ListasTecnicasResultadosPage />, '/listas-tecnicas/resultados'],
  ['notas fiscais', <NotasFiscaisResultadosPage />, '/notas-fiscais/resultados'],
  ['estoque', <EstoqueResultadosPage />, '/estoque/resultados'],
  ['tipos de ordem', <TiposOrdemPage />, '/tipos-ordem'],
  ['perfis', <PerfisAcessoPage />, '/acessos/perfis'],
  ['usuários', <UsuariosAcessoPage />, '/acessos/usuarios'],
  ['relatório', <RelatorioConsumoOrdensPage />, '/relatorios/consumo-de-ordens?de=2026-01-01&ate=2026-03-31'],
]

describe('falha ao carregar não se apresenta como lista vazia', () => {
  beforeEach(() => {
    for (const modulo of MODULOS) {
      for (const funcao of Object.values(modulo)) {
        if (vi.isMockFunction(funcao)) funcao.mockReset().mockRejectedValue(erroHttp(500))
      }
    }
  })

  it.each(TELAS)('%s: mostra o erro, sem contagem zerada nem "nenhum cadastrado"', async (_nome, tela, rota) => {
    renderizarTela(tela, { rotas: [rota], padrao: rota.split('?')[0] })

    const alerta = await screen.findByRole('alert')
    expect(within(alerta).getByText(/temporariamente indispon/i)).toBeInTheDocument()
    expect(screen.queryByText(/^0 /)).not.toBeInTheDocument()
    expect(screen.queryByText(/^Nenhum/)).not.toBeInTheDocument()
  })
})
