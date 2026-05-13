<template>
  <section>
    <PageHeader
      title="角色管理"
      description="维护角色、权限集合和高风险能力分配。"
      eyebrow="Settings"
    >
      <template #actions>
        <el-button
          v-if="canWrite"
          type="primary"
          @click="openCreate"
        >
          <el-icon><Plus /></el-icon>
          创建角色
        </el-button>
      </template>
    </PageHeader>

    <div class="page-surface role-table-surface">
      <RoleTable
        class="role-table"
        :roles="roles"
        :loading="rolesQuery.isFetching.value"
        :can-write="canWrite"
        height="100%"
        @edit="openEdit"
        @assign-permissions="openPermissions"
        @toggle-status="toggleStatus"
      />

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50]"
          :total="totalRoles"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </div>

    <RoleFormDrawer
      v-model="formDrawerOpen"
      :role="editingRole"
      :permissions="permissions"
      :loading="saveRoleMutation.isPending.value"
      :error="drawerError"
      @submit="submitRole"
    />

    <RolePermissionDrawer
      v-model="permissionDrawerOpen"
      :role="editingRole"
      :permissions="permissions"
      :loading="updatePermissionsMutation.isPending.value"
      :error="drawerError"
      @submit="submitPermissions"
    />
  </section>
</template>

<script setup lang="ts">
import { Plus } from '@element-plus/icons-vue'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, ref, watch } from 'vue'

import { useAuthStore } from '@/features/auth/store/useAuthStore'
import type { Role, RoleStatus } from '@/features/auth/types'
import { rolesApi } from '@/features/settings/api/rolesApi'
import RoleFormDrawer from '@/features/settings/components/RoleFormDrawer.vue'
import RolePermissionDrawer from '@/features/settings/components/RolePermissionDrawer.vue'
import RoleTable from '@/features/settings/components/RoleTable.vue'
import PageHeader from '@/shared/components/PageHeader.vue'
import { formatApiError } from '@/shared/utils/apiError'
import { getTotalElements } from '@/shared/utils/pageResponse'

interface RoleFormPayload {
  code: string
  name: string
  description: string
  permissionIds: string[]
}

const authStore = useAuthStore()
const queryClient = useQueryClient()

const formDrawerOpen = ref(false)
const permissionDrawerOpen = ref(false)
const editingRole = ref<Role | null>(null)
const drawerError = ref('')
const page = ref(1)
const size = ref(10)

const canWrite = computed(() => authStore.hasPermission('auth:role:write'))

const rolesQuery = useQuery({
  queryKey: computed(() => [
    'roles',
    'page',
    {
      page: page.value - 1,
      size: size.value,
    },
  ]),
  queryFn: () =>
    rolesApi.queryRolePage({
      page: page.value - 1,
      size: size.value,
    }),
})

const permissionsQuery = useQuery({
  queryKey: ['permissions'],
  queryFn: rolesApi.queryPermissions,
})

const roles = computed(() => rolesQuery.data.value?.content ?? [])
const totalRoles = computed(() => getTotalElements(rolesQuery.data.value))
const permissions = computed(() => permissionsQuery.data.value ?? [])

const saveRoleMutation = useMutation({
  mutationFn: async (payload: RoleFormPayload) => {
    if (editingRole.value) {
      return rolesApi.updateRole(editingRole.value.id, {
        name: payload.name,
        description: payload.description || undefined,
      })
    }

    return rolesApi.createRole({
      code: payload.code,
      name: payload.name,
      description: payload.description || undefined,
      permissionIds: payload.permissionIds,
    })
  },
  onSuccess: async () => {
    formDrawerOpen.value = false
    drawerError.value = ''
    ElMessage.success('角色已保存')
    await queryClient.invalidateQueries({ queryKey: ['roles'] })
  },
  onError: (error) => {
    drawerError.value = formatApiError(error)
  },
})

const updatePermissionsMutation = useMutation({
  mutationFn: (permissionIds: string[]) => {
    if (!editingRole.value) {
      throw new Error('missing role')
    }

    return rolesApi.updateRolePermissions(editingRole.value.id, { permissionIds })
  },
  onSuccess: async () => {
    permissionDrawerOpen.value = false
    drawerError.value = ''
    ElMessage.success('权限已更新')
    await queryClient.invalidateQueries({ queryKey: ['roles'] })
    await queryClient.invalidateQueries({ queryKey: ['permissions'] })
  },
  onError: (error) => {
    drawerError.value = formatApiError(error)
  },
})

const updateStatusMutation = useMutation({
  mutationFn: ({ role, status }: { role: Role; status: RoleStatus }) =>
    rolesApi.updateRoleStatus(role.id, { status }),
  onSuccess: async () => {
    ElMessage.success('角色状态已更新')
    await queryClient.invalidateQueries({ queryKey: ['roles'] })
  },
  onError: (error) => {
    ElMessage.error(formatApiError(error))
  },
})

watch(size, () => {
  page.value = 1
})

function openCreate(): void {
  editingRole.value = null
  drawerError.value = ''
  formDrawerOpen.value = true
}

function openEdit(role: Role): void {
  editingRole.value = role
  drawerError.value = ''
  formDrawerOpen.value = true
}

function openPermissions(role: Role): void {
  editingRole.value = role
  drawerError.value = ''
  permissionDrawerOpen.value = true
}

function submitRole(payload: RoleFormPayload): void {
  saveRoleMutation.mutate(payload)
}

function submitPermissions(permissionIds: string[]): void {
  updatePermissionsMutation.mutate(permissionIds)
}

async function toggleStatus(role: Role): Promise<void> {
  const status: RoleStatus = role.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await ElMessageBox.confirm(
    `确认${status === 'ACTIVE' ? '启用' : '停用'}角色 ${role.name}？`,
    '更新角色状态',
    { type: 'warning' },
  )
  updateStatusMutation.mutate({ role, status })
}
</script>

<style scoped>
section {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
}

.role-table-surface {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
}

.role-table {
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
</style>
