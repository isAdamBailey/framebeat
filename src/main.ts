import { ViteSSG } from 'vite-ssg/single-page'
import '@fontsource-variable/fraunces/soft.css'
import './style.css'
import App from './App.vue'

// Prerendered at build time (`vite-ssg build`) so crawlers and link previews
// get the full page markup; the client hydrates it as usual.
export const createApp = ViteSSG(App)
