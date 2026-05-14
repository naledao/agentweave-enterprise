<template>
  <el-table
    v-loading="loading"
    :data="tools"
    :height="height"
    row-key="id"
    empty-text="暂无工具定义"
  >
    <el-table-column prop="name" label="名称" min-width="150" />
    <el-table-column prop="code" label="编码" min-width="170" />
    <el-table-column label="风险等级" width="120">
      <template #default="{ row }: { row: ToolDefinition }">
        <ToolRiskTag :risk-level="row.riskLevel" />
      </template>
    </el-table-column>
    <el-table-column label="启用状态" width="110">
      <template #default="{ row }: { row: ToolDefinition }">
        <el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">
          {{ row.enabled ? '启用' : '停用' }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="可用性" width="120">
      <template #default="{ row }: { row: ToolDefinition }">
        <el-tag :type="row.available ? 'success' : 'danger'" effect="plain">
          {{ row.available ? '可用' : '不可用' }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="permissionCode" label="权限编码" min-width="190" />
    <el-table-column label="描述" min-width="260">
      <template #default="{ row }: { row: ToolDefinition }">
        <span class="description-text">{{ row.description || '-' }}</span>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import ToolRiskTag from '@/features/tools/components/ToolRiskTag.vue'
import type { ToolDefinition } from '@/features/tools/types'

defineProps<{
  tools: ToolDefinition[]
  loading?: boolean
  height?: string | number
}>()
</script>

<style scoped>
.description-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #4f5f75;
  text-overflow: ellipsis;
  vertical-align: bottom;
  white-space: nowrap;
}
</style>
