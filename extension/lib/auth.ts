import { storage } from 'wxt/utils/storage'

/**
 * Extension-side authentication.
 *
 * Tokens live in `browser.storage.local` (never in page content) and refresh
 * happens in the background service worker. All backend calls go through
 * [authenticatedFetch] so the access token is attached consistently.
 */

export interface ExtensionUser {
  id: number
  email: string
  displayName: string | null
}

export interface StoredTokens {
  accessToken: string
  refreshToken: string
  /** Epoch milliseconds. */
  accessExpiresAt: number
  /** Epoch milliseconds. */
  refreshExpiresAt: number
  user: ExtensionUser
}

export interface ExtensionSettings {
  backendUrl: string
}

export interface AuthStatus {
  connected: boolean
  user: ExtensionUser | null
  backendUrl: string
}

export type ExtensionMessage =
  | { type: 'auth:connect'; backendUrl: string; email: string; password: string }
  | { type: 'auth:status' }
  | { type: 'auth:logout' }
  | { type: 'api:me' }

export type MessageResult =
  | { ok: true, connected?: boolean, user?: ExtensionUser | null, backendUrl?: string, data?: unknown }
  | { ok: false, error: string }

const settingsItem = storage.defineItem<ExtensionSettings>('local:settings', {
  fallback: { backendUrl: '' }
})

const tokensItem = storage.defineItem<StoredTokens | null>('local:tokens', {
  fallback: null
})

/** Refresh the access token 30 seconds before it expires. */
const REFRESH_SKEW_MS = 30_000

export async function getSettings(): Promise<ExtensionSettings> {
  return settingsItem.getValue()
}

export async function getTokens(): Promise<StoredTokens | null> {
  return tokensItem.getValue()
}

export async function getStatus(): Promise<AuthStatus> {
  const [tokens, settings] = await Promise.all([tokensItem.getValue(), settingsItem.getValue()])
  return {
    connected: tokens !== null,
    user: tokens?.user ?? null,
    backendUrl: settings.backendUrl
  }
}

export function normalizeBackendUrl(url: string): string {
  return url.trim().replace(/\/+$/, '').replace(/\/api\/v1$/, '')
}

function toStoredTokens(base: string, body: TokenResponseBody): StoredTokens {
  const now = Date.now()
  return {
    accessToken: body.accessToken,
    refreshToken: body.refreshToken,
    accessExpiresAt: now + body.expiresIn * 1000,
    refreshExpiresAt: now + body.refreshExpiresIn * 1000,
    user: {
      id: body.user.id,
      email: body.user.email,
      displayName: body.user.displayName
    }
  }
}

export async function connect(backendUrl: string, email: string, password: string): Promise<StoredTokens> {
  const base = normalizeBackendUrl(backendUrl)
  if (!base) {
    throw new Error('Enter the URL of your Catch Up Later backend.')
  }

  const response = await fetch(`${base}/api/v1/auth/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: email.trim(), password })
  })

  if (!response.ok) {
    throw new Error(await readErrorMessage(response))
  }

  const tokens = toStoredTokens(base, await response.json() as TokenResponseBody)
  await settingsItem.setValue({ backendUrl: base })
  await tokensItem.setValue(tokens)
  return tokens
}

export async function refreshTokens(): Promise<StoredTokens | null> {
  const [tokens, settings] = await Promise.all([tokensItem.getValue(), settingsItem.getValue()])
  if (!tokens || !settings.backendUrl) {
    return null
  }

  try {
    const response = await fetch(`${settings.backendUrl}/api/v1/auth/token/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: tokens.refreshToken })
    })
    if (!response.ok) {
      await tokensItem.removeValue()
      return null
    }
    const refreshed = toStoredTokens(settings.backendUrl, await response.json() as TokenResponseBody)
    await tokensItem.setValue(refreshed)
    return refreshed
  } catch {
    // Network failure: keep the existing tokens and let the next attempt retry.
    return null
  }
}

export async function logout(): Promise<void> {
  const [tokens, settings] = await Promise.all([tokensItem.getValue(), settingsItem.getValue()])
  if (tokens && settings.backendUrl) {
    try {
      await fetch(`${settings.backendUrl}/api/v1/auth/token/revoke`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: tokens.refreshToken })
      })
    } catch {
      // Best-effort revocation.
    }
  }
  await tokensItem.removeValue()
}

async function accessToken(): Promise<string | null> {
  const tokens = await tokensItem.getValue()
  if (!tokens) {
    return null
  }
  if (tokens.accessExpiresAt - Date.now() > REFRESH_SKEW_MS) {
    return tokens.accessToken
  }
  const refreshed = await refreshTokens()
  return refreshed?.accessToken ?? null
}

/**
 * Fetches a backend path with the current access token, refreshing once on a
 * 401 before giving up.
 */
export async function authenticatedFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const settings = await settingsItem.getValue()
  if (!settings.backendUrl) {
    throw new Error('The extension is not connected to a backend.')
  }

  const request = async (token: string | null): Promise<Response> => {
    const headers = new Headers(init.headers)
    if (token) {
      headers.set('Authorization', `Bearer ${token}`)
    }
    return fetch(`${settings.backendUrl}${path}`, { ...init, headers })
  }

  let response = await request(await accessToken())
  if (response.status === 401) {
    const refreshed = await refreshTokens()
    if (refreshed) {
      response = await request(refreshed.accessToken)
    }
  }
  return response
}

interface TokenResponseBody {
  accessToken: string
  expiresIn: number
  refreshToken: string
  refreshExpiresIn: number
  user: ExtensionUser
}

async function readErrorMessage(response: Response): Promise<string> {
  try {
    const body = await response.json() as { error?: { message?: string } }
    if (body.error?.message) {
      return body.error.message
    }
  } catch {
    // Fall through to the generic message.
  }
  return `Request failed (${response.status}).`
}
