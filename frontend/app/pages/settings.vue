<script setup lang="ts">
const { user, updateProfile, changePassword } = useAuth()

const displayName = ref(user.value?.displayName ?? '')
watch(user, (value) => {
  displayName.value = value?.displayName ?? ''
})

const savingProfile = ref(false)
const profileMessage = ref<string | null>(null)
const profileError = ref<string | null>(null)

async function saveProfile() {
  profileMessage.value = null
  profileError.value = null
  savingProfile.value = true
  try {
    await updateProfile(displayName.value || null)
    profileMessage.value = 'Profile updated.'
  } catch (cause) {
    profileError.value = cause instanceof Error ? cause.message : 'Update failed.'
  } finally {
    savingProfile.value = false
  }
}

const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const savingPassword = ref(false)
const passwordMessage = ref<string | null>(null)
const passwordError = ref<string | null>(null)

async function savePassword() {
  passwordMessage.value = null
  passwordError.value = null

  if (newPassword.value !== confirmPassword.value) {
    passwordError.value = 'New passwords do not match.'
    return
  }

  savingPassword.value = true
  try {
    await changePassword(currentPassword.value, newPassword.value)
    currentPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
    passwordMessage.value = 'Password changed. The extension will need to sign in again.'
  } catch (cause) {
    passwordError.value = cause instanceof Error ? cause.message : 'Password change failed.'
  } finally {
    savingPassword.value = false
  }
}
</script>

<template>
  <section class="mx-auto flex max-w-2xl flex-col gap-10">
    <div>
      <h1 class="font-serif text-3xl">
        Settings
      </h1>
      <p class="mt-2 text-on-surface-variant">
        Account and reader preferences.
      </p>
    </div>

    <UCard>
      <template #header>
        <h2 class="font-serif text-xl">
          Profile
        </h2>
      </template>

      <form class="flex flex-col gap-4" @submit.prevent="saveProfile">
        <UFormField label="Email">
          <UInput :model-value="user?.email" disabled class="w-full" />
        </UFormField>

        <UFormField label="Display name">
          <UInput v-model="displayName" class="w-full" />
        </UFormField>

        <p v-if="profileError" class="text-sm text-rust-600">
          {{ profileError }}
        </p>
        <p v-if="profileMessage" class="text-sm text-sage-600">
          {{ profileMessage }}
        </p>

        <div>
          <UButton type="submit" :loading="savingProfile">
            Save changes
          </UButton>
        </div>
      </form>
    </UCard>

    <UCard>
      <template #header>
        <h2 class="font-serif text-xl">
          Password
        </h2>
      </template>

      <form class="flex flex-col gap-4" @submit.prevent="savePassword">
        <UFormField label="Current password">
          <UInput
            v-model="currentPassword"
            type="password"
            autocomplete="current-password"
            required
            class="w-full"
          />
        </UFormField>

        <UFormField label="New password" description="At least 12 characters.">
          <UInput
            v-model="newPassword"
            type="password"
            autocomplete="new-password"
            required
            class="w-full"
          />
        </UFormField>

        <UFormField label="Confirm new password">
          <UInput
            v-model="confirmPassword"
            type="password"
            autocomplete="new-password"
            required
            class="w-full"
          />
        </UFormField>

        <p v-if="passwordError" class="text-sm text-rust-600">
          {{ passwordError }}
        </p>
        <p v-if="passwordMessage" class="text-sm text-sage-600">
          {{ passwordMessage }}
        </p>

        <div>
          <UButton type="submit" :loading="savingPassword">
            Change password
          </UButton>
        </div>
      </form>
    </UCard>

    <UCard>
      <template #header>
        <h2 class="font-serif text-xl">
          Browser extension
        </h2>
      </template>
      <p class="text-sm text-on-surface-variant">
        Install the Catch Up Later extension, open its options page, and sign in
        with this account to start saving content.
      </p>
    </UCard>
  </section>
</template>
