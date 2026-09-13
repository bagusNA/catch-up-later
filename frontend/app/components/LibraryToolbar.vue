<script setup lang="ts">
import type { ContentType, ReadingStatus, TagResponse } from '~/types/api'
import type { LibraryFilters } from '~/utils/library'
import { READING_STATUS_LABELS, SORT_OPTIONS } from '~/utils/library'

const props = withDefaults(defineProps<{
  filters: LibraryFilters
  tags: TagResponse[]
  total: number
  view: 'list' | 'grid'
  pending?: boolean
  showFavoriteFilter?: boolean
}>(), {
  pending: false,
  showFavoriteFilter: true
})

const emit = defineEmits<{
  'update:filters': [Partial<LibraryFilters>]
  'update:view': ['list' | 'grid']
}>()

const search = ref(props.filters.q)

watch(() => props.filters.q, (value) => {
  if (value !== search.value) search.value = value
})

function submitSearch() {
  emit('update:filters', { q: search.value, page: 0 })
}

function clearSearch() {
  search.value = ''
  emit('update:filters', { q: '', page: 0 })
}

const selectClass
  = 'rounded-md border border-outline bg-surface px-2.5 py-1.5 text-sm text-on-surface focus:border-cul-400 focus:outline-none'
</script>

<template>
  <div class="space-y-4">
    <form class="flex gap-2" role="search" @submit.prevent="submitSearch">
      <label class="relative flex-1">
        <span class="sr-only">Search your library</span>
        <UIcon
          name="i-lucide-search"
          class="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-on-surface-variant"
        />
        <input
          v-model="search"
          type="search"
          placeholder="Search titles, text, and tags…"
          class="w-full rounded-md border border-outline bg-surface py-2 pl-9 pr-3 text-sm text-on-surface placeholder:text-on-surface-variant/70 focus:border-cul-400 focus:outline-none"
        >
      </label>
      <UButton
        type="submit"
        color="primary"
        variant="soft"
        :loading="pending"
      >
        Search
      </UButton>
      <UButton
        v-if="filters.q"
        color="neutral"
        variant="ghost"
        @click="clearSearch"
      >
        Clear
      </UButton>
    </form>

    <div class="flex flex-wrap items-center gap-2">
      <select
        :value="filters.contentType"
        :class="selectClass"
        aria-label="Filter by content type"
        @change="emit('update:filters', { contentType: ($event.target as HTMLSelectElement).value as ContentType | '', page: 0 })"
      >
        <option value="">
          All types
        </option>
        <option value="ARTICLE">
          Articles
        </option>
        <option value="PDF">
          PDFs
        </option>
      </select>

      <select
        :value="filters.status"
        :class="selectClass"
        aria-label="Filter by reading status"
        @change="emit('update:filters', { status: ($event.target as HTMLSelectElement).value as ReadingStatus | '', page: 0 })"
      >
        <option value="">
          Any status
        </option>
        <option v-for="(label, value) in READING_STATUS_LABELS" :key="value" :value="value">
          {{ label }}
        </option>
      </select>

      <select
        v-if="tags.length"
        :value="filters.tag"
        :class="selectClass"
        aria-label="Filter by tag"
        @change="emit('update:filters', { tag: ($event.target as HTMLSelectElement).value, page: 0 })"
      >
        <option value="">
          All tags
        </option>
        <option v-for="tag in tags" :key="tag.id" :value="tag.name">
          #{{ tag.name }} ({{ tag.itemCount }})
        </option>
      </select>

      <button
        v-if="showFavoriteFilter"
        type="button"
        class="inline-flex items-center gap-1.5 rounded-md border px-2.5 py-1.5 text-sm transition-colors"
        :class="filters.favorite
          ? 'border-cul-300 bg-cul-50 text-cul-700'
          : 'border-outline text-on-surface-variant hover:border-cul-300'"
        :aria-pressed="filters.favorite"
        @click="emit('update:filters', { favorite: !filters.favorite, page: 0 })"
      >
        <UIcon name="i-lucide-star" class="size-4" />
        Favorites
      </button>

      <div class="ml-auto flex items-center gap-2">
        <label class="sr-only" for="library-sort">Sort</label>
        <select
          id="library-sort"
          :value="filters.sort"
          :class="selectClass"
          @change="emit('update:filters', { sort: ($event.target as HTMLSelectElement).value as LibraryFilters['sort'], page: 0 })"
        >
          <option v-for="option in SORT_OPTIONS" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>

        <div class="flex overflow-hidden rounded-md border border-outline" role="group" aria-label="View style">
          <button
            type="button"
            class="px-2.5 py-1.5 text-sm"
            :class="view === 'list' ? 'bg-surface-container text-on-surface' : 'text-on-surface-variant'"
            aria-label="List view"
            :aria-pressed="view === 'list'"
            @click="emit('update:view', 'list')"
          >
            <UIcon name="i-lucide-list" class="size-4" />
          </button>
          <button
            type="button"
            class="px-2.5 py-1.5 text-sm"
            :class="view === 'grid' ? 'bg-surface-container text-on-surface' : 'text-on-surface-variant'"
            aria-label="Grid view"
            :aria-pressed="view === 'grid'"
            @click="emit('update:view', 'grid')"
          >
            <UIcon name="i-lucide-layout-grid" class="size-4" />
          </button>
        </div>
      </div>
    </div>

    <p class="text-xs text-on-surface-variant" aria-live="polite">
      {{ total }} {{ total === 1 ? 'item' : 'items' }}
      <template v-if="filters.q">
        matching “{{ filters.q }}”
      </template>
    </p>
  </div>
</template>
