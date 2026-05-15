<template>
  <section class="summary-list">
    <h4>工具调用</h4>
    <div v-if="toolCalls.length === 0" class="list-empty">暂无工具调用</div>
    <article v-for="(toolCall, index) in toolCalls" v-else :key="`${toolCall.toolCode}-${index}`" class="summary-card">
      <div class="card-title">
        <strong>{{ toolCall.toolCode }}</strong>
        <el-tag effect="plain" type="info">{{ toolCall.status }}</el-tag>
      </div>
      <p v-if="toolCall.inputSummary">入参：{{ toolCall.inputSummary }}</p>
      <p v-if="toolCall.resultSummary">结果：{{ toolCall.resultSummary }}</p>
      <div class="tool-meta">
        <span v-if="toolCall.latencyMs !== null && toolCall.latencyMs !== undefined">
          {{ formatDuration(toolCall.latencyMs) }}
        </span>
        <TraceIdText v-if="toolCall.traceId" :trace-id="toolCall.traceId" />
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import { formatDuration } from '@/features/workflows/components/workflowFormatters'
import type { WorkflowToolCall } from '@/features/workflows/types'
import TraceIdText from '@/shared/components/TraceIdText.vue'

defineProps<{
  toolCalls: WorkflowToolCall[]
}>()
</script>

<style scoped>
.summary-list {
  display: grid;
  gap: 10px;
}

.summary-list h4 {
  margin: 0;
  color: #182233;
  font-size: 14px;
}

.list-empty {
  color: #69778d;
  font-size: 13px;
}

.summary-card {
  display: grid;
  gap: 8px;
  border: 1px solid #dfe5ee;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
}

.card-title,
.tool-meta {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.card-title strong {
  overflow-wrap: anywhere;
}

.tool-meta {
  justify-content: flex-start;
  color: #69778d;
  font-size: 12px;
}

p {
  margin: 0;
  color: #39485f;
  font-size: 13px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}
</style>
