import { ViteSSG } from 'vite-ssg'
import '@fontsource-variable/fraunces/soft.css'
import './style.css'
import App from './App.vue'
import HomeView from './views/HomeView.vue'
import PrivacyView from './views/PrivacyView.vue'

// Prerendered at build time (`vite-ssg build`) so crawlers and link previews
// get the full page markup; the client hydrates it as usual.
export const createApp = ViteSSG(App, {
  routes: [
    { path: '/', component: HomeView },
    { path: '/privacy', component: PrivacyView },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})
