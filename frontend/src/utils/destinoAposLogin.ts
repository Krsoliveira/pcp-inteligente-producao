const PADRAO = '/dashboard'

/**
 * Para onde ir depois do login: a página que a pessoa tentou abrir (ex.: o QR code de um lote), guardada no estado
 * da rota pelo ProtectedRoute. Só vale caminho interno: "//site" e "/\site" levam a outro site em alguns
 * navegadores, e um link malicioso não pode usar o login para tirar o usuário do sistema.
 */
export function destinoAposLogin(estado: unknown): string {
  const origem = (estado as { from?: unknown } | null)?.from
  if (typeof origem !== 'string') return PADRAO
  if (!origem.startsWith('/') || origem.startsWith('//') || origem.startsWith('/\\')) return PADRAO
  if (origem === '/login' || origem.startsWith('/login?')) return PADRAO
  return origem
}
