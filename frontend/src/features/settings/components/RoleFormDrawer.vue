<template>
  <el-drawer
    :model-value="modelValue"
    :title="role ? '编辑角色' : '创建角色'"
    size="460px"
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
      <el-form-item label="角色编码" prop="code">
        <el-input
          v-model="form.code"
          :disabled="Boolean(role)"
          maxlength="80"
          placeholder="例如 OPS_ADMIN"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="角色名称" prop="name">
        <el-input
          v-model="form.name"
          maxlength="120"
          placeholder="请输入角色名称"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="描述" prop="description">
        <el-input
          v-model="form.description"
          maxlength="500"
          :rows="4"
          placeholder="请输入角色描述"
          show-word-limit
          type="textarea"
        />
      </el-form-item>

      <el-form-item v-if="!role" label="权限" prop="permissionIds">
        <PermissionTree
          v-model="form.permissionIds"
          :permissions="permissions"
        />
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

import type { Permission, Role } from '@/features/auth/types'
import PermissionTree from './PermissionTree.vue'

interface RoleFormModel {
  code: string
  name: string
  description: string
  permissionIds: string[]
}

const props = defineProps<{
  modelValue: boolean
  role: Role | null
  permissions: Permission[]
  loading?: boolean
  error?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [form: RoleFormModel]
}>()

const formRef = ref<FormInstance>()
const form = reactive<RoleFormModel>({
  code: '',
  name: '',
  description: '',
  permissionIds: [],
})

const rules: FormRules<RoleFormModel> = {
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { max: 80, message: '角色编码最多 80 个字符', trigger: 'blur' },
  ],
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { max: 120, message: '角色名称最多 120 个字符', trigger: 'blur' },
  ],
  description: [{ max: 500, message: '描述最多 500 个字符', trigger: 'blur' }],
  permissionIds: [{ required: true, type: 'array', min: 1, message: '请选择至少一个权限', trigger: 'change' }],
}

watch(
  () => [props.modelValue, props.role] as const,
  ([open, role]) => {
    if (!open) {
      return
    }

    form.code = role?.code ?? ''
    form.name = role?.name ?? ''
    form.description = role?.description ?? ''
    form.permissionIds = role?.permissions.map((permission) => permission.id) ?? []
    formRef.value?.clearValidate()
  },
  { immediate: true },
)

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  emit('submit', { ...form, permissionIds: [...form.permissionIds] })
}

function close(): void {
  emit('update:modelValue', false)
}
</script>
