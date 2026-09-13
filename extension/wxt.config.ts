import { defineConfig } from 'wxt';

// Catch Up Later browser extension.
//
// The extension performs user-initiated capture only. It requests `activeTab`
// and `scripting` so it can read the current page after the user explicitly
// clicks Save. Downloading article images (which are frequently hosted on a
// different origin/CDN) needs host access, so `<all_urls>` is declared as an
// *optional* permission and requested from the popup on first save. If the user
// declines, same-origin images still work and others degrade to warnings.
export default defineConfig({
  modules: ['@wxt-dev/module-vue'],
  manifest: {
    name: 'Catch Up Later',
    description: 'Save web articles and PDFs to your private reading library.',
    version: '0.1.0',
    permissions: ['storage', 'activeTab', 'scripting', 'tabs'],
    optional_host_permissions: ['<all_urls>'],
  },
});
