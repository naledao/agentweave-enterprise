<template>
  <el-table
    v-loading="loading"
    class="audit-log-table"
    :data="logs"
    height="100%"
    row-key="id"
    empty-text="暂无审计日志"
  >
    <el-table-column label="结果" width="95">
      <template #default="{ row }: { row: AuditLog }">
        <el-tag :type="resultType(row.result)" effect="plain">{{ resultLabel(row.result) }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="事件" min-width="150">
      <template #default="{ row }: { row: AuditLog }">{{ eventLabel(row.eventType) }}</template>
    </el-table-column>
    <el-table-column label="资源" min-width="180">
      <template #default="{ row }: { row: AuditLog }">
        <span class="main-text">{{ row.resourceType || '-' }}</span>
        <span class="sub-text">{{ row.resourceId || '-' }}</span>
      </template>
    </el-table-column>
    <el-table-column label="用户" min-width="140">
      <template #default="{ row }: { row: AuditLog }">
        <span class="main-text">{{ row.username || '-' }}</span>
        <span class="sub-text">{{ row.userId || '-' }}</span>
      </template>
    </el-table-column>
    <el-table-column label="耗时" width="100" align="right">
      <template #default="{ row }: { row: AuditLog }">{{ formatDuration(row.durationMs) }}</template>
    </el-table-column>
    <el-table-column label="创建时间" min-width="170">
      <template #default="{ row }: { row: AuditLog }">{{ formatDateTime(row.createdAt) }}</template>
    </el-table-column>
    <el-table-column label="摘要" min-width="240">
      <template #default="{ row }: { row: AuditLog }">
        <span class="summary-text">{{ row.errorMessage || row.responseSummary || row.requestSummary || '-' }}</span>
      </template>
    </el-table-column>
    <el-table-column label="traceId" min-width="230">
      <template #default="{ row }: { row: AuditLog }">
        <TraceIdText v-if="row.traceId" :trace-id="row.traceId" />
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import type { AuditEventType, AuditLog, AuditResult } from '@/features/observability/types'
import TraceIdText from '@/shared/components/TraceIdText.vue'

defineProps<{
  logs: AuditLog[]
  loading?: boolean
}>()

function resultType(result: AuditResult): 'success' | 'warning' | 'danger' | 'info' {
  const normalized = result.toUpperCase()
  if (normalized === 'SUCCESS') {
    return 'success'
  }
  if (normalized === 'FAILED' || normalized === 'DENIED') {
    return 'danger'
  }
  return 'info'
}

function resultLabel(result: AuditResult): string {
  const map: Record<string, string> = {
    SUCCESS: '成功',
    FAILED: '失败',
    DENIED: '拒绝',
  }
  return map[result.toUpperCase()] ?? result
}

function eventLabel(eventType: AuditEventType): string {
  const map: Record<string, string> = {
    LOGIN: '登录',
    LOGOUT: '退出',
    TOOL_INVOCATION: '工具调用',
    TOOL_PERMISSION_DENIED: '工具拒绝',
    WORKFLOW_RUN: '工作流运行',
    USER_MANAGEMENT: '用户管理',
    ROLE_MANAGEMENT: '角色管理',
  }
  return map[eventType] ?? eventType
}

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

  return `${Math.round(value)} ms`
}
</script>

<style scoped>
.audit-log-table {
  width: 100%;
}

.main-text,
.sub-text,
.summary-text {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.main-text {
  color: #263143;
}

.sub-text,
.summary-text {
  color: #69778d;
  font-size: 12px;
}
</style>
