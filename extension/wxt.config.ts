import { defineConfig } from 'wxt';

// Catch Up Later browser extension.
//
// The extension performs user-initiated capture only. Downloading article
// images (which are frequently hosted on a different origin/CDN) needs host
// access, so `<all_urls>` is declared as a host permission. Capture itself is
// still only ever triggered by the user; the background worker uses the
// permission to fetch the images belonging to the page the user chose to save.
export default defineConfig({
  modules: ['@wxt-dev/module-vue'],
  manifest: {
    name: 'Catch Up Later',
    description: 'Save web articles and PDFs to your private reading library.',
    version: '0.1.0',
    permissions: ['storage', 'activeTab', 'scripting', 'tabs'],
    host_permissions: ['<all_urls>'],
  },
});
