import { browser } from 'wxt/browser'
import {
  authenticatedFetch,
  connect,
  getStatus,
  logout,
  type ExtensionMessage,
  type MessageResult
} from '@/lib/auth'
import { captureActiveTab } from '@/lib/capture-client'

/**
 * Background service worker.
 *
 * Owns all backend communication: the popup and options page only exchange
 * messages with this worker, so tokens never leak into a page context.
 */
export default defineBackground(() => {
  browser.runtime.onMessage.addListener((message, _sender, sendResponse) => {
    handle(message as ExtensionMessage)
      .then(sendResponse)
      .catch((error: unknown) => {
        sendResponse({
          ok: false,
          error: error instanceof Error ? error.message : 'Unknown error'
        } satisfies MessageResult)
      })
    // Keep the message channel open for the async response.
    return true
  })
})

async function handle(message: ExtensionMessage): Promise<MessageResult> {
  switch (message.type) {
    case 'auth:connect': {
      const tokens = await connect(message.backendUrl, message.email, message.password)
      return { ok: true, connected: true, user: tokens.user }
    }
    case 'auth:status': {
      const status = await getStatus()
      return { ok: true, connected: status.connected, user: status.user, backendUrl: status.backendUrl }
    }
    case 'auth:logout': {
      await logout()
      return { ok: true, connected: false, user: null }
    }
    case 'api:me': {
      const response = await authenticatedFetch('/api/v1/auth/me')
      if (!response.ok) {
        return { ok: false, error: `Request failed (${response.status}).` }
      }
      return { ok: true, data: await response.json() }
    }
    case 'capture:save': {
      const capture = await captureActiveTab()
      return { ok: true, capture }
    }
    default: {
      return { ok: false, error: 'Unsupported message.' }
    }
  }
}
