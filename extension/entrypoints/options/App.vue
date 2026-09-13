<script lang="ts" setup>
import { browser } from 'wxt/browser';
import { onMounted, ref } from 'vue';
import type { MessageResult } from '@/lib/auth';

const backendUrl = ref('http://localhost:8080');
const email = ref('');
const password = ref('');

const loading = ref(true);
const submitting = ref(false);
const connected = ref(false);
const connectedEmail = ref<string | null>(null);
const error = ref<string | null>(null);
const notice = ref<string | null>(null);

async function refreshStatus() {
  const result = (await browser.runtime.sendMessage({ type: 'auth:status' })) as MessageResult;
  if (result.ok) {
    connected.value = result.connected ?? false;
    connectedEmail.value = result.user?.email ?? null;
    if (result.backendUrl) {
      backendUrl.value = result.backendUrl;
    }
  }
  loading.value = false;
}

onMounted(refreshStatus);

async function connect() {
  error.value = null;
  notice.value = null;
  submitting.value = true;
  try {
    const result = (await browser.runtime.sendMessage({
      type: 'auth:connect',
      backendUrl: backendUrl.value,
      email: email.value,
      password: password.value,
    })) as MessageResult;
    if (!result.ok) {
      throw new Error(result.error);
    }
    connected.value = true;
    connectedEmail.value = result.user?.email ?? email.value;
    password.value = '';
    notice.value = 'Connected.';
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Connection failed.';
  } finally {
    submitting.value = false;
  }
}

async function disconnect() {
  await browser.runtime.sendMessage({ type: 'auth:logout' });
  connected.value = false;
  connectedEmail.value = null;
  notice.value = 'Disconnected.';
}
</script>

<template>
  <div class="page">
    <div>
      <h1>Catch Up Later</h1>
      <p>Connect this extension to your self-hosted library.</p>
    </div>

    <div class="card">
      <h2>Connection</h2>

      <p v-if="loading" class="status">
        Checking connection…
      </p>

      <template v-else-if="connected">
        <p class="status">
          Connected as <strong>{{ connectedEmail }}</strong>
        </p>
        <div class="actions" style="margin-top: 0.9rem">
          <button class="quiet" type="button" @click="disconnect">
            Disconnect
          </button>
        </div>
      </template>

      <form v-else @submit.prevent="connect">
        <div class="field">
          <label for="backend">Backend URL</label>
          <input id="backend" v-model="backendUrl" type="url" placeholder="https://library.example.com" required />
        </div>

        <div class="field">
          <label for="email">Email</label>
          <input id="email" v-model="email" type="email" autocomplete="email" required />
        </div>

        <div class="field">
          <label for="password">Password</label>
          <input id="password" v-model="password" type="password" autocomplete="current-password" required />
        </div>

        <div class="actions">
          <button class="primary" type="submit" :disabled="submitting">
            {{ submitting ? 'Connecting…' : 'Connect' }}
          </button>
        </div>
      </form>

      <p v-if="error" class="status error" style="margin-top: 0.9rem">{{ error }}</p>
      <p v-else-if="notice" class="status notice" style="margin-top: 0.9rem">{{ notice }}</p>
    </div>
  </div>
</template>
