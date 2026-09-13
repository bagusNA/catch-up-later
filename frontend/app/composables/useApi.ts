import type { ApiErrorEnvelope, AuthUser } from '~/types/api'

const SAFE_METHODS = ['GET', 'HEAD', 'OPTIONS']

/**
 * The single HTTP client used by the frontend.
 *
 * - Resolves against `runtimeConfig.public.apiBase` (same-origin, proxied to
 *   the backend during development).
 * - Sends cookies so session-authenticated web requests work.
 * - Adds the `X-XSRF-TOKEN` header to state-changing requests.
 * - Converts the backend error envelope into an `ApiRequestError` and clears
 *   the session on an unexpected 401.
 *
 * Components must go through this client rather than calling `$fetch`
 * directly.
 */
export function useApi() {
  const config = useRuntimeConfig()

  return $fetch.create({
    baseURL: config.public.apiBase,
    credentials: 'include',
    onRequest({ options }) {
      const method = (options.method ?? 'GET').toString().toUpperCase()
      const token = getCsrfToken()
      if (token && !SAFE_METHODS.includes(method)) {
        options.headers = new Headers(options.headers as HeadersInit)
        options.headers.set('X-XSRF-TOKEN', token)
      }
    },
    onResponseError({ request, response }) {
      const body = response._data as ApiErrorEnvelope | undefined

      if (response.status === 401 && import.meta.client) {
        const url = request.toString()
        const isAuthProbe = url.includes('/auth/me') || url.includes('/auth/login') || url.includes('/auth/token')
        if (!isAuthProbe) {
          useState<AuthUser | null>('auth.user').value = null
          if (!window.location.pathname.startsWith('/login')) {
            navigateTo('/login')
          }
        }
      }

      if (body?.error?.code) {
        throw new ApiRequestError(body.error, response.status)
      }
      throw new ApiRequestError({}, response.status)
    }
  })
}
