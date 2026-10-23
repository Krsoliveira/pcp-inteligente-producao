import { createBrowserRouter, RouterProvider, Navigate } from 'react-router-dom'
import { Layout } from './components/Layout'
import { ProtectedRoute } from './components/ProtectedRoute'
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
          { path: '/dashboard', element: <DashboardPage /> },
          // Produção
          { path: '/ordens', element: <OrdensPage /> },
          { path: '/ordens/resultados', element: <OrdensResultadosPage /> },
          { path: '/ordens/:id', element: <OrdemDetalhePage /> },
          // Suprimentos (ADR-0012)
          { path: '/notas-fiscais', element: <NotasFiscaisPage /> },
          { path: '/notas-fiscais/resultados', element: <NotasFiscaisResultadosPage /> },
          { path: '/notas-fiscais/:id', element: <NotaFiscalDetalhePage /> },
          { path: '/estoque', element: <EstoquePage /> },
          { path: '/estoque/resultados', element: <EstoqueResultadosPage /> },
          { path: '/estoque/:materialId', element: <EstoqueMaterialPage /> },
          { path: '/lotes/:id', element: <LoteDetalhePage /> },
          // A lista de lotes virou o estoque por material.
          { path: '/lotes', element: <Navigate to="/estoque" replace /> },
          // Cadastros
          { path: '/materiais', element: <MateriaisPage /> },
          { path: '/materiais/resultados', element: <MateriaisResultadosPage /> },
          { path: '/listas-tecnicas', element: <ListasTecnicasPage /> },
          { path: '/listas-tecnicas/resultados', element: <ListasTecnicasResultadosPage /> },
          { path: '/listas-tecnicas/comparar', element: <ListaTecnicaComparacaoPage /> },
          { path: '/listas-tecnicas/:id', element: <ListaTecnicaDetalhePage /> },
          { path: '/tipos-ordem', element: <TiposOrdemPage /> },
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
