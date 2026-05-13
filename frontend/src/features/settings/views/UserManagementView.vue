<template>
  <section>
    <PageHeader
      title="用户管理"
      description="管理后台账号、状态和角色绑定。"
      eyebrow="Settings"
    >
      <template #actions>
        <el-button
          v-if="canWrite"
          type="primary"
          @click="openCreate"
        >
          <el-icon><Plus /></el-icon>
          创建用户
        </el-button>
      </template>
    </PageHeader>

    <div class="page-toolbar">
      <div class="toolbar-filters">
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="搜索用户名或显示名称"
          style="width: 240px"
          @keyup.enter="refetchUsers"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select
          v-model="filters.status"
          clearable
          placeholder="状态"
          style="width: 140px"
        >
          <el-option label="启用" value="ACTIVE" />
          <el-option label="禁用" value="DISABLED" />
          <el-option label="锁定" value="LOCKED" />
        </el-select>

        <el-button @click="refetchUsers">查询</el-button>
      </div>
    </div>

    <div class="page-surface">
      <UserTable
        :users="users"
        :loading="usersQuery.isFetching.value"
        :can-write="canWrite"
        @edit="openEdit"
        @assign-roles="openRoles"
        @reset-password="openPassword"
        @toggle-status="toggleStatus"
      />

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50]"
          :total="totalUsers"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </div>

    <UserFormDrawer
      v-model="formDrawerOpen"
      :user="editingUser"
      :roles="roles"
      :loading="saveUserMutation.isPending.value"
      :error="drawerError"
      @submit="submitUser"
    />

    <UserRoleDrawer
      v-model="roleDrawerOpen"
      :user="editingUser"
      :roles="roles"
      :loading="updateRolesMutation.isPending.value"
      :error="drawerError"
      @submit="submitRoles"
    />

    <UserPasswordDrawer
      v-model="passwordDrawerOpen"
      :user="editingUser"
      :loading="resetPasswordMutation.isPending.value"
      :error="drawerError"
      @submit="submitPassword"
    />
  </section>
</template>

<script setup lang="ts">
import { Plus, Search } from '@element-plus/icons-vue'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, reactive, ref, watch } from 'vue'

import { useAuthStore } from '@/features/auth/store/useAuthStore'
import type { UserProfile, UserStatus } from '@/features/auth/types'
import { rolesApi } from '@/features/settings/api/rolesApi'
import { usersApi } from '@/features/settings/api/usersApi'
import UserFormDrawer from '@/features/settings/components/UserFormDrawer.vue'
import UserPasswordDrawer from '@/features/settings/components/UserPasswordDrawer.vue'
import UserRoleDrawer from '@/features/settings/components/UserRoleDrawer.vue'
import UserTable from '@/features/settings/components/UserTable.vue'
import PageHeader from '@/shared/components/PageHeader.vue'
import { formatApiError } from '@/shared/utils/apiError'
import { getTotalElements } from '@/shared/utils/pageResponse'

interface UserFormPayload {
  username: string
  displayName: string
  email: string
  password: string
  roleIds: string[]
}

const authStore = useAuthStore()
const queryClient = useQueryClient()

const page = ref(1)
const size = ref(10)
const filters = reactive<{
  keyword: string
  status: UserStatus | ''
}>({
  keyword: '',
  status: '',
})
const formDrawerOpen = ref(false)
const roleDrawerOpen = ref(false)
const passwordDrawerOpen = ref(false)
const editingUser = ref<UserProfile | null>(null)
const drawerError = ref('')

const canWrite = computed(() => authStore.hasPermission('auth:user:write'))

const usersQuery = useQuery({
  queryKey: computed(() => [
    'users',
    {
      page: page.value - 1,
      size: size.value,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
    },
  ]),
  queryFn: () =>
    usersApi.queryUsers({
      page: page.value - 1,
      size: size.value,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
    }),
})

const rolesQuery = useQuery({
  queryKey: ['roles'],
  queryFn: rolesApi.queryRoles,
  enabled: canWrite,
})

const users = computed(() => usersQuery.data.value?.content ?? [])
const totalUsers = computed(() => getTotalElements(usersQuery.data.value))
const roles = computed(() => rolesQuery.data.value ?? [])

const saveUserMutation = useMutation({
  mutationFn: async (payload: UserFormPayload) => {
    if (editingUser.value) {
      return usersApi.updateUser(editingUser.value.id, {
        displayName: payload.displayName,
        email: payload.email || undefined,
      })
    }

    return usersApi.createUser({
      username: payload.username,
      displayName: payload.displayName,
      password: payload.password,
      email: payload.email || undefined,
      roleIds: payload.roleIds,
    })
  },
  onSuccess: async () => {
    formDrawerOpen.value = false
    drawerError.value = ''
    ElMessage.success('用户已保存')
    await queryClient.invalidateQueries({ queryKey: ['users'] })
  },
  onError: (error) => {
    drawerError.value = formatApiError(error)
  },
})

const updateRolesMutation = useMutation({
  mutationFn: (roleIds: string[]) => {
    if (!editingUser.value) {
      throw new Error('missing user')
    }

    return usersApi.updateUserRoles(editingUser.value.id, { roleIds })
  },
  onSuccess: async () => {
    roleDrawerOpen.value = false
    drawerError.value = ''
    ElMessage.success('角色已更新')
    await queryClient.invalidateQueries({ queryKey: ['users'] })
  },
  onError: (error) => {
    drawerError.value = formatApiError(error)
  },
})

const updateStatusMutation = useMutation({
  mutationFn: ({ user, status }: { user: UserProfile; status: UserStatus }) =>
    usersApi.updateUserStatus(user.id, { status }),
  onSuccess: async () => {
    ElMessage.success('用户状态已更新')
    await queryClient.invalidateQueries({ queryKey: ['users'] })
  },
  onError: (error) => {
    ElMessage.error(formatApiError(error))
  },
})

const resetPasswordMutation = useMutation({
  mutationFn: (password: string) => {
    if (!editingUser.value) {
      throw new Error('missing user')
    }

    return usersApi.resetPassword(editingUser.value.id, { password })
  },
  onSuccess: async () => {
    passwordDrawerOpen.value = false
    drawerError.value = ''
    ElMessage.success('密码已重置')
    await queryClient.invalidateQueries({ queryKey: ['users'] })
  },
  onError: (error) => {
    drawerError.value = formatApiError(error)
  },
})

watch([size, () => filters.status], () => {
  page.value = 1
})

function refetchUsers(): void {
  page.value = 1
  void usersQuery.refetch()
}

function openCreate(): void {
  editingUser.value = null
  drawerError.value = ''
  formDrawerOpen.value = true
}

function openEdit(user: UserProfile): void {
  editingUser.value = user
  drawerError.value = ''
  formDrawerOpen.value = true
}

function openRoles(user: UserProfile): void {
  editingUser.value = user
  drawerError.value = ''
  roleDrawerOpen.value = true
}

function openPassword(user: UserProfile): void {
  editingUser.value = user
  drawerError.value = ''
  passwordDrawerOpen.value = true
}

function submitUser(payload: UserFormPayload): void {
  saveUserMutation.mutate(payload)
}

function submitRoles(roleIds: string[]): void {
  updateRolesMutation.mutate(roleIds)
}

function submitPassword(password: string): void {
  resetPasswordMutation.mutate(password)
}

async function toggleStatus(user: UserProfile): Promise<void> {
  const status: UserStatus = user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await ElMessageBox.confirm(
    `确认${status === 'ACTIVE' ? '启用' : '禁用'}用户 ${user.displayName}？`,
    '更新用户状态',
    { type: 'warning' },
  )
  updateStatusMutation.mutate({ user, status })
}
</script>

<style scoped>
.pagination-row {
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #edf1f5;
  padding: 14px 16px;
}
</style>
