import type { ReactNode } from 'react'
import DashboardIcon from '@mui/icons-material/Dashboard'
import ListAltIcon from '@mui/icons-material/ListAlt'
import InventoryIcon from '@mui/icons-material/Inventory2Outlined'
import AccountTreeIcon from '@mui/icons-material/AccountTreeOutlined'
import LabelOutlinedIcon from '@mui/icons-material/LabelOutlined'
import ReceiptLongIcon from '@mui/icons-material/ReceiptLongOutlined'
import WarehouseIcon from '@mui/icons-material/WarehouseOutlined'
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettingsOutlined'
import GroupsIcon from '@mui/icons-material/GroupsOutlined'
import AssessmentIcon from '@mui/icons-material/AssessmentOutlined'
import type { Permissao } from '../types'

export interface NavItem {
  label: string
  icon: ReactNode
  rota: string
  /** Outras rotas que também destacam o item (ex.: telas de detalhe). */
  ativoTambem?: string[]
  /** O item aparece para quem tem ao menos uma destas permissões. */
  permissoes: Permissao[]
}

export interface NavSection {
  titulo: string
  itens: NavItem[]
}

export const NAV_SECTIONS: NavSection[] = [
  {
    titulo: 'Painel',
    itens: [
      { label: 'Dashboard', icon: <DashboardIcon fontSize="small" />, rota: '/dashboard', permissoes: ['DASHBOARD_VISUALIZAR'] },
    ],
  },
  {
    titulo: 'Cadastros',
    itens: [
      { label: 'Materiais', icon: <InventoryIcon fontSize="small" />, rota: '/materiais', permissoes: ['MATERIAL_VISUALIZAR'] },
      {
        label: 'Listas Técnicas', icon: <AccountTreeIcon fontSize="small" />, rota: '/listas-tecnicas',
        permissoes: ['LISTA_VISUALIZAR_ATIVAS', 'LISTA_VISUALIZAR_TODAS'],
      },
      { label: 'Tipos de Ordem', icon: <LabelOutlinedIcon fontSize="small" />, rota: '/tipos-ordem', permissoes: ['TIPO_ORDEM_VISUALIZAR'] },
    ],
  },
  {
    titulo: 'Suprimentos',
    itens: [
      { label: 'Entrada de notas', icon: <ReceiptLongIcon fontSize="small" />, rota: '/notas-fiscais', permissoes: ['NOTA_FISCAL_VISUALIZAR'] },
      {
        label: 'Estoque', icon: <WarehouseIcon fontSize="small" />, rota: '/estoque', ativoTambem: ['/lotes'],
        permissoes: ['ESTOQUE_VISUALIZAR'],
      },
    ],
  },
  {
    titulo: 'Produção',
    itens: [
      { label: 'Ordens', icon: <ListAltIcon fontSize="small" />, rota: '/ordens', permissoes: ['ORDEM_VISUALIZAR'] },
    ],
  },
  {
    titulo: 'Relatórios',
    itens: [
      {
        label: 'Consumo de ordens', icon: <AssessmentIcon fontSize="small" />, rota: '/relatorios/consumo-de-ordens',
        permissoes: ['RELATORIO_CONSUMO_VISUALIZAR'],
      },
    ],
  },
  {
    titulo: 'Administração',
    itens: [
      { label: 'Perfis de acesso', icon: <AdminPanelSettingsIcon fontSize="small" />, rota: '/acessos/perfis', permissoes: ['ACESSO_VISUALIZAR'] },
      { label: 'Usuários', icon: <GroupsIcon fontSize="small" />, rota: '/acessos/usuarios', permissoes: ['ACESSO_VISUALIZAR'] },
    ],
  },
]

/** O que o usuário pode abrir no menu, sem as seções que ficaram vazias. */
export function secoesVisiveis(temAlguma: (...permissoes: Permissao[]) => boolean): NavSection[] {
  return NAV_SECTIONS
    .map((secao) => ({ ...secao, itens: secao.itens.filter((item) => temAlguma(...item.permissoes)) }))
    .filter((secao) => secao.itens.length > 0)
}

/** Primeira tela que o usuário pode abrir; `null` se o perfil não dá acesso a nenhuma. */
export function primeiraRotaPermitida(temAlguma: (...permissoes: Permissao[]) => boolean): string | null {
  return secoesVisiveis(temAlguma)[0]?.itens[0]?.rota ?? null
}
