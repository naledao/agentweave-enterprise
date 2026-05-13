<template>
  <el-drawer
    :model-value="modelValue"
    :append-to-body="false"
    :destroy-on-close="false"
    title="重置密码"
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

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
    >
      <el-form-item label="新密码" prop="password">
        <el-input
          v-model="form.password"
          maxlength="128"
          placeholder="请输入新密码"
          show-password
          type="password"
        />
      </el-form-item>

      <el-form-item label="确认密码" prop="confirmPassword" :error="confirmPasswordError">
        <el-input
          v-model="form.confirmPassword"
          maxlength="128"
          placeholder="请再次输入新密码"
          show-password
          type="password"
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

import type { UserProfile } from '@/features/auth/types'

interface PasswordFormModel {
  password: string
  confirmPassword: string
}

const props = defineProps<{
  modelValue: boolean
  user: UserProfile | null
  loading?: boolean
  error?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [password: string]
}>()

const formRef = ref<FormInstance>()
const confirmPasswordError = ref('')
const form = reactive<PasswordFormModel>({
  password: '',
  confirmPassword: '',
})

const rules: FormRules<PasswordFormModel> = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 128, message: '密码长度为 8 到 128 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
  ],
}

watch(
  () => [props.modelValue, props.user] as const,
  ([open]) => {
    if (!open) {
      return
    }

    form.password = ''
    form.confirmPassword = ''
    confirmPasswordError.value = ''
    formRef.value?.clearValidate()
  },
  { immediate: true },
)

async function submit(): Promise<void> {
  confirmPasswordError.value = ''
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  if (form.confirmPassword !== form.password) {
    confirmPasswordError.value = '两次输入的密码不一致'
    return
  }

  emit('submit', form.password)
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
</style>
