import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// ─────────────────────────────────────────────────────────────────────────────
// vite.config.js
//
// Vite is the build tool and dev server for this React project.
//
// Key concepts:
//   • plugins: [react()] → enables JSX transform + React Fast Refresh
//     (Fast Refresh = hot module replacement that preserves component state
//      when you save a file – like live reload but smarter)
//
//   • server.proxy: during development, any request to /api/* is forwarded
//     to http://localhost:8080 (Spring Boot). This avoids CORS issues in dev
//     because the browser thinks it's talking to the same origin.
//
//     Real-world analogy: like a hotel concierge who, when you ask for room
//     service, calls the kitchen on your behalf. From your perspective, you
//     only talked to the concierge (Vite), not the kitchen (Spring Boot).
//
//   • resolve.alias: lets us write import '@/components/X' instead of
//     '../../components/X' – cleaner imports regardless of file depth.
// ─────────────────────────────────────────────────────────────────────────────
export default defineConfig({
  plugins: [react()],

  server: {
    port: 5173,
    // Proxy /api/* → Spring Boot in development
    // In production, your reverse proxy (nginx/caddy) handles this
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,   // sets the Host header to the target host
        secure: false,        // accept self-signed certs in dev
      }
    }
  },

  resolve: {
    alias: {
      '@': '/src',  // import '@/services/api' instead of '../../services/api'
    }
  },

  build: {
    outDir: 'dist',
    sourcemap: true,  // generates .map files for debugging production builds
    rollupOptions: {
      output: {
        // Split vendor libraries into separate chunks for better caching
        // Recharts and React are large – browsers cache them between deploys
        manualChunks: {
          vendor: ['react', 'react-dom', 'react-router-dom'],
          charts: ['recharts'],
        }
      }
    }
  }
})
