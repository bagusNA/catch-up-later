import type { ApiErrorEnvelope } from '~/types/api'

/**
 * The single HTTP client used by the frontend.
 *
 * - Resolves against `runtimeConfig.public.apiBase` (same-origin, proxied to
 *   the backend during development).
 * - Sends cookies so session-authenticated web requests work.
 * - Converts the backend error envelope into an `ApiRequestError`.
 *
 * Components must go through this client rather than calling `$fetch`
 * directly.
 */
export function useApi() {
  const config = useRuntimeConfig()

  return $fetch.create({
    baseURL: config.public.apiBase,
    credentials: 'include',
    onResponseError({ response }) {
      const body = response._data as ApiErrorEnvelope | undefined
      if (body?.error?.code) {
        throw new ApiRequestError(body.error, response.status)
      }
      throw new ApiRequestError({}, response.status)
    }
  })
}
