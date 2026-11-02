import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In dev, everything goes through the gateway on 8080, same as nginx does in production.
const gateway = process.env.GATEWAY_URL ?? 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': gateway,
      '/soap': gateway,
      '/admin': gateway,
      '/ws': { target: gateway.replace(/^http/, 'ws'), ws: true },
    },
  },
});
