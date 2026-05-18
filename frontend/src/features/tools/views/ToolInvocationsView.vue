<template>
  <section class="tool-invocations-view">
    <PageHeader
      title="工具调用记录"
      description="查看工具执行状态、耗时、错误摘要和 traceId。"
      eyebrow="Tools"
    />

    <el-alert
      v-if="listError.message"
      class="page-error"
      :title="listError.message"
      type="error"
      :closable="false"
      show-icon
    >
      <TraceIdText v-if="listError.traceId" :trace-id="listError.traceId" />
    </el-alert>

    <div class="page-toolbar">
      <div class="toolbar-filters">
        <el-input
          v-model="filterForm.toolCode"
          clearable
          placeholder="工具编码"
          style="width: 220px"
          @keyup.enter="searchInvocations"
          @clear="searchInvocations"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select
          v-model="filterForm.toolType"
          clearable
          placeholder="工具类型"
          style="width: 150px"
          @change="searchInvocations"
          @clear="searchInvocations"
        >
          <el-option label="业务查询" value="BUSINESS_QUERY" />
          <el-option label="日志检索" value="LOG_SEARCH" />
          <el-option label="数据库只读" value="DATABASE_READ" />
          <el-option label="接口状态" value="ENDPOINT_STATUS" />
          <el-option label="消息通知" value="NOTIFICATION" />
          <el-option label="MCP 资源" value="MCP_RESOURCE" />
          <el-option label="脚本" value="SCRIPT" />
          <el-option label="未知" value="UNKNOWN" />
        </el-select>

        <el-select
          v-model="filterForm.status"
          clearable
          placeholder="状态"
          style="width: 140px"
          @change="searchInvocations"
          @clear="searchInvocations"
        >
          <el-option label="执行中" value="running" />
          <el-option label="成功" value="success" />
          <el-option label="失败" value="failed" />
          <el-option label="拒绝" value="denied" />
          <el-option label="超时" value="timeout" />
        </el-select>

        <el-date-picker
          v-model="filterForm.createdRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          style="width: 360px"
          @change="searchInvocations"
        />

        <el-button @click="searchInvocations">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </div>

    <ToolInvocationSummaryPanel
      :summary="invocationSummary"
      :loading="summaryQuery.isFetching.value"
    />

    <div class="page-surface invocation-table-surface">
      <ToolInvocationTable
        class="invocation-table"
        :invocations="invocations"
        :loading="summaryQuery.isFetching.value"
        @open="openInvocation"
      />

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </div>

    <ToolInvocationDetailDrawer
      v-model="detailDrawerVisible"
      :invocation="invocationDetail"
      :loading="invocationDetailQuery.isFetching.value"
      :error="detailError"
    />
  </section>
</template>

<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'
import { useQuery } from '@tanstack/vue-query'
import { computed, reactive, ref, watch } from 'vue'

import { toolsApi } from '@/features/tools/api/toolsApi'
import ToolInvocationDetailDrawer from '@/features/tools/components/ToolInvocationDetailDrawer.vue'
import ToolInvocationSummaryPanel from '@/features/tools/components/ToolInvocationSummaryPanel.vue'
import ToolInvocationTable from '@/features/tools/components/ToolInvocationTable.vue'
import type { ToolInvocation, ToolInvocationStatus, ToolType } from '@/features/tools/types'
import PageHeader from '@/shared/components/PageHeader.vue'
import TraceIdText from '@/shared/components/TraceIdText.vue'
import { getApiErrorDisplay } from '@/shared/utils/apiError'

type DateRange = [Date, Date] | null

const page = ref(1)
const size = ref(20)
const selectedInvocationId = ref('')
const detailDrawerVisible = ref(false)

const filterForm = reactive<{
  toolCode: string
  toolType: ToolType | ''
  status: ToolInvocationStatus | ''
  createdRange: DateRange
}>({
  toolCode: '',
  toolType: '',
  status: '',
  createdRange: null,
})

const appliedFilters = reactive<{
  toolCode: string
  toolType: ToolType | ''
  status: ToolInvocationStatus | ''
  createdRange: DateRange
}>({
  toolCode: '',
  toolType: '',
  status: '',
  createdRange: null,
})

const queryParams = computed(() => ({
  page: page.value - 1,
  size: size.value,
  toolCode: appliedFilters.toolCode || undefined,
  toolType: appliedFilters.toolType || undefined,
  status: appliedFilters.status || undefined,
  createdFrom: toIso(appliedFilters.createdRange?.[0] ?? null),
  createdTo: toIso(appliedFilters.createdRange?.[1] ?? null),
}))

const summaryQuery = useQuery({
  queryKey: computed(() => ['tool-invocation-summary', queryParams.value]),
  queryFn: () => toolsApi.getInvocationSummary(queryParams.value),
})

const invocationDetailQuery = useQuery({
  queryKey: computed(() => ['tool-invocation-detail', selectedInvocationId.value]),
  queryFn: () => toolsApi.getInvocation(selectedInvocationId.value),
  enabled: computed(() => Boolean(selectedInvocationId.value) && detailDrawerVisible.value),
})

const invocationSummary = computed(() => summaryQuery.data.value ?? null)
const invocations = computed(() => invocationSummary.value?.invocations.items ?? [])
const total = computed(() => invocationSummary.value?.invocations.total ?? 0)
const invocationDetail = computed(() => invocationDetailQuery.data.value ?? null)
const listError = computed(() => {
  if (!summaryQuery.isError.value) {
    return { message: '', traceId: null }
  }

  return getApiErrorDisplay(summaryQuery.error.value, '工具调用记录加载失败')
})
const detailError = computed(() => {
  if (!invocationDetailQuery.isError.value) {
    return { message: '', traceId: null }
  }

  return getApiErrorDisplay(invocationDetailQuery.error.value, '工具调用详情加载失败')
})

watch(size, () => {
  page.value = 1
})

watch(detailDrawerVisible, (visible) => {
  if (!visible) {
    selectedInvocationId.value = ''
  }
})

function searchInvocations(): void {
  appliedFilters.toolCode = filterForm.toolCode.trim()
  appliedFilters.toolType = filterForm.toolType
  appliedFilters.status = filterForm.status
  appliedFilters.createdRange = filterForm.createdRange
  page.value = 1
}

function resetFilters(): void {
  filterForm.toolCode = ''
  filterForm.toolType = ''
  filterForm.status = ''
  filterForm.createdRange = null
  searchInvocations()
}

function openInvocation(invocation: ToolInvocation): void {
  selectedInvocationId.value = invocation.id
  detailDrawerVisible.value = true
}

function toIso(value: Date | null): string | undefined {
  return value ? value.toISOString() : undefined
}
</script>

<style scoped>
.tool-invocations-view {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
}

.page-error {
  flex: none;
  margin-bottom: 16px;
}

.invocation-table-surface {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
}

.invocation-table {
  min-height: 0;
  flex: 1;
}

.pagination-row {
  display: flex;
  flex: none;
  justify-content: flex-end;
  border-top: 1px solid #edf1f5;
  padding: 14px 16px;
}

@media (max-width: 820px) {
  .toolbar-filters > * {
    width: 100% !important;
  }

  .pagination-row {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
