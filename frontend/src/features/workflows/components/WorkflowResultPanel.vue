<template>
  <aside class="workflow-result-panel">
    <div class="section-heading">
      <h3>最终结果</h3>
      <WorkflowStatusTag :status="run.status" />
    </div>

    <div v-if="run.finalAnswer" class="answer-box">{{ run.finalAnswer }}</div>
    <el-empty v-else description="暂无最终答案" />

    <section v-if="run.errorCode || run.errorMessage" class="error-box">
      <strong>{{ run.errorCode || 'WORKFLOW_ERROR' }}</strong>
      <p>{{ run.errorMessage }}</p>
    </section>

    <dl class="meta-list">
      <div>
        <dt>当前步骤</dt>
        <dd>{{ run.currentStepIndex }}</dd>
      </div>
      <div>
        <dt>开始时间</dt>
        <dd>{{ formatDateTime(run.startedAt) }}</dd>
      </div>
      <div>
        <dt>结束时间</dt>
        <dd>{{ formatDateTime(run.finishedAt) }}</dd>
      </div>
      <div>
        <dt>耗时</dt>
        <dd>{{ elapsedDuration(run.startedAt, run.finishedAt) }}</dd>
      </div>
    </dl>
  </aside>
</template>

<script setup lang="ts">
import {
  elapsedDuration,
  formatDateTime,
} from '@/features/workflows/components/workflowFormatters'
import WorkflowStatusTag from '@/features/workflows/components/WorkflowStatusTag.vue'
import type { WorkflowRun } from '@/features/workflows/types'

defineProps<{
  run: WorkflowRun
}>()
</script>

<style scoped>
.workflow-result-panel {
  display: grid;
  align-content: start;
  gap: 14px;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  padding: 16px;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-heading h3 {
  margin: 0;
  color: #182233;
  font-size: 16px;
}

.answer-box {
  max-height: 260px;
  overflow: auto;
  border: 1px solid #dce3ed;
  border-radius: 8px;
  background: #f8fafc;
  color: #263143;
  font-size: 13px;
  line-height: 1.7;
  padding: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}

.error-box {
  display: grid;
  gap: 6px;
  border: 1px solid #f0c4c4;
  border-radius: 8px;
  background: #fff7f7;
  padding: 12px;
}

.error-box strong {
  color: #b84242;
  font-size: 13px;
}

.error-box p {
  margin: 0;
  color: #7c4545;
  font-size: 13px;
  line-height: 1.5;
}

.meta-list {
  display: grid;
  gap: 10px;
  margin: 0;
}

.meta-list div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.meta-list dt,
.meta-list dd {
  margin: 0;
  font-size: 13px;
}

.meta-list dt {
  color: #69778d;
}

.meta-list dd {
  color: #263143;
}
</style>
