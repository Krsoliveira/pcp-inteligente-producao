import { Navigate, Outlet } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Skeleton from '@mui/material/Skeleton'
import { usePermissoes } from '../hooks/usePermissoes'
import type { Permissao } from '../types'
import { primeiraRotaPermitida } from './navegacao'
import { SemAcesso } from './SemAcesso'

interface ExigirPermissaoProps {
  /** Basta ter uma destas permissões para abrir as rotas filhas. */
  qualquer: Permissao[]
  /** Rota de entrada do sistema: sem permissão, leva o usuário para a primeira tela que ele pode abrir. */
  paginaInicial?: boolean
}

/**
 * Guarda de rota por permissão. É conforto (esconde o que o servidor recusaria de qualquer jeito): a
 * barreira de verdade é o servidor, em cada endpoint (ADR-0020).
 */
export function ExigirPermissao({ qualquer, paginaInicial = false }: ExigirPermissaoProps) {
  const { carregando, erro, recarregar, temAlguma } = usePermissoes()

  if (carregando) return <Skeleton variant="rounded" height={240} />
  if (erro) {
    return (
      <Alert severity="error" action={<Button color="inherit" size="small" onClick={() => recarregar()}>Tentar de novo</Button>}>
        Não foi possível verificar o seu acesso. Tente novamente em instantes.
      </Alert>
    )
  }
  if (temAlguma(...qualquer)) return <Outlet />

  const inicial = primeiraRotaPermitida(temAlguma)
  if (paginaInicial && inicial) return <Navigate to={inicial} replace />
  return <SemAcesso rotaInicial={inicial} />
}
