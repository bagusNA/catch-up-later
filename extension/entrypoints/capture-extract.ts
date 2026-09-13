import { defineUnlistedScript } from 'wxt/utils/define-unlisted-script'
import { extractPage } from '@/lib/capture/pipeline'

/**
 * Page-context capture script.
 *
 * Injected into the active tab with `browser.scripting.executeScript` after the
 * user clicks Save. It runs in the isolated world, reads the live DOM, rewrites
 * image references to package-local keys, and returns a JSON-serializable page
 * result. The service worker downloads asset bytes and builds the archive.
 */
export default defineUnlistedScript(() => extractPage())
