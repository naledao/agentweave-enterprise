<template>
  <section class="summary-grid">
    <div class="summary-item">
      <span>构建日志</span>
      <strong>{{ summary?.indexLogCount ?? 0 }}</strong>
      <small>latest {{ latestIndexStatus }}</small>
    </div>
    <div class="summary-item">
      <span>检索日志</span>
      <strong>{{ summary?.retrievalLogCount ?? 0 }}</strong>
      <small>latest {{ latestRetrievalStatus }}</small>
    </div>
    <div class="summary-item">
      <span>最近构建耗时</span>
      <strong>{{ formatDuration(summary?.latestIndexLog?.durationMs ?? null) }}</strong>
      <TraceIdText
        v-if="summary?.latestIndexLog?.traceId"
        :trace-id="summary.latestIndexLog.traceId"
      />
    </div>
    <div class="summary-item">
      <span>最近路径数量</span>
      <strong>{{ summary?.latestRetrievalLog?.filteredPathCount ?? 0 }}</strong>
      <TraceIdText
        v-if="summary?.latestRetrievalLog?.traceId"
        :trace-id="summary.latestRetrievalLog.traceId"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { GraphRagSummary } from '@/features/observability/types'
import TraceIdText from '@/shared/components/TraceIdText.vue'

const props = defineProps<{
  summary: GraphRagSummary | null
}>()

const latestIndexStatus = computed(() => props.summary?.latestIndexLog?.status ?? '-')
const latestRetrievalStatus = computed(() => props.summary?.latestRetrievalLog?.status ?? '-')

function formatDuration(value: number | null): string {
  if (value === null || value === undefined) {
    return '-'
  }

  return `${value} ms`
}
</script>

<style scoped>
.summary-grid {
  display: grid;
  flex: none;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-item {
  min-width: 0;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.summary-item span,
.summary-item small {
  display: block;
  overflow: hidden;
  color: #69778d;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-item strong {
  display: block;
  overflow: hidden;
  margin: 8px 0 6px;
  color: #182233;
  font-size: 22px;
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.trace-id-text code) {
  max-width: 180px;
}

@media (max-width: 1180px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
