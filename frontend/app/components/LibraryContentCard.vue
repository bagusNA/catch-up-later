<script setup lang="ts">
import type { ContentItemSummary } from '~/types/api'
import { CAPTURE_STATUS_LABELS, READING_STATUS_LABELS } from '~/utils/library'

const props = withDefaults(defineProps<{
  item: ContentItemSummary
  view?: 'list' | 'grid'
  busy?: boolean
}>(), {
  view: 'list',
  busy: false
})

defineEmits<{ 'toggle-favorite': [ContentItemSummary] }>()

const statusLabel = computed(() => CAPTURE_STATUS_LABELS[props.item.status] ?? props.item.status)
const readingLabel = computed(() => READING_STATUS_LABELS[props.item.readingStatus])
const hasWarning = computed(() => props.item.status !== 'READY')
</script>

<template>
  <article
    class="group relative flex gap-4 rounded-lg border border-outline/70 bg-surface p-4 transition-colors hover:border-cul-300"
  >
    <div class="min-w-0 flex-1">
      <div class="flex flex-wrap items-center gap-2">
        <h2 class="font-serif text-lg leading-snug group-hover:text-cul-700">
          <NuxtLink :to="`/content/${item.id}`" class="focus:outline-none focus-visible:underline">
            {{ item.title }}
          </NuxtLink>
        </h2>
        <UBadge
          v-if="hasWarning"
          color="warning"
          variant="subtle"
          size="sm"
        >
          {{ statusLabel }}
        </UBadge>
      </div>

      <p v-if="item.description" class="mt-1 line-clamp-2 text-sm text-on-surface-variant">
        {{ item.description }}
      </p>

      <div class="mt-3 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-on-surface-variant">
        <span v-if="item.sourceName">{{ item.sourceName }}</span>
        <span>{{ readingLabel }}</span>
        <span v-if="formatReadingTime(item.readingTimeMinutes)">{{ formatReadingTime(item.readingTimeMinutes) }}</span>
        <span>{{ formatDate(item.createdAt) }}</span>
      </div>

      <ul v-if="item.tags.length" class="mt-3 flex flex-wrap gap-1.5">
        <li v-for="tag in item.tags" :key="tag">
          <NuxtLink
            :to="`/library?tag=${encodeURIComponent(tag)}`"
            class="rounded-full bg-surface-container px-2 py-0.5 text-xs text-on-surface-variant hover:text-cul-700"
          >
            #{{ tag }}
          </NuxtLink>
        </li>
      </ul>
    </div>

    <div class="flex flex-col items-end justify-between">
      <UButton
        icon="i-lucide-star"
        :color="item.isFavorite ? 'primary' : 'neutral'"
        :variant="item.isFavorite ? 'soft' : 'ghost'"
        size="sm"
        :loading="busy"
        :aria-label="item.isFavorite ? 'Remove from favorites' : 'Add to favorites'"
        @click.prevent="$emit('toggle-favorite', item)"
      />
      <NuxtLink
        :to="`/content/${item.id}`"
        class="text-xs text-on-surface-variant hover:text-cul-700"
      >
        Details
      </NuxtLink>
    </div>
  </article>
</template>
