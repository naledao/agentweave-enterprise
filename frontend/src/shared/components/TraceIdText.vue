<template>
  <button class="trace-id-text" type="button" :title="traceId" @click="copyTraceId">
    <el-icon aria-hidden="true">
      <DocumentCopy />
    </el-icon>
    <span>traceId:</span>
    <code>{{ traceId }}</code>
  </button>
</template>

<script setup lang="ts">
import { DocumentCopy } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  traceId: string
}>()

async function copyTraceId(): Promise<void> {
  await navigator.clipboard.writeText(props.traceId)
  ElMessage.success('traceId copied')
}
</script>

<style scoped>
.trace-id-text {
  display: inline-flex;
  max-width: 100%;
  align-items: center;
  gap: 6px;
  border: 0;
  background: transparent;
  color: #5b6b84;
  cursor: pointer;
  font: inherit;
  font-size: 12px;
  padding: 0;
}

.trace-id-text:hover {
  color: #2457c5;
}

.trace-id-text code {
  max-width: 220px;
  overflow: hidden;
  color: inherit;
  font-family: ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", monospace;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
