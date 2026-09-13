<script setup lang="ts">
withDefaults(defineProps<{
  title?: string
  message?: string
  retryLabel?: string
}>(), {
  title: 'Something went wrong',
  message: 'The request could not be completed.',
  retryLabel: 'Try again'
})

defineEmits<{ retry: [] }>()
</script>

<template>
  <UAlert
    color="error"
    variant="subtle"
    :title="title"
    :description="message"
  >
    <template #actions>
      <UButton
        color="error"
        variant="soft"
        size="sm"
        @click="$emit('retry')"
      >
        {{ retryLabel }}
      </UButton>
    </template>
  </UAlert>
</template>
