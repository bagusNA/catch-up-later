<script lang="ts" setup>
import { ref } from 'vue';

/**
 * Popup placeholder for the foundation slice.
 *
 * The save flow (adapter selection, packaging, upload and status) is delivered
 * in the core capture slice. This placeholder establishes the shell and the
 * product identity so the extension builds and loads cleanly.
 */
const saving = ref(false);
const message = ref('Capture is coming in the next slice.');
</script>

<template>
  <main class="popup">
    <h1>Catch Up Later</h1>
    <p class="tagline">Save something interesting now. Read it later.</p>
    <button type="button" class="save" :disabled="saving">
      {{ saving ? 'Saving…' : 'Save this page' }}
    </button>
    <p class="status">{{ message }}</p>
  </main>
</template>
