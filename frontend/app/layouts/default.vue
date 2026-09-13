<script setup lang="ts">
const route = useRoute()
const { user, logout } = useAuth()

const nav = [
  { label: 'Library', to: '/library', icon: 'i-lucide-library' },
  { label: 'Favorites', to: '/favorites', icon: 'i-lucide-star' },
  { label: 'Tags', to: '/tags', icon: 'i-lucide-tag' },
  { label: 'Settings', to: '/settings', icon: 'i-lucide-settings' }
]

const bare = computed(() => route.path === '/login' || route.path === '/setup')

function isActive(to: string): boolean {
  return route.path === to || route.path.startsWith(`${to}/`)
}
</script>

<template>
  <div v-if="bare" class="min-h-screen bg-surface text-on-surface antialiased">
    <slot />
  </div>

  <div v-else class="min-h-screen bg-surface text-on-surface antialiased">
    <div class="mx-auto flex min-h-screen w-full max-w-7xl">
      <aside class="hidden w-60 shrink-0 flex-col border-r border-outline/70 px-4 py-6 md:flex">
        <NuxtLink to="/library" class="px-2 font-serif text-lg">
          Catch Up Later
        </NuxtLink>

        <nav class="mt-8 flex flex-1 flex-col gap-1">
          <NuxtLink
            v-for="item in nav"
            :key="item.to"
            :to="item.to"
            class="flex items-center gap-3 rounded-md px-3 py-2 text-sm transition-colors"
            :class="isActive(item.to)
              ? 'bg-cul-600/10 text-cul-700'
              : 'text-on-surface-variant hover:bg-surface-container'"
          >
            <UIcon :name="item.icon" class="size-4" />
            {{ item.label }}
          </NuxtLink>
        </nav>

        <div class="mt-6 border-t border-outline/70 pt-4">
          <p class="truncate px-2 text-sm font-medium">
            {{ user?.displayName || user?.email }}
          </p>
          <button
            type="button"
            class="mt-2 w-full rounded-md px-3 py-2 text-left text-sm text-on-surface-variant transition-colors hover:bg-surface-container"
            @click="logout"
          >
            Sign out
          </button>
        </div>
      </aside>

      <div class="flex min-w-0 flex-1 flex-col">
        <header class="flex flex-col gap-3 border-b border-outline/70 px-4 py-4 md:hidden">
          <div class="flex items-center justify-between">
            <NuxtLink to="/library" class="font-serif text-lg">
              Catch Up Later
            </NuxtLink>
            <button type="button" class="text-sm text-on-surface-variant" @click="logout">
              Sign out
            </button>
          </div>
          <nav class="flex gap-2 overflow-x-auto">
            <NuxtLink
              v-for="item in nav"
              :key="item.to"
              :to="item.to"
              class="rounded-md px-3 py-1.5 text-sm whitespace-nowrap"
              :class="isActive(item.to)
                ? 'bg-cul-600/10 text-cul-700'
                : 'text-on-surface-variant'"
            >
              {{ item.label }}
            </NuxtLink>
          </nav>
        </header>

        <main class="flex-1 px-4 py-8 sm:px-8">
          <slot />
        </main>
      </div>
    </div>
  </div>
</template>
