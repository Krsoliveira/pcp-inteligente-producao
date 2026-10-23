import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { consultarUsuarioLogado } from '../api/acessos'
import { useAuthStore } from '../store/authStore'
import type { Permissao } from '../types'

/** Chave da consulta de "quem sou eu"; qualquer tela pode invalidá-la depois de mudar acessos. */
export const CHAVE_USUARIO_LOGADO = ['acessos', 'eu'] as const

/**
 * Usuário logado e o que ele pode fazer (GET /acessos/eu). Serve para mostrar ou esconder telas e
 * botões; a barreira de verdade é o servidor, que confere a permissão em cada endpoint (ADR-0020).
 * As permissões são relidas ao voltar à janela, para acompanhar mudanças feitas por um gestor.
 */
export function usePermissoes() {
  const token = useAuthStore((s) => s.token)
  const consulta = useQuery({
    queryKey: CHAVE_USUARIO_LOGADO,
    queryFn: () => consultarUsuarioLogado(),
    enabled: !!token,
    staleTime: 30_000,
    refetchOnWindowFocus: true,
  })

  const conjunto = useMemo(() => new Set<Permissao>(consulta.data?.permissoes ?? []), [consulta.data])

  return {
    usuario: consulta.data,
    carregando: consulta.isLoading,
    erro: consulta.isError,
    recarregar: consulta.refetch,
    /** O usuário tem esta permissão? */
    tem: (permissao: Permissao) => conjunto.has(permissao),
    /** O usuário tem ao menos uma destas permissões? */
    temAlguma: (...permissoes: Permissao[]) => permissoes.some((p) => conjunto.has(p)),
    permissoes: conjunto,
  }
}
