import { createBrowserRouter, RouterProvider, Navigate } from 'react-router-dom'
import { Layout } from './components/Layout'
import { ProtectedRoute } from './components/ProtectedRoute'
import { ExigirPermissao } from './components/ExigirPermissao'
import { LoginPage } from './pages/LoginPage'
import { CadastroPage } from './pages/CadastroPage'
import { DashboardPage } from './pages/DashboardPage'
import { OrdensPage } from './pages/OrdensPage'
import { OrdensResultadosPage } from './pages/OrdensResultadosPage'
import { OrdemDetalhePage } from './pages/OrdemDetalhePage'
import { MateriaisPage } from './pages/MateriaisPage'
import { MateriaisResultadosPage } from './pages/MateriaisResultadosPage'
import { ListasTecnicasPage } from './pages/ListasTecnicasPage'
import { ListasTecnicasResultadosPage } from './pages/ListasTecnicasResultadosPage'
import { ListaTecnicaDetalhePage } from './pages/ListaTecnicaDetalhePage'
import { ListaTecnicaComparacaoPage } from './pages/ListaTecnicaComparacaoPage'
import { TiposOrdemPage } from './pages/TiposOrdemPage'
import { LoteDetalhePage } from './pages/LoteDetalhePage'
import { NotasFiscaisPage } from './pages/NotasFiscaisPage'
import { NotasFiscaisResultadosPage } from './pages/NotasFiscaisResultadosPage'
import { NotaFiscalDetalhePage } from './pages/NotaFiscalDetalhePage'
import { EstoquePage } from './pages/EstoquePage'
import { EstoqueResultadosPage } from './pages/EstoqueResultadosPage'
import { EstoqueMaterialPage } from './pages/EstoqueMaterialPage'
import { PerfisAcessoPage } from './pages/PerfisAcessoPage'
import { UsuariosAcessoPage } from './pages/UsuariosAcessoPage'

const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/cadastro',
    element: <CadastroPage />,
  },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <Layout />,
        children: [
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
