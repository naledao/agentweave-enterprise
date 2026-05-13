<template>
  <el-drawer
    :model-value="modelValue"
    title="分配权限"
    size="560px"
    @close="close"
  >
    <el-alert
      v-if="error"
      class="drawer-error"
      :title="error"
      type="error"
      :closable="false"
      show-icon
    />

    <div v-if="role" class="role-summary">
      <strong>{{ role.name }}</strong>
      <span>{{ role.code }}</span>
    </div>

    <PermissionTree
      v-model="selectedPermissionIds"
      :permissions="permissions"
    />

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="loading" @click="submit">保存</el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

import type { Permission, Role } from '@/features/auth/types'
import PermissionTree from './PermissionTree.vue'

const props = defineProps<{
  modelValue: boolean
  role: Role | null
  permissions: Permission[]
  loading?: boolean
  error?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [permissionIds: string[]]
}>()

const selectedPermissionIds = ref<string[]>([])

watch(
  () => [props.modelValue, props.role] as const,
  ([open, role]) => {
    if (!open || !role) {
      selectedPermissionIds.value = []
      return
    }

    selectedPermissionIds.value = role.permissions.map((permission) => permission.id)
  },
  { immediate: true },
)

function submit(): void {
  emit('submit', [...selectedPermissionIds.value])
}

function close(): void {
  emit('update:modelValue', false)
}
</script>

<style scoped>
.role-summary {
  display: grid;
  gap: 4px;
  margin-bottom: 18px;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #f7f9fc;
  padding: 12px;
}

.role-summary span {
  color: #69778d;
  font-size: 13px;
}
</style>
