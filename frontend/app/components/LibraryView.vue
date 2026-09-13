<script setup lang="ts">
import type { ContentItemListResponse, ContentItemSummary, TagResponse } from '~/types/api'
import type { LibraryFilters } from '~/utils/library'
import { buildLibraryQuery, buildRouteQuery, parseLibraryFilters } from '~/utils/library'

const props = withDefaults(defineProps<{
  scope?: 'library' | 'favorites'
  emptyTitle?: string
  emptyDescription?: string
}>(), {
  scope: 'library',
  emptyTitle: 'Nothing saved yet',
  emptyDescription: 'Use the browser extension to save an article or PDF.'
})

const route = useRoute()
const router = useRouter()
const api = useApi()

const view = ref<'list' | 'grid'>('list')
const items = ref<ContentItemSummary[]>([])
const total = ref(0)
const tags = ref<TagResponse[]>([])
const pending = ref(false)
const error = ref<{ message: string } | null>(null)
const favoriteBusy = ref<number | null>(null)

const filters = computed<LibraryFilters>(() => {
  const parsed = parseLibraryFilters(route.query)
  return props.scope === 'favorites' ? { ...parsed, favorite: true } : parsed
})

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / filters.value.pageSize)))

async function load() {
  pending.value = true
  error.value = null
  try {
    const result = await api<ContentItemListResponse>('/content-items', {
      query: buildLibraryQuery(filters.value)
    })
    items.value = result.items
    total.value = result.total
  } catch (exception) {
    error.value = { message: toApiRequestError(exception).message }
  } finally {
    pending.value = false
  }
}

async function loadTags() {
  try {
    tags.value = await api<TagResponse[]>('/tags')
  } catch {
    tags.value = []
  }
}

function updateFilters(partial: Partial<LibraryFilters>) {
  const next = { ...filters.value, ...partial }
  router.push({ path: route.path, query: buildRouteQuery(next) })
}

async function toggleFavorite(item: ContentItemSummary) {
  favoriteBusy.value = item.id
  try {
    const method = item.isFavorite ? 'DELETE' : 'POST'
    const result = await api<{ isFavorite: boolean }>(`/content-items/${item.id}/favorite`, { method })
    const updated = items.value.map(entry =>
      entry.id === item.id ? { ...entry, isFavorite: result.isFavorite } : entry
    )
    items.value = props.scope === 'favorites' && !result.isFavorite
      ? updated.filter(entry => entry.id !== item.id)
      : updated
    if (props.scope === 'favorites' && !result.isFavorite) {
      total.value = Math.max(0, total.value - 1)
    }
    loadTags()
  } finally {
    favoriteBusy.value = null
  }
}

watch(() => route.query, load, { immediate: true, deep: true })
onMounted(loadTags)
</script>

<template>
  <section :class="view === 'grid' ? 'mx-auto max-w-6xl' : 'mx-auto max-w-3xl'">
    <div class="flex items-center justify-between gap-4">
      <slot name="heading">
        <h1 class="font-serif text-3xl">
          {{ scope === 'favorites' ? 'Favorites' : 'Library' }}
        </h1>
      </slot>
    </div>

    <LibraryToolbar
      class="mt-6"
      :filters="filters"
      :tags="tags"
      :total="total"
      :view="view"
      :pending="pending"
      :show-favorite-filter="scope !== 'favorites'"
      @update:filters="updateFilters"
      @update:view="view = $event"
    />

    <p v-if="pending && !items.length" class="mt-10 text-center text-sm text-on-surface-variant">
      Loading your library…
    </p>

    <LibraryErrorState
      v-else-if="error"
      class="mt-8"
      :message="error.message"
      @retry="load"
    />

    <LibraryEmptyState
      v-else-if="!items.length"
      class="mt-10"
      :title="filters.q || filters.contentType || filters.status || filters.tag || filters.favorite
        ? 'No matching items'
        : emptyTitle"
      :description="filters.q || filters.contentType || filters.status || filters.tag || filters.favorite
        ? 'Try a different search or clear your filters.'
        : emptyDescription"
    />

    <ul
      v-else
      class="mt-8"
      :class="view === 'grid' ? 'grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3' : 'space-y-3'"
    >
      <li v-for="item in items" :key="item.id">
        <LibraryContentCard
          :item="item"
          :view="view"
          :busy="favoriteBusy === item.id"
          @toggle-favorite="toggleFavorite"
        />
      </li>
    </ul>

    <nav
      v-if="items.length && totalPages > 1"
      class="mt-8 flex items-center justify-between"
      aria-label="Pagination"
    >
      <UButton
        color="neutral"
        variant="ghost"
        :disabled="filters.page <= 0"
        icon="i-lucide-chevron-left"
        @click="updateFilters({ page: filters.page - 1 })"
      >
        Previous
      </UButton>
      <span class="text-xs text-on-surface-variant">
        Page {{ filters.page + 1 }} of {{ totalPages }}
      </span>
      <UButton
        color="neutral"
        variant="ghost"
        trailing-icon="i-lucide-chevron-right"
        :disabled="filters.page >= totalPages - 1"
        @click="updateFilters({ page: filters.page + 1 })"
      >
        Next
      </UButton>
    </nav>
  </section>
</template>
