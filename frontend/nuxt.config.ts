// https://nuxt.com/docs/api/configuration/nuxt-config
const env = (globalThis as { process?: { env?: Record<string, string | undefined> } }).process?.env
const apiProxyTarget = env?.NUXT_API_PROXY_TARGET ?? 'http://localhost:8080'

export default defineNuxtConfig({
  modules: [
    '@nuxt/eslint',
    '@nuxt/ui',
    '@vueuse/nuxt',
    '@nuxt/hints',
    '@nuxt/image',
    '@nuxtjs/google-fonts'
  ],

  // Rendered as an SPA: authentication uses session cookies and the app has no
  // public, indexable content.
  ssr: false,

  devtools: {
    enabled: true
  },

  css: ['~/assets/css/main.css'],

  runtimeConfig: {
    public: {
      apiBase: '/api/v1'
    }
  },

  // The frontend talks to the backend through a same-origin path. In
  // development this proxy forwards to the backend origin; in production a
  // reverse proxy serves the same path.
  routeRules: {
    '/api/**': {
      proxy: `${apiProxyTarget}/api/**`
    }
  },

  compatibilityDate: '2026-06-30',

  eslint: {
    config: {
      stylistic: {
        commaDangle: 'never',
        braceStyle: '1tbs'
      }
    }
  },

  googleFonts: {
    families: {
      'Newsreader': [400, 500, 600],
      'Hanken Grotesk': [400, 500, 600],
      'JetBrains Mono': [400]
    },
    display: 'swap'
  }
})
