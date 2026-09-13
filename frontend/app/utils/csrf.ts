/**
 * In-memory CSRF token cache.
 *
 * The token is also written to the `XSRF-TOKEN` cookie by the backend; caching
 * it here avoids a race between the cookie write and the first state-changing
 * request.
 */
let csrfToken: string | null = null

export function setCsrfToken(token: string | null): void {
  csrfToken = token
}

export function getCsrfToken(): string | null {
  return csrfToken
}
