import { defineConfig } from 'vite';
import { resolve } from 'path';

export default defineConfig({
  base: './', // Risolve i percorsi relativi su GitHub Pages (es. /FlickTrove/)
  build: {
    outDir: '../docs', // Esporta nella cartella di GitHub Pages
    emptyOutDir: true, // Pulisce la cartella docs prima della build
    rollupOptions: {
      input: {
        main: resolve(__dirname, 'index.html'),
        privacy: resolve(__dirname, 'privacy.html'),
        terms: resolve(__dirname, 'terms.html'),
        open: resolve(__dirname, 'open.html')
      }
    }
  }
});
