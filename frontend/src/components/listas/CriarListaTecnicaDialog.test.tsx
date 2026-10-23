import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { cadastrarListaTecnica } from '../../api/listasTecnicas'
import { listarMateriais } from '../../api/materiais'
import { erroHttp } from '../../test/erros'
import { listaTecnica, material } from '../../test/fabricas'
import { renderizarTela } from '../../test/renderizar'
import { CriarListaTecnicaDialog } from './CriarListaTecnicaDialog'

vi.mock('../../api/listasTecnicas', () => ({ cadastrarListaTecnica: vi.fn() }))
vi.mock('../../api/materiais', () => ({ listarMateriais: vi.fn() }))

const cadastrarMock = vi.mocked(cadastrarListaTecnica)

const modelo = listaTecnica({
  id: 'l-2', versao: 'v2', status: 'ATIVA',
  itens: [
    { id: 'i-1', materialComponenteId: 'm-2', quantidadePlanejada: 2.5, unidadeDeMedida: 'kg' },
    { id: 'i-2', materialComponenteId: 'm-3', quantidadePlanejada: 8, unidadeDeMedida: 'un' },
  ],
})

function montar(props: Partial<React.ComponentProps<typeof CriarListaTecnicaDialog>> = {}) {
  const onFechar = vi.fn()
  const onCriada = vi.fn()
  renderizarTela(<CriarListaTecnicaDialog aberto onFechar={onFechar} onCriada={onCriada} {...props} />)
  return { onFechar, onCriada }
}

describe('CriarListaTecnicaDialog', () => {
  beforeEach(() => {
    cadastrarMock.mockReset()
    vi.mocked(listarMateriais).mockResolvedValue([
      material({ id: 'm-1', codigo: '103000001', descricao: 'Viga metálica' }),
      material({ id: 'm-2', codigo: '110000001', descricao: 'Aço inox', tipo: 'MATERIA_PRIMA', unidadeDeMedida: 'kg' }),
      material({ id: 'm-3', codigo: '110000002', descricao: 'Parafuso', tipo: 'MATERIA_PRIMA', unidadeDeMedida: 'un' }),
    ])
  })

  describe('do zero', () => {
    it('começa vazio, com o material livre para escolher', async () => {
      montar()

      expect(await screen.findByText('Nova Lista Técnica (BOM)')).toBeInTheDocument()
      expect(screen.getByLabelText(/^Versão/)).toHaveValue('')
      expect(screen.getAllByRole('combobox', { name: /Material componente/ })).toHaveLength(1)
      expect(screen.getByRole('combobox', { name: /^Material \*/ })).not.toHaveAttribute('aria-disabled', 'true')
    })

    it('exige material, versão e ao menos um componente', async () => {
      montar()

      await userEvent.click(await screen.findByRole('button', { name: 'Criar Lista' }))

      expect(await screen.findByText('Material e versão são obrigatórios.')).toBeInTheDocument()
      expect(cadastrarMock).not.toHaveBeenCalled()
    })
  })

  describe('a partir de uma versão', () => {
    it('copia os componentes, fixa o material e sugere a próxima versão', async () => {
      montar({ modelo, versoesExistentes: ['v1', 'v2'] })

      expect(await screen.findByText('Nova versão a partir da v2')).toBeInTheDocument()
      expect(screen.getByLabelText(/^Versão/)).toHaveValue('v3')
      expect(screen.getAllByRole('combobox', { name: /Material componente/ })).toHaveLength(2)
      expect(screen.getAllByLabelText('Qtd').map((c) => (c as HTMLInputElement).value)).toEqual(['2.5', '8'])
      expect(screen.getAllByLabelText('Un.').map((c) => (c as HTMLInputElement).value)).toEqual(['kg', 'un'])
      await waitFor(() => expect(screen.getByRole('combobox', { name: /^Material \*/ })).toHaveAttribute('aria-disabled', 'true'))
      expect(screen.getByText(/nasce em revisão/i)).toBeInTheDocument()
    })

    it('sem rótulo no padrão "v1, v2…" deixa a versão em branco para o usuário digitar', async () => {
      montar({ modelo, versoesExistentes: ['rev-a'] })

      await screen.findByText('Nova versão a partir da v2')

      expect(screen.getByLabelText(/^Versão/)).toHaveValue('')
    })

    it('envia o material do modelo, a versão digitada e os componentes ajustados', async () => {
      cadastrarMock.mockResolvedValue()
      const { onCriada, onFechar } = montar({ modelo, versoesExistentes: ['v1', 'v2'] })
      await screen.findByText('Nova versão a partir da v2')

      const qtds = screen.getAllByLabelText('Qtd')
      await userEvent.clear(qtds[0])
      await userEvent.type(qtds[0], '3')
      await userEvent.click(screen.getByRole('button', { name: 'Criar versão' }))

      await waitFor(() => expect(cadastrarMock).toHaveBeenCalled())
      expect(cadastrarMock.mock.calls[0][0]).toEqual({
        materialId: 'm-1',
        versao: 'v3',
        itens: [
          { materialComponenteId: 'm-2', quantidadePlanejada: 3, unidadeDeMedida: 'kg' },
          { materialComponenteId: 'm-3', quantidadePlanejada: 8, unidadeDeMedida: 'un' },
        ],
      })
      await waitFor(() => expect(onCriada).toHaveBeenCalledWith('m-1', 'v3'))
      expect(onFechar).toHaveBeenCalled()
    })

    it('remover um componente da cópia não altera a versão original', async () => {
      cadastrarMock.mockResolvedValue()
      montar({ modelo, versoesExistentes: ['v1', 'v2'] })
      await screen.findByText('Nova versão a partir da v2')

      await userEvent.click(screen.getByRole('button', { name: 'Remover componente 2' }))
      await userEvent.click(screen.getByRole('button', { name: 'Criar versão' }))

      await waitFor(() => expect(cadastrarMock).toHaveBeenCalled())
      expect(cadastrarMock.mock.calls[0][0].itens).toHaveLength(1)
      expect(modelo.itens).toHaveLength(2)
    })

    it('mostra o motivo quando o sistema recusa (versão repetida) e não fecha', async () => {
      cadastrarMock.mockRejectedValue(erroHttp(409, { detail: 'A versão v3 já existe para este material.' }))
      const { onFechar, onCriada } = montar({ modelo, versoesExistentes: ['v1', 'v2'] })
      await screen.findByText('Nova versão a partir da v2')

      await userEvent.click(screen.getByRole('button', { name: 'Criar versão' }))

      expect(await screen.findByText('A versão v3 já existe para este material.')).toBeInTheDocument()
      expect(onFechar).not.toHaveBeenCalled()
      expect(onCriada).not.toHaveBeenCalled()
    })
  })
})
