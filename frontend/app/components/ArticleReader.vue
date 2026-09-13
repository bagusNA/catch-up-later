<script setup lang="ts">
/**
 * Isolated article surface.
 *
 * Sanitized backend HTML is rendered inside a sandboxed iframe. The sandbox has
 * `allow-same-origin` (so the parent can size the frame and same-origin asset
 * requests carry the session cookie) but no `allow-scripts`, so no script in
 * the stored artifact can ever execute.
 */
const props = defineProps<{ html: string }>()

const frame = ref<HTMLIFrameElement>()
const height = ref('24rem')

const srcDoc = computed(() => `<!doctype html>
<html>
  <head>
    <meta charset="utf-8" />
    <style>
      :root { color-scheme: light dark; }
      body {
        margin: 0;
        padding: 0;
        font-family: 'Newsreader', Georgia, serif;
        font-size: 1.125rem;
        line-height: 1.75;
        color: #23211e;
        background: transparent;
        overflow-wrap: break-word;
      }
      h1, h2, h3, h4, h5, h6 { line-height: 1.25; margin: 1.8em 0 0.6em; font-weight: 600; }
      h1 { font-size: 1.9em; }
      h2 { font-size: 1.5em; }
      h3 { font-size: 1.25em; }
      p { margin: 0 0 1.2em; }
      a { color: #a8432d; text-decoration: underline; text-underline-offset: 2px; }
      img { max-width: 100%; height: auto; border-radius: 0.375rem; margin: 1.5em 0; }
      figure { margin: 1.5em 0; }
      figcaption { font-size: 0.875em; color: #57534e; }
      blockquote {
        margin: 1.5em 0;
        padding-left: 1.25em;
        border-left: 3px solid #e3dfd5;
        color: #57534e;
        font-style: italic;
      }
      pre {
        padding: 1em;
        overflow-x: auto;
        background: #f0ede4;
        border-radius: 0.375rem;
        font-family: 'JetBrains Mono', ui-monospace, monospace;
        font-size: 0.875em;
      }
      code { font-family: 'JetBrains Mono', ui-monospace, monospace; font-size: 0.9em; }
      table { width: 100%; border-collapse: collapse; margin: 1.5em 0; font-size: 0.95em; }
      th, td { border: 1px solid #e3dfd5; padding: 0.5em 0.75em; text-align: left; }
      hr { border: none; border-top: 1px solid #e3dfd5; margin: 2em 0; }
      @media (prefers-color-scheme: dark) {
        body { color: #f2f1ec; }
        blockquote { border-color: #3d3a36; color: #b3aea6; }
        pre { background: #2b2925; }
        th, td { border-color: #3d3a36; }
        hr { border-color: #3d3a36; }
      }
    </style>
  </head>
  <body>${props.html}</body>
</html>`)

function resize(): void {
  const body = frame.value?.contentDocument?.body
  if (!body) return
  height.value = `${Math.max(body.scrollHeight + 48, 320)}px`
}

function onLoad(): void {
  resize()
  const document = frame.value?.contentDocument
  if (!document) return
  document.querySelectorAll('img').forEach((image) => {
    image.addEventListener('load', resize)
  })
  if (typeof ResizeObserver !== 'undefined') {
    const observer = new ResizeObserver(resize)
    observer.observe(document.body)
  }
}

onMounted(() => window.addEventListener('resize', resize))
onBeforeUnmount(() => window.removeEventListener('resize', resize))
</script>

<template>
  <iframe
    ref="frame"
    :srcdoc="srcDoc"
    sandbox="allow-same-origin"
    title="Article content"
    class="w-full border-0"
    :style="{ height }"
    @load="onLoad"
  />
</template>
