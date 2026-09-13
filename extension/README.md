# Catch Up Later — Browser Extension

WebExtension (WXT + Vue 3) that captures the page the user is reading and sends
it to their self-hosted Catch Up Later backend.

## Development

```bash
pnpm install
pnpm dev            # Chrome
pnpm dev:firefox    # Firefox
pnpm compile        # type-check
pnpm build          # production build
```

## Scope

The extension only captures on explicit user action. It requests `activeTab` and
`scripting` so it can read the current page after the user clicks Save, instead
of holding broad host permissions.

## Status

Foundation slice: the extension builds with product metadata and a placeholder
popup. Adapters, packaging, upload, and the retry queue arrive in later slices.
