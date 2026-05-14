<template>
  <section class="tool-center-view">
    <PageHeader
      title="工具中心"
      description="查看平台可用工具、风险等级、启用状态和当前账号可用性。"
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

    <div class="tool-summary">
      <div class="summary-item">
        <span>工具总数</span>
        <strong>{{ totalCount }}</strong>
      </div>
      <div class="summary-item">
        <span>可用</span>
        <strong>{{ availableCount }}</strong>
      </div>
      <div class="summary-item">
        <span>中高风险</span>
        <strong>{{ guardedCount }}</strong>
      </div>
      <el-button :loading="toolsQuery.isFetching.value" @click="refreshTools">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <div class="page-surface tool-table-surface">
      <ToolDefinitionTable
        class="tool-table"
        :tools="tools"
        :loading="toolsQuery.isFetching.value"
        height="100%"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue'
import { useQuery } from '@tanstack/vue-query'
import { computed } from 'vue'

import { toolsApi } from '@/features/tools/api/toolsApi'
import ToolDefinitionTable from '@/features/tools/components/ToolDefinitionTable.vue'
import PageHeader from '@/shared/components/PageHeader.vue'
import TraceIdText from '@/shared/components/TraceIdText.vue'
import { getApiErrorDisplay } from '@/shared/utils/apiError'

const toolsQuery = useQuery({
  queryKey: ['tool-definitions'],
  queryFn: toolsApi.listDefinitions,
})

const tools = computed(() => toolsQuery.data.value ?? [])
const totalCount = computed(() => tools.value.length)
const availableCount = computed(() => tools.value.filter((tool) => tool.available).length)
const guardedCount = computed(
  () => tools.value.filter((tool) => tool.riskLevel === 'MEDIUM' || tool.riskLevel === 'HIGH').length,
)
const listError = computed(() => {
  if (!toolsQuery.isError.value) {
    return { message: '', traceId: null }
  }

  return getApiErrorDisplay(toolsQuery.error.value, '工具定义加载失败')
})

async function refreshTools(): Promise<void> {
  await toolsQuery.refetch()
}
</script>

<style scoped>
.tool-center-view {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
}

.page-error {
  flex: none;
  margin-bottom: 16px;
}

.tool-summary {
  display: flex;
  flex: none;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.summary-item {
  display: flex;
  min-width: 128px;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  border: 1px solid #dce3ed;
  border-radius: 8px;
  background: #fff;
  padding: 10px 12px;
}

.summary-item span {
  color: #66758c;
  font-size: 13px;
}

.summary-item strong {
  color: #182233;
  font-size: 18px;
}

.tool-table-surface {
  min-height: 0;
  flex: 1;
  overflow: hidden;
}

.tool-table {
  height: 100%;
}

@media (max-width: 820px) {
  .tool-summary {
    align-items: stretch;
    flex-wrap: wrap;
  }

  .summary-item {
    flex: 1 1 150px;
  }
}
</style>
