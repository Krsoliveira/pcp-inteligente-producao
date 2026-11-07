import { lazy } from 'react'
import { createBrowserRouter, RouterProvider, Navigate } from 'react-router-dom'
import { Layout } from './components/Layout'
import { ProtectedRoute } from './components/ProtectedRoute'
import { ExigirPermissao } from './components/ExigirPermissao'
import { LoginPage } from './pages/LoginPage'
import { RotaDoAutocadastro } from './components/auth/RotaDoAutocadastro'

/** Cada tela vira um arquivo à parte: quem abre o login não baixa os gráficos nem as demais telas. */
const DashboardPage = lazy(() => import('./pages/DashboardPage').then((m) => ({ default: m.DashboardPage })))
const OrdensPage = lazy(() => import('./pages/OrdensPage').then((m) => ({ default: m.OrdensPage })))
const OrdensResultadosPage = lazy(() => import('./pages/OrdensResultadosPage').then((m) => ({ default: m.OrdensResultadosPage })))
const OrdemDetalhePage = lazy(() => import('./pages/OrdemDetalhePage').then((m) => ({ default: m.OrdemDetalhePage })))
const MateriaisPage = lazy(() => import('./pages/MateriaisPage').then((m) => ({ default: m.MateriaisPage })))
const MateriaisResultadosPage = lazy(() => import('./pages/MateriaisResultadosPage').then((m) => ({ default: m.MateriaisResultadosPage })))
const ListasTecnicasPage = lazy(() => import('./pages/ListasTecnicasPage').then((m) => ({ default: m.ListasTecnicasPage })))
const ListasTecnicasResultadosPage = lazy(() => import('./pages/ListasTecnicasResultadosPage').then((m) => ({ default: m.ListasTecnicasResultadosPage })))
const ListaTecnicaDetalhePage = lazy(() => import('./pages/ListaTecnicaDetalhePage').then((m) => ({ default: m.ListaTecnicaDetalhePage })))
const ListaTecnicaComparacaoPage = lazy(() => import('./pages/ListaTecnicaComparacaoPage').then((m) => ({ default: m.ListaTecnicaComparacaoPage })))
const TiposOrdemPage = lazy(() => import('./pages/TiposOrdemPage').then((m) => ({ default: m.TiposOrdemPage })))
const LoteDetalhePage = lazy(() => import('./pages/LoteDetalhePage').then((m) => ({ default: m.LoteDetalhePage })))
const NotasFiscaisPage = lazy(() => import('./pages/NotasFiscaisPage').then((m) => ({ default: m.NotasFiscaisPage })))
const NotasFiscaisResultadosPage = lazy(() => import('./pages/NotasFiscaisResultadosPage').then((m) => ({ default: m.NotasFiscaisResultadosPage })))
const NotaFiscalDetalhePage = lazy(() => import('./pages/NotaFiscalDetalhePage').then((m) => ({ default: m.NotaFiscalDetalhePage })))
const EstoquePage = lazy(() => import('./pages/EstoquePage').then((m) => ({ default: m.EstoquePage })))
const EstoqueResultadosPage = lazy(() => import('./pages/EstoqueResultadosPage').then((m) => ({ default: m.EstoqueResultadosPage })))
const EstoqueMaterialPage = lazy(() => import('./pages/EstoqueMaterialPage').then((m) => ({ default: m.EstoqueMaterialPage })))
const LancamentoDeConsumoPage = lazy(() => import('./pages/LancamentoDeConsumoPage').then((m) => ({ default: m.LancamentoDeConsumoPage })))
const PerfisAcessoPage = lazy(() => import('./pages/PerfisAcessoPage').then((m) => ({ default: m.PerfisAcessoPage })))
const RelatorioConsumoOrdensPage = lazy(() => import('./pages/RelatorioConsumoOrdensPage').then((m) => ({ default: m.RelatorioConsumoOrdensPage })))
const TrocarSenhaPage = lazy(() => import('./pages/TrocarSenhaPage').then((m) => ({ default: m.TrocarSenhaPage })))
const UsuariosAcessoPage = lazy(() => import('./pages/UsuariosAcessoPage').then((m) => ({ default: m.UsuariosAcessoPage })))

const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/cadastro',
    element: <RotaDoAutocadastro />,
  },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <Layout />,
        children: [
          // Troca de senha: obrigatória no primeiro acesso (ADR-0021) e voluntária depois; não exige permissão.
          { path: '/minha-senha', element: <TrocarSenhaPage /> },
          // Cada grupo exige a permissão da área (conforto; o servidor confere de novo em cada endpoint).
          // O painel é a entrada do sistema: sem acesso a ele, leva para a primeira tela permitida.
          {
            element: <ExigirPermissao qualquer={['DASHBOARD_VISUALIZAR']} paginaInicial />,
            children: [{ path: '/dashboard', element: <DashboardPage /> }],
          },
          // Produção
          {
            element: <ExigirPermissao qualquer={['ORDEM_VISUALIZAR']} />,
            children: [
              { path: '/ordens', element: <OrdensPage /> },
              { path: '/ordens/resultados', element: <OrdensResultadosPage /> },
              { path: '/ordens/:id', element: <OrdemDetalhePage /> },
              {
                element: <ExigirPermissao qualquer={['CONSUMO_REGISTRAR']} />,
                children: [{ path: '/ordens/:id/lancar-consumo', element: <LancamentoDeConsumoPage /> }],
              },
            ],
          },
          // Suprimentos (ADR-0012)
          {
            element: <ExigirPermissao qualquer={['NOTA_FISCAL_VISUALIZAR']} />,
            children: [
              { path: '/notas-fiscais', element: <NotasFiscaisPage /> },
              { path: '/notas-fiscais/resultados', element: <NotasFiscaisResultadosPage /> },
              { path: '/notas-fiscais/:id', element: <NotaFiscalDetalhePage /> },
            ],
          },
          {
            element: <ExigirPermissao qualquer={['ESTOQUE_VISUALIZAR']} />,
            children: [
              { path: '/estoque', element: <EstoquePage /> },
              { path: '/estoque/resultados', element: <EstoqueResultadosPage /> },
              { path: '/estoque/:materialId', element: <EstoqueMaterialPage /> },
              { path: '/lotes/:id', element: <LoteDetalhePage /> },
              // A lista de lotes virou o estoque por material.
              { path: '/lotes', element: <Navigate to="/estoque" replace /> },
            ],
          },
          // Cadastros
          {
            element: <ExigirPermissao qualquer={['MATERIAL_VISUALIZAR']} />,
            children: [
              { path: '/materiais', element: <MateriaisPage /> },
              { path: '/materiais/resultados', element: <MateriaisResultadosPage /> },
            ],
          },
          {
            element: <ExigirPermissao qualquer={['LISTA_VISUALIZAR_ATIVAS', 'LISTA_VISUALIZAR_TODAS']} />,
            children: [
              { path: '/listas-tecnicas', element: <ListasTecnicasPage /> },
              { path: '/listas-tecnicas/resultados', element: <ListasTecnicasResultadosPage /> },
              { path: '/listas-tecnicas/:id', element: <ListaTecnicaDetalhePage /> },
            ],
          },
          {
            element: <ExigirPermissao qualquer={['LISTA_COMPARAR']} />,
            children: [{ path: '/listas-tecnicas/comparar', element: <ListaTecnicaComparacaoPage /> }],
          },
          {
            element: <ExigirPermissao qualquer={['TIPO_ORDEM_VISUALIZAR']} />,
            children: [{ path: '/tipos-ordem', element: <TiposOrdemPage /> }],
          },
          // Relatórios (ADR-0023)
          {
            element: <ExigirPermissao qualquer={['RELATORIO_CONSUMO_VISUALIZAR']} />,
            children: [{ path: '/relatorios/consumo-de-ordens', element: <RelatorioConsumoOrdensPage /> }],
          },
          // Administração (ADR-0020)
          {
            element: <ExigirPermissao qualquer={['ACESSO_VISUALIZAR']} />,
            children: [
              { path: '/acessos/perfis', element: <PerfisAcessoPage /> },
              { path: '/acessos/usuarios', element: <UsuariosAcessoPage /> },
            ],
          },
        ],
      },
    ],
  },
  { path: '/', element: <Navigate to="/dashboard" replace /> },
  { path: '*', element: <Navigate to="/dashboard" replace /> },
])

export function App() {
  return <RouterProvider router={router} />
}
