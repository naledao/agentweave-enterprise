<template>
  <el-drawer
    :model-value="modelValue"
    destroy-on-close
    size="620px"
    title="工具调用详情"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <el-alert
      v-if="error.message"
      class="drawer-error"
      :title="error.message"
      type="error"
      :closable="false"
      show-icon
    >
      <TraceIdText v-if="error.traceId" :trace-id="error.traceId" />
    </el-alert>

    <div v-loading="loading" class="invocation-detail">
      <template v-if="invocation">
        <section class="detail-section">
          <h3>基础信息</h3>
          <dl class="detail-list">
            <div>
              <dt>工具名称</dt>
              <dd>{{ invocation.toolName }}</dd>
            </div>
            <div>
              <dt>工具编码</dt>
              <dd class="monospace-text">{{ invocation.toolCode }}</dd>
            </div>
            <div>
              <dt>工具类型</dt>
              <dd>{{ formatToolType(invocation.toolType) }}</dd>
            </div>
            <div>
              <dt>风险等级</dt>
              <dd>
                <ToolRiskTag v-if="invocation.riskLevel" :risk-level="invocation.riskLevel" />
                <span v-else>-</span>
              </dd>
            </div>
            <div>
              <dt>调用人</dt>
              <dd>{{ invocation.username || invocation.userId }}</dd>
            </div>
            <div>
              <dt>状态</dt>
              <dd><ToolInvocationStatusTag :status="invocation.status" /></dd>
            </div>
            <div>
              <dt>耗时</dt>
              <dd>{{ formatDuration(invocation.durationMs) }}</dd>
            </div>
            <div>
              <dt>创建时间</dt>
              <dd>{{ formatDateTime(invocation.createdAt) }}</dd>
            </div>
            <div>
              <dt>完成时间</dt>
              <dd>{{ formatDateTime(invocation.finishedAt) }}</dd>
            </div>
            <div>
              <dt>会话 ID</dt>
              <dd class="monospace-text">{{ invocation.conversationId || '-' }}</dd>
            </div>
            <div>
              <dt>消息 ID</dt>
              <dd class="monospace-text">{{ invocation.messageId || '-' }}</dd>
            </div>
            <div>
              <dt>工作流 Run</dt>
              <dd class="monospace-text">{{ invocation.workflowRunId || '-' }}</dd>
            </div>
            <div>
              <dt>工作流 Step</dt>
              <dd class="monospace-text">{{ invocation.workflowStepId || '-' }}</dd>
            </div>
          </dl>
          <TraceIdText v-if="invocation.traceId" :trace-id="invocation.traceId" />
        </section>

        <section class="detail-section">
          <h3>入参摘要</h3>
          <pre>{{ invocation.inputSummary || '-' }}</pre>
        </section>

        <section class="detail-section">
          <h3>结果摘要</h3>
          <pre>{{ invocation.resultSummary || '-' }}</pre>
        </section>

        <section class="detail-section">
          <h3>错误摘要</h3>
          <pre>{{ invocation.errorMessage || '-' }}</pre>
        </section>
      </template>

      <el-empty v-else-if="!loading && !error.message" description="请选择工具调用记录" />
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import ToolInvocationStatusTag from '@/features/tools/components/ToolInvocationStatusTag.vue'
import ToolRiskTag from '@/features/tools/components/ToolRiskTag.vue'
import type { ToolInvocationDetail } from '@/features/tools/types'
import TraceIdText from '@/shared/components/TraceIdText.vue'

defineProps<{
  modelValue: boolean
  invocation: ToolInvocationDetail | null
  loading?: boolean
  error: {
    message: string
    traceId: string | null
  }
}>()

defineEmits<{
  'update:modelValue': [value: boolean]
}>()

function formatDateTime(value: string | null): string {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function formatDuration(value: number | null): string {
  if (value === null || value === undefined) {
    return '-'
  }

  return `${value} ms`
}

function formatToolType(value: ToolInvocationDetail['toolType']): string {
  const map: Record<ToolInvocationDetail['toolType'], string> = {
    BUSINESS_QUERY: '业务查询',
    LOG_SEARCH: '日志检索',
    DATABASE_READ: '数据库只读',
    ENDPOINT_STATUS: '接口状态',
    NOTIFICATION: '消息通知',
    MCP_RESOURCE: 'MCP 资源',
    SCRIPT: '脚本',
    UNKNOWN: '未知',
  }
  return map[value] ?? value
}
</script>

<style scoped>
.invocation-detail {
  min-height: 240px;
}

.detail-section {
  display: grid;
  gap: 12px;
  border-bottom: 1px solid #edf1f5;
  padding: 0 0 18px;
  margin-bottom: 18px;
}

.detail-section:last-child {
  border-bottom: 0;
  margin-bottom: 0;
}

.detail-section h3 {
  margin: 0;
  color: #182233;
  font-size: 15px;
}

.detail-list {
  display: grid;
  gap: 10px;
  margin: 0;
}

.detail-list div {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 12px;
}

.detail-list dt {
  color: #69778d;
  font-size: 13px;
}

.detail-list dd {
  min-width: 0;
  margin: 0;
  color: #263143;
}

.monospace-text {
  overflow: hidden;
  font-family: ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

pre {
  min-height: 72px;
  max-height: 220px;
  overflow: auto;
  border: 1px solid #dce3ed;
  border-radius: 8px;
  background: #f8fafc;
  color: #263143;
  font-family: ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  line-height: 1.6;
  margin: 0;
  padding: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
