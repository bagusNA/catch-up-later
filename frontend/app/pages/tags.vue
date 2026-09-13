<script setup lang="ts">
import type { TagResponse } from '~/types/api'

const api = useApi()
const tags = ref<TagResponse[]>([])
const pending = ref(true)
const error = ref<string | null>(null)
const newTag = ref('')
const busy = ref(false)

async function load() {
  pending.value = true
  error.value = null
  try {
    tags.value = await api<TagResponse[]>('/tags')
  } catch (exception) {
    error.value = toApiRequestError(exception).message
  } finally {
    pending.value = false
  }
}

async function create() {
  const name = newTag.value.trim()
  if (!name) return
  busy.value = true
  error.value = null
  try {
    await api('/tags', { method: 'POST', body: { name } })
    newTag.value = ''
    await load()
  } catch (exception) {
    error.value = toApiRequestError(exception).message
  } finally {
    busy.value = false
  }
}

async function remove(tag: TagResponse) {
  busy.value = true
  error.value = null
  try {
    await api(`/tags/${tag.id}`, { method: 'DELETE' })
    await load()
  } catch (exception) {
    error.value = toApiRequestError(exception).message
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="mx-auto max-w-3xl">
    <h1 class="font-serif text-3xl">
      Tags
    </h1>
    <p class="mt-2 text-on-surface-variant">
      Organize saved content with tags. Names are case-insensitive and unique.
    </p>

    <form class="mt-8 flex gap-2" @submit.prevent="create">
      <input
        v-model="newTag"
        type="text"
        placeholder="New tag name…"
        maxlength="100"
        class="flex-1 rounded-md border border-outline bg-surface px-3 py-2 text-sm focus:border-cul-400 focus:outline-none"
      >
      <UButton
        type="submit"
        color="primary"
        variant="soft"
        :loading="busy"
        :disabled="!newTag.trim()"
      >
        Create tag
      </UButton>
    </form>

    <UAlert
      v-if="error"
      class="mt-4"
      color="error"
      variant="subtle"
      title="Tag operation failed"
      :description="error"
    />

    <p v-if="pending" class="mt-8 text-sm text-on-surface-variant">
      Loading tags…
    </p>

    <LibraryEmptyState
      v-else-if="!tags.length"
      class="mt-8"
      icon="i-lucide-tag"
      title="No tags yet"
      description="Create your first tag above, then assign it from an item's detail page."
    />

    <ul v-else class="mt-8 divide-y divide-outline/70">
      <li v-for="tag in tags" :key="tag.id" class="flex items-center justify-between gap-4 py-3">
        <NuxtLink
          :to="`/library?tag=${encodeURIComponent(tag.name)}`"
          class="min-w-0 flex-1 hover:text-cul-700"
        >
          <span class="text-sm font-medium">#{{ tag.name }}</span>
          <span class="ml-2 text-xs text-on-surface-variant">
            {{ tag.itemCount }} {{ tag.itemCount === 1 ? 'item' : 'items' }}
          </span>
        </NuxtLink>
        <UButton
          color="neutral"
          variant="ghost"
          size="sm"
          icon="i-lucide-trash-2"
          :disabled="busy"
          :aria-label="`Delete tag ${tag.name}`"
          @click="remove(tag)"
        />
      </li>
    </ul>
  </section>
</template>
