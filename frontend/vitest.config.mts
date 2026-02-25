import { defineConfig } from 'vitest/config';
import angular from '@analogjs/vite-plugin-angular';

export default defineConfig({
  plugins: [
    // @ts-ignore
    angular.default ? angular.default() : angular(),
  ],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test-setup.ts'],
    include: ['src/**/*.spec.ts'],
    server: {
      deps: {
        inline: [
          '@angular/core',
          '@angular/common',
          '@angular/platform-browser-dynamic',
          '@angular/core/testing',
          '@angular/platform-browser-dynamic/testing',
        ],
      },
    },
  },
});