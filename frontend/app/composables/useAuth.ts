import type { AuthUser, SetupStatus } from '~/types/api'

export interface LoginCredentials {
  email: string
  password: string
}

export interface SetupPayload extends LoginCredentials {
  displayName?: string
}

/**
 * Authentication and account state for the web client.
 *
 * Session cookies are the source of truth; this composable mirrors the current
 * user into Nuxt state so navigation and the app shell can react to it.
 */
export function useAuth() {
  const api = useApi()
  const user = useState<AuthUser | null>('auth.user', () => null)
  const setupRequired = useState<boolean | null>('auth.setupRequired', () => null)
  const initialized = useState<boolean>('auth.initialized', () => false)

  async function ensureCsrf(): Promise<string> {
    const existing = getCsrfToken()
    if (existing) {
      return existing
    }
    const response = await api<{ token: string }>('/auth/csrf')
    setCsrfToken(response.token)
    return response.token
  }

  async function fetchSetupStatus(): Promise<boolean> {
    const response = await api<SetupStatus>('/setup/status')
    setupRequired.value = response.required
    return response.required
  }

  async function fetchMe(): Promise<AuthUser | null> {
    try {
      user.value = await api<AuthUser>('/auth/me')
    } catch (error) {
      if (error instanceof ApiRequestError && error.status === 401) {
        user.value = null
        return null
      }
      throw error
    }
    return user.value
  }

  async function login(credentials: LoginCredentials): Promise<AuthUser> {
    await ensureCsrf()
    user.value = await api<AuthUser>('/auth/login', { method: 'POST', body: credentials })
    setupRequired.value = false
    return user.value
  }

  async function completeSetup(payload: SetupPayload): Promise<AuthUser> {
    await ensureCsrf()
    const created = await api<AuthUser>('/setup', { method: 'POST', body: payload })
    setupRequired.value = false
    return created
  }

  async function logout(): Promise<void> {
    try {
      await ensureCsrf()
      await api('/auth/logout', { method: 'POST' })
    } catch {
      // Logout is best-effort; local state is cleared regardless.
    }
    user.value = null
    setCsrfToken(null)
    await navigateTo('/login')
  }

  async function updateProfile(displayName: string | null): Promise<AuthUser> {
    user.value = await api<AuthUser>('/users/me', { method: 'PATCH', body: { displayName } })
    return user.value
  }

  async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
    await ensureCsrf()
    await api('/users/me/password', { method: 'POST', body: { currentPassword, newPassword } })
  }

  async function initialize(): Promise<void> {
    if (initialized.value) {
      return
    }
    await fetchSetupStatus()
    if (!setupRequired.value) {
      await fetchMe()
    }
    initialized.value = true
  }

  return {
    user,
    setupRequired,
    initialized,
    initialize,
    ensureCsrf,
    fetchSetupStatus,
    fetchMe,
    login,
    completeSetup,
    logout,
    updateProfile,
    changePassword
  }
}
