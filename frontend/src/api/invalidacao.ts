import type { QueryClient } from '@tanstack/react-query'

/**
 * O que muda quando a ordem ou o consumo dela muda: a própria ordem, as listas, o saldo dos lotes e o histórico.
 * Numa recusa também vale chamar: costuma significar que outra pessoa mudou a ordem e a tela está velha.
 */
export function invalidarDadosDaOrdem(queryClient: QueryClient, ordemId: string | undefined) {
  for (const chave of [['ordem', ordemId], ['consumos', ordemId], ['lotes-ordem', ordemId], ['ordens'], ['lotes'], ['estoque'], ['lotes-disponiveis'], ['auditoria']]) {
    queryClient.invalidateQueries({ queryKey: chave })
  }
}
