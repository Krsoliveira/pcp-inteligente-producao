import { createBrowserRouter, RouterProvider, Navigate } from 'react-router-dom'
import { Layout } from './components/Layout'
import { ProtectedRoute } from './components/ProtectedRoute'
import { LoginPage } from './pages/LoginPage'
import { CadastroPage } from './pages/CadastroPage'
import { DashboardPage } from './pages/DashboardPage'
import { OrdensPage } from './pages/OrdensPage'
import { OrdemDetalhePage } from './pages/OrdemDetalhePage'
import { MateriaisPage } from './pages/MateriaisPage'
import { ListasTecnicasPage } from './pages/ListasTecnicasPage'
import { TiposOrdemPage } from './pages/TiposOrdemPage'
import { LotesPage } from './pages/LotesPage'

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
          { path: '/ordens/:id', element: <OrdemDetalhePage /> },
          { path: '/lotes', element: <LotesPage /> },
          // Cadastros
          { path: '/materiais', element: <MateriaisPage /> },
          { path: '/listas-tecnicas', element: <ListasTecnicasPage /> },
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
