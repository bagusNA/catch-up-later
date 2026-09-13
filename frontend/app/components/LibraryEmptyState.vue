<script setup lang="ts">
withDefaults(defineProps<{
  title: string
  description?: string
  icon?: string
  actionLabel?: string
}>(), {
  description: '',
  icon: 'i-lucide-library',
  actionLabel: ''
})

defineEmits<{ action: [] }>()
</script>

<template>
  <div class="rounded-lg border border-dashed border-outline px-6 py-16 text-center text-on-surface-variant">
    <UIcon :name="icon" class="mx-auto size-8 text-on-surface-variant/70" />
    <p class="mt-4 font-serif text-lg text-on-surface">
      {{ title }}
    </p>
    <p v-if="description" class="mx-auto mt-1 max-w-md text-sm">
      {{ description }}
    </p>
    <UButton
      v-if="actionLabel"
      class="mt-5"
      color="primary"
      variant="soft"
      @click="$emit('action')"
    >
      {{ actionLabel }}
    </UButton>
  </div>
</template>
