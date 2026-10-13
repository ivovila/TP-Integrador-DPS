import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

const apiProxy = { '/api': { target: process.env.CERTIFLOW_API_URL ?? 'http://localhost:8080' } };

export default defineConfig({
  plugins: [react()],
  server: { proxy: apiProxy },
  preview: { proxy: apiProxy },
  test: {
    include: ['src/**/*.test.{ts,tsx}'],
    exclude: ['src/**/*.e2e.test.{ts,tsx}'],
  },
});
