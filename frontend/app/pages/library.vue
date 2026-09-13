<script setup lang="ts">
import type { ContentItemListResponse, ContentItemSummary } from '~/types/api'

const api = useApi()
const { data, pending, error, refresh } = await useAsyncData(
  'library',
  () => api<ContentItemListResponse>('/content-items')
)

const items = computed<ContentItemSummary[]>(() => data.value?.items ?? [])

const statusLabels: Record<ContentItemSummary['status'], string> = {
  QUEUED: 'Queued',
  UPLOADING: 'Uploading',
  PROCESSING: 'Processing',
  READY: 'Ready',
  FAILED: 'Failed',
  CANCELLED: 'Cancelled'
}
</script>

<template>
  <section class="mx-auto max-w-3xl">
    <div class="flex items-center justify-between gap-4">
      <h1 class="font-serif text-3xl">
        Library
      </h1>
      <UButton
        v-if="items.length"
        color="neutral"
        variant="ghost"
        icon="i-lucide-refresh-cw"
        :loading="pending"
        @click="refresh()"
      >
        Refresh
      </UButton>
    </div>

    <p v-if="pending && !items.length" class="mt-8 text-on-surface-variant">
      Loading your library…
    </p>

    <UAlert
      v-else-if="error"
      class="mt-8"
      color="error"
      variant="subtle"
      title="Could not load your library"
      :description="error.message"
    />

    <div
      v-else-if="!items.length"
      class="mt-10 rounded-lg border border-dashed border-outline px-6 py-16 text-center text-on-surface-variant"
    >
      <p class="font-serif text-lg">
        Nothing saved yet
      </p>
      <p class="mt-1 text-sm">
        Use the browser extension to save an article or PDF.
      </p>
    </div>

    <ul v-else class="mt-8 divide-y divide-outline/70">
      <li v-for="item in items" :key="item.id">
        <NuxtLink
          :to="`/content/${item.id}/read`"
          class="group flex items-start gap-4 py-5"
        >
          <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-center gap-2">
              <h2 class="font-serif text-xl group-hover:text-cul-700">
                {{ item.title }}
              </h2>
              <UBadge
                v-if="item.status !== 'READY'"
                color="warning"
                variant="subtle"
                size="sm"
              >
                {{ statusLabels[item.status] }}
              </UBadge>
              <UBadge
                v-if="item.isFavorite"
                color="primary"
                variant="subtle"
                size="sm"
              >
                Favorite
              </UBadge>
            </div>

            <p v-if="item.description" class="mt-1 line-clamp-2 text-sm text-on-surface-variant">
              {{ item.description }}
            </p>

            <p class="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-on-surface-variant">
              <span v-if="item.sourceName">{{ item.sourceName }}</span>
              <span v-if="formatReadingTime(item.readingTimeMinutes)">
                {{ formatReadingTime(item.readingTimeMinutes) }}
              </span>
              <span>{{ formatDate(item.createdAt) }}</span>
            </p>
          </div>

          <UIcon
            name="i-lucide-chevron-right"
            class="mt-1 size-5 shrink-0 text-on-surface-variant transition-transform group-hover:translate-x-0.5"
          />
        </NuxtLink>
      </li>
    </ul>
  </section>
</template>
