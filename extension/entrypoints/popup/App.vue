<script lang="ts" setup>
import { browser } from 'wxt/browser';
import { onMounted, ref } from 'vue';
import type { MessageResult } from '@/lib/auth';

const loading = ref(true);
const connected = ref(false);
const email = ref<string | null>(null);

onMounted(async () => {
  const result = (await browser.runtime.sendMessage({ type: 'auth:status' })) as MessageResult;
  if (result.ok) {
    connected.value = result.connected ?? false;
    email.value = result.user?.email ?? null;
  }
  loading.value = false;
});

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
      <button type="button" class="save" disabled>
        Save this page
      </button>
      <p class="status">
        Connected as {{ email }}. Capture arrives in the next slice.
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
