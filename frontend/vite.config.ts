import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          react: ['react', 'react-dom', 'react-router-dom'],
          mui: ['@mui/material', '@mui/icons-material'],
          echarts: ['echarts', 'echarts-for-react'],
          query: ['@tanstack/react-query'],
        },
      },
    },
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
