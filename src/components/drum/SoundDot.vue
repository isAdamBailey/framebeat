<script setup lang="ts">
defineProps<{ dotClass: string; label: string }>()
</script>

<template>
  <span class="h-2 w-2 rounded-full" :class="dotClass" />
  {{ label }}
</template>
