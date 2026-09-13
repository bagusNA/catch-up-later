import { defineUnlistedScript } from 'wxt/utils/define-unlisted-script'
import { capturePage } from '@/lib/capture/pipeline'

/**
 * Page-context capture script.
 *
 * Injected into the active tab with `browser.scripting.executeScript` after the
 * user clicks Save. It runs in the isolated world, reads the live DOM, captures
 * assets, and returns a base64-encoded capture package to the service worker.
 */
export default defineUnlistedScript(() => capturePage())
