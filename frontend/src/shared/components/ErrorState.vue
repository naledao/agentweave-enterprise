<template>
  <section class="error-state" :class="{ 'error-state--compact': compact }">
    <el-alert
      :title="title"
      :description="message"
      type="error"
      :closable="false"
      show-icon
    />
    <TraceIdText v-if="traceId" :trace-id="traceId" />
    <div v-if="$slots.actions" class="error-state__actions">
      <slot name="actions" />
    </div>
  </section>
</template>

<script setup lang="ts">
import TraceIdText from '@/shared/components/TraceIdText.vue'

withDefaults(
  defineProps<{
    title?: string
    message: string
    traceId?: string | null
    compact?: boolean
  }>(),
  {
    title: '加载失败',
    traceId: null,
    compact: false,
  },
)
</script>

<style scoped>
.error-state {
  display: grid;
  gap: 12px;
  padding: 20px;
}

.error-state--compact {
  padding: 12px;
}

.error-state__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
