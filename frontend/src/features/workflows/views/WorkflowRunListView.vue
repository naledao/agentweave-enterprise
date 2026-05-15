<template>
  <section class="workflow-run-list-view">
    <PageHeader
      title="工作流运行"
      description="查看 Agent 工作流执行路径、状态、耗时、错误摘要和 traceId。"
      eyebrow="Workflows"
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
        <el-select
          v-model="statusInput"
          clearable
          placeholder="状态"
          style="width: 160px"
          @change="searchRuns"
          @clear="searchRuns"
        >
          <el-option label="已创建" value="CREATED" />
          <el-option label="规划中" value="PLANNING" />
          <el-option label="执行中" value="EXECUTING" />
          <el-option label="等待审批" value="WAITING_APPROVAL" />
          <el-option label="复核中" value="REVIEWING" />
          <el-option label="成功" value="SUCCEEDED" />
          <el-option label="失败" value="FAILED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>

        <el-button @click="searchRuns">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </div>

    <div class="page-surface workflow-table-surface">
      <WorkflowRunTable
        class="workflow-table"
        :loading="runsQuery.isFetching.value"
        :runs="runs"
        @open="openRun"
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
  </section>
</template>

<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'
import { useQuery } from '@tanstack/vue-query'
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { workflowsApi } from '@/features/workflows/api/workflowsApi'
import WorkflowRunTable from '@/features/workflows/components/WorkflowRunTable.vue'
import type { WorkflowRun, WorkflowRunStatus } from '@/features/workflows/types'
import PageHeader from '@/shared/components/PageHeader.vue'
import TraceIdText from '@/shared/components/TraceIdText.vue'
import { getApiErrorDisplay } from '@/shared/utils/apiError'

const router = useRouter()
const page = ref(1)
const size = ref(20)
const statusInput = ref<WorkflowRunStatus | ''>('')
const appliedStatus = ref<WorkflowRunStatus | ''>('')

const runsQuery = useQuery({
  queryKey: computed(() => [
    'workflow-runs',
    {
      page: page.value - 1,
      size: size.value,
      status: appliedStatus.value || undefined,
    },
  ]),
  queryFn: () => workflowsApi.listWorkflowRuns({
    page: page.value - 1,
    size: size.value,
    status: appliedStatus.value || undefined,
  }),
})

const runs = computed(() => runsQuery.data.value?.items ?? [])
const total = computed(() => runsQuery.data.value?.total ?? 0)
const listError = computed(() => {
  if (!runsQuery.isError.value) {
    return { message: '', traceId: null }
  }

  return getApiErrorDisplay(runsQuery.error.value, '工作流运行列表加载失败')
})

watch(size, () => {
  page.value = 1
})

function searchRuns(): void {
  appliedStatus.value = statusInput.value
  page.value = 1
}

function resetFilters(): void {
  statusInput.value = ''
  searchRuns()
}

async function openRun(run: WorkflowRun): Promise<void> {
  await router.push({ name: 'WorkflowRunDetail', params: { runId: run.runId } })
}
</script>

<style scoped>
.workflow-run-list-view {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
}

.page-error,
.page-toolbar {
  flex: none;
}

.page-error {
  margin-bottom: 16px;
}

.workflow-table-surface {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
}

.workflow-table {
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
