import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Sem globais do vitest, o Testing Library não limpa a tela sozinho entre os testes.
afterEach(() => {
  cleanup()
  localStorage.clear()
})
