import { defineConfig } from 'wxt';

// Catch Up Later browser extension.
//
// The extension performs user-initiated capture only. It requests `activeTab`
// and `scripting` so it can read the current page after the user explicitly
// clicks Save, rather than holding broad host permissions at rest.
export default defineConfig({
  modules: ['@wxt-dev/module-vue'],
  manifest: {
    name: 'Catch Up Later',
    description: 'Save web articles and PDFs to your private reading library.',
    version: '0.1.0',
    permissions: ['storage', 'activeTab', 'scripting', 'tabs'],
  },
});
