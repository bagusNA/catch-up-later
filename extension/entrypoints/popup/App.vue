<script lang="ts" setup>
import { browser } from 'wxt/browser';
import { onMounted, ref } from 'vue';
import type { MessageResult } from '@/lib/auth';
import type { CaptureSummary } from '@/lib/capture-client';

const loading = ref(true);
const saving = ref(false);
const connected = ref(false);
const email = ref<string | null>(null);
const capture = ref<CaptureSummary | null>(null);
const error = ref<string | null>(null);

onMounted(async () => {
  const result = (await browser.runtime.sendMessage({ type: 'auth:status' })) as MessageResult;
  if (result.ok) {
    connected.value = result.connected ?? false;
    email.value = result.user?.email ?? null;
  }
  loading.value = false;
});

async function save() {
  if (saving.value) return;
  saving.value = true;
  error.value = null;
  capture.value = null;
  try {
    const result = (await browser.runtime.sendMessage({ type: 'capture:save' })) as MessageResult;
    if (!result.ok) {
      error.value = result.error;
      return;
    }
    capture.value = result.capture ?? null;
    if (capture.value?.status === 'FAILED') {
      error.value = capture.value.error?.message ?? 'The capture failed.';
    }
  } catch (sendError) {
    error.value = sendError instanceof Error ? sendError.message : 'The capture failed.';
  } finally {
    saving.value = false;
  }
}

function openOptions() {
  browser.runtime.openOptionsPage();
}
</script>

<template>
  <main class="popup">
    <h1>Catch Up Later</h1>
    <p class="tagline">Save something interesting now. Read it later.</p>

    <p v-if="loading" class="status">
      Checking connection…
    </p>

    <template v-else-if="connected">
      <button type="button" class="save" :disabled="saving" @click="save">
        {{ saving ? 'Saving…' : 'Save this page' }}
      </button>

      <p v-if="capture?.status === 'READY'" class="status success">
        Saved. {{ capture.warnings.length ? `${capture.warnings.length} warning(s).` : 'Ready to read.' }}
      </p>
      <p v-else-if="error" class="status error">
        {{ error }}
      </p>
      <p v-else-if="!saving" class="status">
        Connected as {{ email }}.
      </p>
    </template>

    <template v-else>
      <button type="button" class="save" @click="openOptions">
        Connect to your library
      </button>
      <p class="status">
        Not connected yet.
      </p>
    </template>

    <button type="button" class="secondary" @click="openOptions">
      Settings
    </button>
  </main>
</template>
