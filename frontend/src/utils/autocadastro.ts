/**
 * O build de produção desliga a oferta do cadastro público (`.env.production`, ADR-0031). Isto só evita mostrar uma tela
 * que não funcionaria: quem barra de verdade é o servidor.
 */
export const autocadastroHabilitado = (): boolean => import.meta.env.VITE_AUTOCADASTRO !== 'false'
