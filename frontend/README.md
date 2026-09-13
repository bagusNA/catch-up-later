# Catch Up Later — Frontend

Nuxt + Vue + Nuxt UI web client for the Catch Up Later private reading library.

## Setup

```bash
pnpm install
pnpm dev
```

Runs on `http://localhost:3000`. Requests to `/api/**` are proxied to the
backend (`NUXT_API_PROXY_TARGET`, default `http://localhost:8080`); the API
client uses `runtimeConfig.public.apiBase` (`/api/v1`).

## Checks

```bash
pnpm lint
pnpm typecheck
```

## Design system

The UI uses the **Editorial Reading Room** tokens defined in
`app/assets/css/main.css` and mapped in `app/app.config.ts`. See
`specifications/11-implementation/02-decisions.md` (DEC-009, DEC-010).

## Structure

```text
app/
  assets/css/main.css   design tokens and fonts
  composables/useApi.ts the single HTTP client
  layouts/              app layouts
  pages/                routes
  types/api.ts          API envelope types
  utils/api.ts          error normalization
```
