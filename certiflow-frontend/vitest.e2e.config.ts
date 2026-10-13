import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [react()],
  test: {
    include: ['src/**/*.e2e.test.{ts,tsx}'],
    env: { CERTIFLOW_API_URL: process.env.CERTIFLOW_API_URL ?? 'http://localhost:8080' },
    testTimeout: 30000,
    hookTimeout: 30000,
    fileParallelism: false,
  },
});
