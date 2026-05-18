<template>
  <pre class="json-viewer"><code>{{ formattedValue }}</code></pre>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    value: unknown
    emptyText?: string
  }>(),
  {
    emptyText: '{}',
  },
)

const formattedValue = computed(() => {
  if (props.value === null || props.value === undefined || props.value === '') {
    return props.emptyText
  }

  if (typeof props.value === 'string') {
    try {
      return JSON.stringify(JSON.parse(props.value), null, 2)
    } catch {
      return props.value
    }
  }

  return JSON.stringify(props.value, null, 2)
})
</script>

<style scoped>
.json-viewer {
  max-height: 360px;
  overflow: auto;
  margin: 0;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #0f1722;
  color: #d9e4f2;
  font-family: ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  line-height: 1.6;
  padding: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
