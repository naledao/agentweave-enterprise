<template>
  <el-drawer
    :model-value="modelValue"
    :title="user ? '编辑用户' : '创建用户'"
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

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
    >
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="form.username"
          :disabled="Boolean(user)"
          maxlength="80"
          placeholder="例如 zhangsan"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="显示名称" prop="displayName">
        <el-input
          v-model="form.displayName"
          maxlength="120"
          placeholder="请输入显示名称"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="邮箱" prop="email">
        <el-input
          v-model="form.email"
          maxlength="255"
          placeholder="name@example.com"
        />
      </el-form-item>

      <el-form-item v-if="!user" label="初始密码" prop="password">
        <el-input
          v-model="form.password"
          maxlength="128"
          minlength="8"
          placeholder="至少 8 个字符"
          show-password
          type="password"
        />
      </el-form-item>

      <el-form-item v-if="!user" label="角色" prop="roleIds">
        <el-select
          v-model="form.roleIds"
          multiple
          placeholder="请选择角色"
          style="width: 100%"
        >
          <el-option
            v-for="role in roles"
            :key="role.id"
            :label="`${role.name} (${role.code})`"
            :value="role.id"
          />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="loading" @click="submit">保存</el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { reactive, ref, watch } from 'vue'

import type { Role, UserProfile } from '@/features/auth/types'

interface UserFormModel {
  username: string
  displayName: string
  email: string
  password: string
  roleIds: string[]
}

const props = defineProps<{
  modelValue: boolean
  user: UserProfile | null
  roles: Role[]
  loading?: boolean
  error?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [form: UserFormModel]
}>()

const formRef = ref<FormInstance>()
const form = reactive<UserFormModel>({
  username: '',
  displayName: '',
  email: '',
  password: '',
  roleIds: [],
})

const rules: FormRules<UserFormModel> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 80, message: '用户名长度为 3 到 80 个字符', trigger: 'blur' },
  ],
  displayName: [
    { required: true, message: '请输入显示名称', trigger: 'blur' },
    { max: 120, message: '显示名称最多 120 个字符', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 8, max: 128, message: '密码长度为 8 到 128 个字符', trigger: 'blur' },
  ],
  roleIds: [{ required: true, type: 'array', min: 1, message: '请选择至少一个角色', trigger: 'change' }],
}

watch(
  () => [props.modelValue, props.user] as const,
  ([open, user]) => {
    if (!open) {
      return
    }

    form.username = user?.username ?? ''
    form.displayName = user?.displayName ?? ''
    form.email = user?.email ?? ''
    form.password = ''
    form.roleIds = []
    formRef.value?.clearValidate()
  },
  { immediate: true },
)

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  emit('submit', { ...form, roleIds: [...form.roleIds] })
}

function close(): void {
  emit('update:modelValue', false)
}
</script>
