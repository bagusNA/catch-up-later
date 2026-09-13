<script setup lang="ts">
import type { ReaderResponse } from '~/types/api'

const route = useRoute()
const api = useApi()
const id = computed(() => String(route.params.id))

const { data, pending, error } = await useAsyncData(
  `reader-${id.value}`,
  () => api<ReaderResponse>(`/content-items/${id.value}/reader`)
)

const item = computed(() => data.value?.contentItem)
const metadata = computed(() => data.value?.metadata)
</script>

<template>
  <section class="mx-auto max-w-2xl">
    <div v-if="pending && !data" class="py-16 text-center text-on-surface-variant">
      Loading article…
    </div>

    <UAlert
      v-else-if="error"
      color="error"
      variant="subtle"
      title="This article is not available"
      :description="error.message"
    />

    <template v-else-if="data">
      <div class="flex items-center justify-between gap-4">
        <NuxtLink
          to="/library"
          class="inline-flex items-center gap-1.5 text-sm text-on-surface-variant hover:text-on-surface"
        >
          <UIcon name="i-lucide-arrow-left" class="size-4" />
          Library
        </NuxtLink>

        <a
          v-if="item?.sourceUrl"
          :href="item.sourceUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="inline-flex items-center gap-1.5 text-sm text-on-surface-variant hover:text-on-surface"
        >
          Original
          <UIcon name="i-lucide-external-link" class="size-4" />
        </a>
      </div>

      <header class="mt-8">
        <h1 class="font-serif text-3xl leading-tight sm:text-4xl">
          {{ item?.title }}
        </h1>

        <div class="mt-4 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-on-surface-variant">
          <span v-if="metadata?.author">{{ metadata.author }}</span>
          <span v-if="metadata?.siteName">· {{ metadata.siteName }}</span>
          <span v-if="metadata?.publishedAt">· {{ formatDate(metadata.publishedAt) }}</span>
          <span v-if="metadata?.readingTimeMinutes">· {{ formatReadingTime(metadata.readingTimeMinutes) }}</span>
        </div>

        <p class="mt-2 text-xs text-on-surface-variant">
          Saved {{ formatDate(data.artifact.capturedAt) }}
        </p>
      </header>

      <UAlert
        v-if="data.warnings.length"
        class="mt-6"
        color="warning"
        variant="subtle"
        title="Partial capture"
        :description="data.warnings.map(warning => warning.message).join(' ')"
      />

      <ArticleReader class="mt-8" :html="data.html" />
    </template>
  </section>
</template>
