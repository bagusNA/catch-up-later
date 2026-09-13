<script setup lang="ts">
import type { ContentItemDetailResponse, ReadingStatus } from '~/types/api'
import { CAPTURE_STATUS_LABELS, READING_STATUS_LABELS } from '~/utils/library'

const route = useRoute()
const api = useApi()
const id = computed(() => String(route.params.id))

const detail = ref<ContentItemDetailResponse | null>(null)
const pending = ref(true)
const error = ref<string | null>(null)
const actionError = ref<string | null>(null)
const busy = ref(false)
const showDelete = ref(false)

const statusOrder: ReadingStatus[] = ['UNREAD', 'IN_PROGRESS', 'READ']

async function load() {
  pending.value = true
  error.value = null
  try {
    detail.value = await api<ContentItemDetailResponse>(`/content-items/${id.value}`)
  } catch (exception) {
    error.value = toApiRequestError(exception).message
  } finally {
    pending.value = false
  }
}

async function toggleFavorite() {
  if (!detail.value) return
  busy.value = true
  actionError.value = null
  try {
    const method = detail.value.contentItem.isFavorite ? 'DELETE' : 'POST'
    const result = await api<{ isFavorite: boolean }>(`/content-items/${id.value}/favorite`, { method })
    detail.value.contentItem.isFavorite = result.isFavorite
  } catch (exception) {
    actionError.value = toApiRequestError(exception).message
  } finally {
    busy.value = false
  }
}

async function setReadingStatus(status: ReadingStatus) {
  busy.value = true
  actionError.value = null
  try {
    const state = await api<ContentItemDetailResponse['readingState']>(`/content-items/${id.value}/reading-state`, {
      method: 'PUT',
      body: { status }
    })
    if (detail.value) detail.value.readingState = state
  } catch (exception) {
    actionError.value = toApiRequestError(exception).message
  } finally {
    busy.value = false
  }
}

function onTagsUpdated(tags: string[]) {
  if (detail.value) detail.value.contentItem.tags = tags
}

async function confirmDelete() {
  busy.value = true
  actionError.value = null
  try {
    await api(`/content-items/${id.value}`, { method: 'DELETE' })
    await navigateTo('/library')
  } catch (exception) {
    actionError.value = toApiRequestError(exception).message
    busy.value = false
    showDelete.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="mx-auto max-w-3xl">
    <div v-if="pending" class="py-16 text-center text-sm text-on-surface-variant">
      Loading item…
    </div>

    <LibraryErrorState
      v-else-if="error"
      :title="'This item is not available'"
      :message="error"
      @retry="load"
    />

    <template v-else-if="detail">
      <div class="flex items-center justify-between gap-4">
        <NuxtLink
          to="/library"
          class="inline-flex items-center gap-1.5 text-sm text-on-surface-variant hover:text-on-surface"
        >
          <UIcon name="i-lucide-arrow-left" class="size-4" />
          Library
        </NuxtLink>
        <a
          :href="detail.contentItem.sourceUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="inline-flex items-center gap-1.5 text-sm text-on-surface-variant hover:text-on-surface"
        >
          Original
          <UIcon name="i-lucide-external-link" class="size-4" />
        </a>
      </div>

      <header class="mt-8">
        <div class="flex flex-wrap items-center gap-2">
          <h1 class="font-serif text-3xl leading-tight sm:text-4xl">
            {{ detail.contentItem.title }}
          </h1>
          <UBadge color="neutral" variant="subtle" size="sm">
            {{ detail.contentItem.contentType === 'PDF' ? 'PDF' : 'Article' }}
          </UBadge>
        </div>

        <p v-if="detail.contentItem.description" class="mt-3 text-on-surface-variant">
          {{ detail.contentItem.description }}
        </p>

        <div class="mt-4 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-on-surface-variant">
          <span v-if="detail.contentItem.sourceName">{{ detail.contentItem.sourceName }}</span>
          <span v-if="formatReadingTime(detail.contentItem.readingTimeMinutes)">
            {{ formatReadingTime(detail.contentItem.readingTimeMinutes) }}
          </span>
          <span>Saved {{ formatDate(detail.contentItem.createdAt) }}</span>
          <span v-if="detail.contentItem.lastReadAt">· Last read {{ formatDate(detail.contentItem.lastReadAt) }}</span>
        </div>
      </header>

      <UAlert
        v-if="detail.warnings.length"
        class="mt-6"
        color="warning"
        variant="subtle"
        title="Partial capture"
        :description="detail.warnings.map(warning => warning.message).join(' ')"
      />

      <UAlert
        v-if="actionError"
        class="mt-4"
        color="error"
        variant="subtle"
        title="Action failed"
        :description="actionError"
      />

      <div class="mt-8 flex flex-wrap items-center gap-3">
        <UButton
          v-if="detail.contentItem.status === 'READY'"
          :to="`/content/${detail.contentItem.id}/read`"
          color="primary"
        >
          {{ detail.readingState.status === 'UNREAD' ? 'Start reading' : 'Continue reading' }}
        </UButton>
        <UButton
          color="neutral"
          :variant="detail.contentItem.isFavorite ? 'soft' : 'outline'"
          :icon="'i-lucide-star'"
          :loading="busy"
          @click="toggleFavorite"
        >
          {{ detail.contentItem.isFavorite ? 'Favorited' : 'Favorite' }}
        </UButton>
        <UButton
          color="error"
          variant="ghost"
          icon="i-lucide-trash-2"
          @click="showDelete = true"
        >
          Delete
        </UButton>
      </div>

      <section class="mt-10">
        <h2 class="font-serif text-xl">
          Reading status
        </h2>
        <div class="mt-3 flex flex-wrap gap-2">
          <button
            v-for="status in statusOrder"
            :key="status"
            type="button"
            class="rounded-md border px-3 py-1.5 text-sm transition-colors"
            :class="detail.readingState.status === status
              ? 'border-cul-300 bg-cul-50 text-cul-700'
              : 'border-outline text-on-surface-variant hover:border-cul-300'"
            :aria-pressed="detail.readingState.status === status"
            :disabled="busy"
            @click="setReadingStatus(status)"
          >
            {{ READING_STATUS_LABELS[status] }}
          </button>
        </div>
      </section>

      <section class="mt-10">
        <h2 class="font-serif text-xl">
          Tags
        </h2>
        <LibraryTagPicker
          class="mt-3"
          :content-item-id="detail.contentItem.id"
          :assigned="detail.contentItem.tags"
          @updated="onTagsUpdated"
        />
      </section>

      <section class="mt-10">
        <h2 class="font-serif text-xl">
          Capture details
        </h2>
        <dl class="mt-3 space-y-2 text-sm">
          <div class="flex justify-between gap-4">
            <dt class="text-on-surface-variant">
              Capture status
            </dt>
            <dd>{{ CAPTURE_STATUS_LABELS[detail.contentItem.status] ?? detail.contentItem.status }}</dd>
          </div>
          <div class="flex justify-between gap-4">
            <dt class="text-on-surface-variant">
              Artifact versions
            </dt>
            <dd>{{ detail.artifacts.length }}</dd>
          </div>
        </dl>

        <ul class="mt-4 divide-y divide-outline/70">
          <li
            v-for="artifact in detail.artifacts"
            :key="artifact.id"
            class="flex flex-wrap items-center justify-between gap-3 py-3 text-sm"
          >
            <div>
              <p class="font-medium">
                Version {{ artifact.versionNumber }}
                <span v-if="artifact.isCurrent" class="ml-2 text-xs text-cul-700">current</span>
              </p>
              <p class="mt-0.5 text-xs text-on-surface-variant">
                {{ artifact.adapterId }} · schema v{{ artifact.packageSchemaVersion }} · {{ formatBytes(artifact.byteSize) }}
              </p>
            </div>
            <div class="text-right text-xs text-on-surface-variant">
              <p>{{ artifact.validationStatus.replaceAll('_', ' ').toLowerCase() }}</p>
              <p>{{ formatDate(artifact.capturedAt) }}</p>
            </div>
          </li>
        </ul>
      </section>
    </template>

    <div
      v-if="showDelete"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="delete-title"
      @click.self="showDelete = false"
    >
      <div class="w-full max-w-sm rounded-lg border border-outline bg-surface p-5">
        <h2 id="delete-title" class="font-serif text-lg">
          Delete this item?
        </h2>
        <p class="mt-2 text-sm text-on-surface-variant">
          The saved content, its artifacts, and search index rows will be removed.
          This cannot be undone.
        </p>
        <div class="mt-5 flex justify-end gap-2">
          <UButton
            color="neutral"
            variant="ghost"
            :disabled="busy"
            @click="showDelete = false"
          >
            Cancel
          </UButton>
          <UButton color="error" :loading="busy" @click="confirmDelete">
            Delete
          </UButton>
        </div>
      </div>
    </div>
  </section>
</template>
