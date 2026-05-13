<template>
  <div class="permission-tree">
    <section
      v-for="group in groupedPermissions"
      :key="group.type"
      class="permission-group"
    >
      <header>
        <strong>{{ group.type }}</strong>
        <span>{{ group.items.length }} 项</span>
      </header>

      <el-checkbox-group v-model="selectedIds">
        <div
          v-for="permission in group.items"
          :key="permission.id"
          class="permission-row"
        >
          <el-checkbox :label="permission.id">
            <span>{{ permission.name }}</span>
            <code>{{ permission.code }}</code>
          </el-checkbox>
          <RiskLevelTag :permission="permission" />
        </div>
      </el-checkbox-group>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { Permission, PermissionType } from '@/features/auth/types'
import RiskLevelTag from './RiskLevelTag.vue'

const props = defineProps<{
  permissions: Permission[]
  modelValue: string[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

const selectedIds = computed({
  get: () => props.modelValue,
  set: (value: string[]) => emit('update:modelValue', value),
})

const groupedPermissions = computed(() => {
  const groups = new Map<PermissionType, Permission[]>()

  for (const permission of props.permissions) {
    const items = groups.get(permission.type) ?? []
    items.push(permission)
    groups.set(permission.type, items)
  }

  return [...groups.entries()].map(([type, items]) => ({
    type,
    items: [...items].sort((left: Permission, right: Permission) => left.code.localeCompare(right.code)),
  }))
})
</script>

<style scoped>
.permission-tree {
  display: grid;
  gap: 14px;
}

.permission-group {
  border: 1px solid #d8dee8;
  border-radius: 8px;
  overflow: hidden;
}

.permission-group header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f6f8fb;
  padding: 10px 12px;
}

.permission-group header span {
  color: #69778d;
  font-size: 13px;
}

.permission-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-top: 1px solid #eef1f5;
  padding: 10px 12px;
}

.permission-row code {
  margin-left: 8px;
  color: #69778d;
  font-size: 12px;
}
</style>
