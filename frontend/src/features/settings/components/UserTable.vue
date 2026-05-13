<template>
  <el-table
    v-loading="loading"
    :data="users"
    row-key="id"
    empty-text="暂无用户"
  >
    <el-table-column prop="username" label="用户名" min-width="150" />
    <el-table-column prop="displayName" label="显示名称" min-width="160" />
    <el-table-column prop="email" label="邮箱" min-width="220">
      <template #default="{ row }: { row: UserProfile }">
        <span>{{ row.email || '-' }}</span>
      </template>
    </el-table-column>
    <el-table-column label="状态" width="100">
      <template #default="{ row }: { row: UserProfile }">
        <UserStatusTag :status="row.status" />
      </template>
    </el-table-column>
    <el-table-column label="角色" min-width="220">
      <template #default="{ row }: { row: UserProfile }">
        <div class="role-list">
          <el-tag
            v-for="role in row.roles"
            :key="role"
            effect="plain"
          >
            {{ role }}
          </el-tag>
          <span v-if="row.roles.length === 0" class="muted-text">未分配</span>
        </div>
      </template>
    </el-table-column>
    <el-table-column label="最近登录" min-width="180">
      <template #default="{ row }: { row: UserProfile }">
        <span>{{ formatDateTime(row.lastLoginAt) }}</span>
      </template>
    </el-table-column>
    <el-table-column label="操作" width="340" fixed="right">
      <template #default="{ row }: { row: UserProfile }">
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
            @click="$emit('assignRoles', row)"
          >
            角色
          </el-button>
          <el-button
            v-if="canWrite"
            size="small"
            @click="$emit('resetPassword', row)"
          >
            密码
          </el-button>
          <el-button
            v-if="canWrite"
            :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
            size="small"
            @click="$emit('toggleStatus', row)"
          >
            {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
          </el-button>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import type { UserProfile } from '@/features/auth/types'
import UserStatusTag from './UserStatusTag.vue'

defineProps<{
  users: UserProfile[]
  loading?: boolean
  canWrite?: boolean
}>()

defineEmits<{
  edit: [user: UserProfile]
  assignRoles: [user: UserProfile]
  resetPassword: [user: UserProfile]
  toggleStatus: [user: UserProfile]
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
</script>

<style scoped>
.role-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
