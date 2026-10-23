import type { ReactNode } from 'react'
import { usePermissoes } from '../hooks/usePermissoes'
import type { Permissao } from '../types'

interface SePermitidoProps {
  /** Mostra os filhos a quem tem ao menos uma destas permissões. */
  qualquer: Permissao[]
  children: ReactNode
}

/** Esconde botões e blocos que o perfil do usuário não pode usar. O servidor confere de novo, sempre. */
export function SePermitido({ qualquer, children }: SePermitidoProps) {
  const { temAlguma } = usePermissoes()
  return temAlguma(...qualquer) ? <>{children}</> : null
}
