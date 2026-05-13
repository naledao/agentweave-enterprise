<template>
  <el-drawer
    :model-value="modelValue"
    title="分配角色"
    size="420px"
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

    <div v-if="user" class="user-summary">
      <strong>{{ user.displayName }}</strong>
      <span>{{ user.username }}</span>
    </div>

    <el-checkbox-group v-model="selectedRoleIds" class="role-checks">
      <el-checkbox
        v-for="role in roles"
        :key="role.id"
        :label="role.id"
        border
      >
        {{ role.name }} ({{ role.code }})
      </el-checkbox>
    </el-checkbox-group>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="loading" @click="submit">保存</el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

import type { Role, UserProfile } from '@/features/auth/types'

const props = defineProps<{
  modelValue: boolean
  user: UserProfile | null
  roles: Role[]
  loading?: boolean
  error?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [roleIds: string[]]
}>()

const selectedRoleIds = ref<string[]>([])

watch(
  () => [props.modelValue, props.user, props.roles] as const,
  ([open, user, roles]) => {
    if (!open || !user) {
      selectedRoleIds.value = []
      return
    }

    selectedRoleIds.value = roles
      .filter((role) => user.roles.includes(role.code))
      .map((role) => role.id)
  },
  { immediate: true },
)

function submit(): void {
  emit('submit', [...selectedRoleIds.value])
}

function close(): void {
  emit('update:modelValue', false)
}
</script>

<style scoped>
.user-summary {
  display: grid;
  gap: 4px;
  margin-bottom: 18px;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #f7f9fc;
  padding: 12px;
}

.user-summary span {
  color: #69778d;
  font-size: 13px;
}

.role-checks {
  display: grid;
  gap: 10px;
}

.role-checks :deep(.el-checkbox.is-bordered) {
  width: 100%;
  margin-right: 0;
}
</style>
