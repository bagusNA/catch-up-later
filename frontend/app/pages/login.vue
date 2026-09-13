<script setup lang="ts">
const { login } = useAuth()

const email = ref('')
const password = ref('')
const error = ref<string | null>(null)
const submitting = ref(false)

async function submit() {
  error.value = null
  submitting.value = true
  try {
    await login({ email: email.value, password: password.value })
    await navigateTo('/library')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Sign in failed.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="flex min-h-screen items-center justify-center px-6 py-16">
    <div class="w-full max-w-sm">
      <p class="font-mono text-xs uppercase tracking-[0.18em] text-on-surface-variant">
        Catch Up Later
      </p>
      <h1 class="mt-3 font-serif text-3xl">
        Sign in
      </h1>
      <p class="mt-2 text-sm text-on-surface-variant">
        Your private reading library.
      </p>

      <form class="mt-8 flex flex-col gap-4" @submit.prevent="submit">
        <UFormField label="Email">
          <UInput
            v-model="email"
            type="email"
            autocomplete="email"
            required
            class="w-full"
          />
        </UFormField>

        <UFormField label="Password">
          <UInput
            v-model="password"
            type="password"
            autocomplete="current-password"
            required
            class="w-full"
          />
        </UFormField>

        <p v-if="error" class="text-sm text-rust-600">
          {{ error }}
        </p>

        <UButton type="submit" :loading="submitting" block>
          Sign in
        </UButton>
      </form>

      <p class="mt-6 text-sm text-on-surface-variant">
        Connecting the browser extension? Use the same credentials in its options page.
      </p>
    </div>
  </main>
</template>
