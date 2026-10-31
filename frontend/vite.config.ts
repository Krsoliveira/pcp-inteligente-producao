import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    rolldownOptions: {
      output: {
        // Bibliotecas em pedaços próprios: mudam pouco e ficam em cache no navegador entre versões da aplicação.
        // O Vite 8 (Rolldown) trocou o manualChunks em objeto por grupos que casam o caminho do módulo.
        codeSplitting: {
          groups: [
            { name: 'react', test: /node_modules[\\/](react|react-dom|react-router|react-router-dom|scheduler)[\\/]/ },
            { name: 'mui', test: /node_modules[\\/](@mui|@emotion)[\\/]/ },
            { name: 'echarts', test: /node_modules[\\/](echarts|echarts-for-react|zrender)[\\/]/ },
            { name: 'query', test: /node_modules[\\/]@tanstack[\\/]/ },
          ],
        },
      },
    },
  },
  test: {
    // Ambiente de navegador simulado; as importações do vitest são explícitas (sem globais).
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
    css: false,
    // Telas com MUI + userEvent levam alguns segundos com vários arquivos em paralelo; o padrão (5 s) dava falha intermitente.
    testTimeout: 15000,
  },
  server: {
    port: 3000,
    proxy: {
      // Em desenvolvimento, redireciona /api/* para o backend Spring Boot
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
