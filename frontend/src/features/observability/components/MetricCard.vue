<template>
  <article class="metric-card" :class="toneClass">
    <div class="metric-card__header">
      <span>{{ label }}</span>
      <el-tag v-if="tag" size="small" effect="plain" :type="tagType">{{ tag }}</el-tag>
    </div>
    <strong>{{ value }}</strong>
    <small>{{ hint }}</small>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  label: string
  value: string | number
  hint?: string
  tone?: 'default' | 'success' | 'warning' | 'danger'
  tag?: string
}>(), {
  hint: '',
  tone: 'default',
  tag: '',
})

const toneClass = computed(() => `metric-card--${props.tone}`)
const tagType = computed(() => {
  const map = {
    default: 'info',
    success: 'success',
    warning: 'warning',
    danger: 'danger',
  } as const
  return map[props.tone]
})
</script>

<style scoped>
.metric-card {
  display: grid;
  min-width: 0;
  gap: 8px;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.metric-card__header {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.metric-card__header span,
.metric-card small {
  overflow: hidden;
  color: #69778d;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-card strong {
  overflow: hidden;
  color: #182233;
  font-size: 24px;
  line-height: 1.1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-card--success strong {
  color: #137044;
}

.metric-card--warning strong {
  color: #a15c09;
}

.metric-card--danger strong {
  color: #c03639;
}
</style>
