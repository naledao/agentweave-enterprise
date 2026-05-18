<template>
  <article class="tool-invocation-card">
    <header class="tool-invocation-card__header">
      <div>
        <strong>{{ invocation.toolName }}</strong>
        <span v-if="toolCode">{{ toolCode }}</span>
      </div>
      <el-tag :type="statusMeta.type" effect="plain">{{ statusMeta.label }}</el-tag>
    </header>

    <dl class="tool-invocation-card__meta">
      <div v-if="durationText">
        <dt>耗时</dt>
        <dd>{{ durationText }}</dd>
      </div>
      <div v-if="invocation.traceId">
        <dt>追踪</dt>
        <dd><TraceIdText :trace-id="invocation.traceId" /></dd>
      </div>
    </dl>

    <section v-if="invocation.inputSummary" class="tool-invocation-card__section">
      <h4>入参摘要</h4>
      <p>{{ invocation.inputSummary }}</p>
    </section>

    <section v-if="invocation.resultSummary" class="tool-invocation-card__section">
      <h4>结果摘要</h4>
      <p>{{ invocation.resultSummary }}</p>
    </section>

    <section v-if="invocation.errorMessage" class="tool-invocation-card__section">
      <h4>错误摘要</h4>
      <p>{{ invocation.errorMessage }}</p>
    </section>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import TraceIdText from '@/shared/components/TraceIdText.vue'

type ToolInvocationCardStatus =
  | 'RUNNING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'running'
  | 'success'
  | 'failed'
  | 'denied'
  | 'timeout'

export interface ToolInvocationCardModel {
  toolCallId?: string
  id?: string
  toolCode?: string | null
  toolName: string
  status: ToolInvocationCardStatus
  inputSummary?: string | null
  resultSummary?: string | null
  errorMessage?: string | null
  latencyMs?: number | null
  durationMs?: number | null
  traceId?: string | null
}

const props = defineProps<{
  invocation: ToolInvocationCardModel
}>()

const toolCode = computed(() => props.invocation.toolCode ?? props.invocation.toolCallId ?? props.invocation.id)

const durationText = computed(() => {
  const duration = props.invocation.durationMs ?? props.invocation.latencyMs
  return duration === null || duration === undefined ? '' : `${duration} ms`
})

const statusMeta = computed(() => {
  const map: Record<ToolInvocationCardStatus, { label: string; type: 'info' | 'success' | 'warning' | 'danger' }> = {
    RUNNING: { label: '执行中', type: 'info' },
    SUCCEEDED: { label: '成功', type: 'success' },
    FAILED: { label: '失败', type: 'danger' },
    running: { label: '执行中', type: 'info' },
    success: { label: '成功', type: 'success' },
    failed: { label: '失败', type: 'danger' },
    denied: { label: '拒绝', type: 'warning' },
    timeout: { label: '超时', type: 'danger' },
  }

  return map[props.invocation.status]
})
</script>

<style scoped>
.tool-invocation-card {
  display: grid;
  gap: 10px;
  border: 1px solid #dfe5ee;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
}

.tool-invocation-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.tool-invocation-card__header > div {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.tool-invocation-card__header strong {
  overflow: hidden;
  color: #263143;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tool-invocation-card__header span {
  overflow: hidden;
  color: #69778d;
  font-family: ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tool-invocation-card__meta {
  display: grid;
  gap: 8px;
  margin: 0;
}

.tool-invocation-card__meta div {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 8px;
}

.tool-invocation-card__meta dt {
  color: #69778d;
  font-size: 12px;
}

.tool-invocation-card__meta dd {
  min-width: 0;
  margin: 0;
  color: #263143;
  font-size: 12px;
}

.tool-invocation-card__section {
  display: grid;
  gap: 4px;
}

.tool-invocation-card__section h4 {
  margin: 0;
  color: #69778d;
  font-size: 12px;
  font-weight: 600;
}

.tool-invocation-card__section p {
  margin: 0;
  color: #39485f;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
}
</style>
