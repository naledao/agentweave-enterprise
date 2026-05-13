<template>
  <el-table
    v-loading="loading"
    :data="roles"
    :height="height"
    row-key="id"
    empty-text="暂无角色"
  >
    <el-table-column prop="code" label="编码" min-width="140" />
    <el-table-column prop="name" label="名称" min-width="160" />
    <el-table-column prop="description" label="描述" min-width="240">
      <template #default="{ row }: { row: Role }">
        <span>{{ row.description || '-' }}</span>
      </template>
    </el-table-column>
    <el-table-column label="状态" width="100">
      <template #default="{ row }: { row: Role }">
        <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" effect="plain">
          {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="权限" min-width="220">
      <template #default="{ row }: { row: Role }">
        <div class="permission-summary">
          <el-tag
            v-for="permission in row.permissions.slice(0, 3)"
            :key="permission.id"
            effect="plain"
          >
            {{ permission.code }}
          </el-tag>
          <span v-if="row.permissions.length > 3" class="muted-text">
            +{{ row.permissions.length - 3 }}
          </span>
          <span v-if="row.permissions.length === 0" class="muted-text">未分配</span>
        </div>
      </template>
    </el-table-column>
    <el-table-column label="操作" width="270" fixed="right">
      <template #default="{ row }: { row: Role }">
        <div class="table-actions">
          <el-button
            v-if="canWrite"
            size="small"
            @click="$emit('edit', row)"
          >
            编辑
          </el-button>
          <el-button
            v-if="canWrite"
            size="small"
            @click="$emit('assignPermissions', row)"
          >
            权限
          </el-button>
          <el-button
            v-if="canWrite"
            :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
            size="small"
            @click="$emit('toggleStatus', row)"
          >
            {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
          </el-button>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import type { Role } from '@/features/auth/types'

defineProps<{
  roles: Role[]
  loading?: boolean
  canWrite?: boolean
  height?: string | number
}>()

defineEmits<{
  edit: [role: Role]
  assignPermissions: [role: Role]
  toggleStatus: [role: Role]
}>()
</script>

<style scoped>
.permission-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
