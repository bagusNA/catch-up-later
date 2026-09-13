<script setup lang="ts">
const { completeSetup, login } = useAuth()

const email = ref('')
const displayName = ref('')
const password = ref('')
const confirmPassword = ref('')
const error = ref<string | null>(null)
const submitting = ref(false)

async function submit() {
  error.value = null

  if (password.value !== confirmPassword.value) {
    error.value = 'Passwords do not match.'
    return
  }

  submitting.value = true
  try {
    await completeSetup({
      email: email.value,
      password: password.value,
      displayName: displayName.value || undefined
    })
    await login({ email: email.value, password: password.value })
    await navigateTo('/library')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Setup failed.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="flex min-h-screen items-center justify-center px-6 py-16">
    <div class="w-full max-w-md">
      <p class="font-mono text-xs uppercase tracking-[0.18em] text-on-surface-variant">
        Welcome
      </p>
      <h1 class="mt-3 font-serif text-3xl">
        Create your account
      </h1>
      <p class="mt-2 text-sm text-on-surface-variant">
        This is the first account on this instance. It becomes the owner and
        administrator, and public registration stays closed afterwards.
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

        <UFormField label="Display name" description="Optional.">
          <UInput
            v-model="displayName"
            autocomplete="name"
            class="w-full"
          />
        </UFormField>

        <UFormField label="Password" description="At least 12 characters.">
          <UInput
            v-model="password"
            type="password"
            autocomplete="new-password"
            required
            class="w-full"
          />
        </UFormField>

        <UFormField label="Confirm password">
          <UInput
            v-model="confirmPassword"
            type="password"
            autocomplete="new-password"
            required
            class="w-full"
          />
        </UFormField>

        <p v-if="error" class="text-sm text-rust-600">
          {{ error }}
        </p>

        <UButton type="submit" :loading="submitting" block>
          Create account
        </UButton>
      </form>
    </div>
  </main>
</template>
