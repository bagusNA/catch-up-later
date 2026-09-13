<script setup lang="ts">
import type { TagResponse } from '~/types/api'

const props = defineProps<{
  contentItemId: number
  assigned: string[]
}>()

const emit = defineEmits<{ updated: [string[]] }>()

const api = useApi()
const tags = ref<TagResponse[]>([])
const selected = ref<string[]>([...props.assigned])
const newTag = ref('')
const busy = ref(false)
const error = ref<string | null>(null)

watch(() => props.assigned, (value) => {
  selected.value = [...value]
})

async function loadTags() {
  try {
    tags.value = await api<TagResponse[]>('/tags')
  } catch {
    tags.value = []
  }
}

async function save() {
  busy.value = true
  error.value = null
  try {
    const ids = tags.value.filter(tag => selected.value.includes(tag.name)).map(tag => tag.id)
    const result = await api<{ tags: string[] }>(`/content-items/${props.contentItemId}/tags`, {
      method: 'PUT',
      body: { tagIds: ids }
    })
    selected.value = result.tags
    emit('updated', result.tags)
  } catch (exception) {
    error.value = toApiRequestError(exception).message
  } finally {
    busy.value = false
  }
}

function toggle(name: string) {
  selected.value = selected.value.includes(name)
    ? selected.value.filter(tag => tag !== name)
    : [...selected.value, name]
  save()
}

async function createAndAssign() {
  const name = newTag.value.trim()
  if (!name) return
  busy.value = true
  error.value = null
  try {
    const created = await api<TagResponse>('/tags', { method: 'POST', body: { name } })
    newTag.value = ''
    await loadTags()
    selected.value = [...selected.value, created.name]
    await save()
  } catch (exception) {
    error.value = toApiRequestError(exception).message
    busy.value = false
  }
}

onMounted(async () => {
  await loadTags()
})
</script>

<template>
  <div>
    <div class="flex flex-wrap gap-2">
      <button
        v-for="tag in tags"
        :key="tag.id"
        type="button"
        class="rounded-full border px-2.5 py-1 text-xs transition-colors"
        :class="selected.includes(tag.name)
          ? 'border-cul-300 bg-cul-50 text-cul-700'
          : 'border-outline text-on-surface-variant hover:border-cul-300'"
        :aria-pressed="selected.includes(tag.name)"
        :disabled="busy"
        @click="toggle(tag.name)"
      >
        #{{ tag.name }}
      </button>
      <span v-if="!tags.length" class="text-xs text-on-surface-variant">
        No tags yet.
      </span>
    </div>

    <form class="mt-3 flex gap-2" @submit.prevent="createAndAssign">
      <input
        v-model="newTag"
        type="text"
        placeholder="Create a tag…"
        maxlength="100"
        class="flex-1 rounded-md border border-outline bg-surface px-2.5 py-1.5 text-sm focus:border-cul-400 focus:outline-none"
      >
      <UButton
        type="submit"
        color="neutral"
        variant="soft"
        size="sm"
        :disabled="!newTag.trim() || busy"
      >
        Add
      </UButton>
    </form>

    <p v-if="error" class="mt-2 text-xs text-rust-600">
      {{ error }}
    </p>
  </div>
</template>
