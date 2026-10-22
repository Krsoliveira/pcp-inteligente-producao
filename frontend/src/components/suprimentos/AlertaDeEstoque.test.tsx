import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { ResumoEstoque } from '../../types'
import { erroHttp } from '../../test/erros'
import { renderizarTela } from '../../test/renderizar'
import { AlertaDeEstoque } from './AlertaDeEstoque'

// Função simples no lugar de vi.fn: o Vitest 4 acompanha as promessas devolvidas por vi.fn e, quando
// uma delas é rejeitada, a rejeição acompanhada escapa como erro do teste mesmo tratada pela tela.
let consultas = 0
let responder: () => Promise<ResumoEstoque>
vi.mock('../../api/estoque', () => ({
  consultarResumoEstoque: () => { consultas++; return responder() },
}))

const resumo = (parcial: Partial<ResumoEstoque>): ResumoEstoque =>
  ({ totalMateriais: 20, materiaisVencendo: 0, materiasPrimasSemSaldo: 0, ...parcial })

describe('AlertaDeEstoque', () => {
  beforeEach(() => { consultas = 0 })

  it('avisa materiais vencendo e matérias-primas sem saldo, no plural', async () => {
    responder = async () => resumo({ materiaisVencendo: 3, materiasPrimasSemSaldo: 2 })
    renderizarTela(<AlertaDeEstoque />)

    const alerta = await screen.findByRole('alert')

    expect(alerta).toHaveTextContent('3 materiais têm lotes vencendo em até 30 dias.')
    expect(alerta).toHaveTextContent('2 matérias-primas estão sem saldo disponível.')
  })

  it('usa o singular quando há um só', async () => {
    responder = async () => resumo({ materiaisVencendo: 1, materiasPrimasSemSaldo: 1 })
    renderizarTela(<AlertaDeEstoque />)

    const alerta = await screen.findByRole('alert')

    expect(alerta).toHaveTextContent('1 material tem lote vencendo em até 30 dias.')
    expect(alerta).toHaveTextContent('1 matéria-prima está sem saldo disponível.')
  })

  it('mostra só o alerta que existe', async () => {
    responder = async () => resumo({ materiasPrimasSemSaldo: 4 })
    renderizarTela(<AlertaDeEstoque />)

    const alerta = await screen.findByRole('alert')

    expect(alerta).not.toHaveTextContent(/vencendo/)
    expect(alerta).toHaveTextContent('4 matérias-primas estão sem saldo disponível.')
  })

  it('não mostra nada quando não há alertas', async () => {
    responder = async () => resumo({})
    renderizarTela(<AlertaDeEstoque />)

    await vi.waitFor(() => expect(consultas).toBe(1))
    await new Promise((resolver) => setTimeout(resolver, 20))

    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('não atrapalha a tela quando o resumo falha', async () => {
    responder = () => Promise.reject(erroHttp(503))
    renderizarTela(<AlertaDeEstoque />)

    await vi.waitFor(() => expect(consultas).toBe(1))
    await new Promise((resolver) => setTimeout(resolver, 50))

    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
