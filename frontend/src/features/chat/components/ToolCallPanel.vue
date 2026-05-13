<template>
  <section class="panel-section">
    <h3>工具调用</h3>
    <div v-if="invocations.length === 0" class="panel-empty">暂无工具调用</div>
    <article v-for="invocation in invocations" v-else :key="invocation.toolCallId" class="tool-card">
      <div class="tool-title">
        <strong>{{ invocation.toolName }}</strong>
        <el-tag :type="tagType(invocation.status)" effect="plain">{{ statusLabel(invocation.status) }}</el-tag>
      </div>
      <p v-if="invocation.inputSummary">{{ invocation.inputSummary }}</p>
      <p v-if="invocation.resultSummary">{{ invocation.resultSummary }}</p>
      <span v-if="invocation.latencyMs !== undefined" class="muted-text">{{ invocation.latencyMs }}ms</span>
      <TraceIdText v-if="invocation.traceId" :trace-id="invocation.traceId" />
    </article>
  </section>
</template>

<script setup lang="ts">
import type { ToolInvocation, ToolInvocationStatus } from '@/features/chat/types'
import TraceIdText from '@/shared/components/TraceIdText.vue'

defineProps<{
  invocations: ToolInvocation[]
}>()

function tagType(status: ToolInvocationStatus): 'info' | 'success' | 'danger' {
  const map: Record<ToolInvocationStatus, 'info' | 'success' | 'danger'> = {
    RUNNING: 'info',
    SUCCEEDED: 'success',
    FAILED: 'danger',
  }

  return map[status]
}

function statusLabel(status: ToolInvocationStatus): string {
  const map: Record<ToolInvocationStatus, string> = {
    RUNNING: '执行中',
    SUCCEEDED: '成功',
    FAILED: '失败',
  }

  return map[status]
}
</script>

<style scoped>
.panel-section {
  display: grid;
  gap: 10px;
}

.panel-section h3 {
  margin: 0;
  color: #182233;
  font-size: 15px;
}

.panel-empty {
  color: #69778d;
  font-size: 13px;
}

.tool-card {
  display: grid;
  gap: 8px;
  border: 1px solid #dfe5ee;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
}

.tool-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.tool-card p {
  margin: 0;
  color: #39485f;
  font-size: 13px;
  line-height: 1.6;
}
</style>
