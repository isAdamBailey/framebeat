import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'
import type { ViteSSGOptions } from 'vite-ssg'

declare module 'vite' {
  interface UserConfig {
    ssgOptions?: ViteSSGOptions
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  ssgOptions: {
    dirStyle: 'nested',
  },
})
