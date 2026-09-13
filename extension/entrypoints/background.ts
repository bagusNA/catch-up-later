/**
 * Background service worker.
 *
 * Owns all backend communication (upload, retry queue, authentication) in
 * later slices. Content scripts and the popup never talk to the backend
 * directly.
 */
export default defineBackground(() => {
  // Intentionally minimal in the foundation slice.
});
