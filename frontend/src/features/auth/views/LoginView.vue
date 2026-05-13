<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="brand-row">
        <div class="brand-mark">AW</div>
        <div>
          <h1>AgentWeave Enterprise</h1>
          <p>企业级 Agent 编排平台</p>
        </div>
      </div>

      <el-alert
        v-if="expired"
        class="login-alert"
        title="登录状态已过期，请重新登录。"
        type="warning"
        :closable="false"
        show-icon
      />

      <el-alert
        v-if="errorMessage"
        class="login-alert"
        :title="errorMessage"
        type="error"
        :closable="false"
        show-icon
      />

      <el-form
        ref="formRef"
        class="login-form"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="submit"
      >
        <el-form-item label="用户名" prop="username" :error="fieldErrors.username">
          <el-input
            v-model="form.username"
            autocomplete="username"
            placeholder="请输入用户名"
            size="large"
            @input="fieldErrors.username = ''"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password" :error="fieldErrors.password">
          <el-input
            v-model="form.password"
            autocomplete="current-password"
            placeholder="请输入密码"
            show-password
            size="large"
            type="password"
            @input="fieldErrors.password = ''"
          />
        </el-form-item>

        <el-button
          class="submit-button"
          :loading="authStore.loading"
          native-type="submit"
          size="large"
          type="primary"
        >
          登录
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/features/auth/store/useAuthStore'
import type { LoginRequest } from '@/features/auth/types'
import { formatApiError } from '@/shared/utils/apiError'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const form = reactive<LoginRequest>({
  username: '',
  password: '',
})
const fieldErrors = reactive({
  username: '',
  password: '',
})
const errorMessage = ref('')
const expired = computed(() => route.query.reason === 'expired')

const rules: FormRules<LoginRequest> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 64, message: '用户名长度为 3 到 64 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 128, message: '密码长度为 8 到 128 个字符', trigger: 'blur' },
  ],
}

async function submit(): Promise<void> {
  errorMessage.value = ''
  fieldErrors.username = ''
  fieldErrors.password = ''

  if (!form.username.trim() || !form.password) {
    fieldErrors.username = form.username.trim() ? '' : '请输入用户名'
    fieldErrors.password = form.password ? '' : '请输入密码'
    return
  }

  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  try {
    await authStore.login(form)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/app/chat'
    await router.replace(redirect)
  } catch (error) {
    errorMessage.value = formatApiError(error, '登录失败，请检查用户名或密码')
  }
}
</script>

<style scoped>
.login-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  background:
    linear-gradient(180deg, rgba(238, 242, 246, 0.88), rgba(238, 242, 246, 0.98)),
    url('/src/assets/hero.png') center/cover;
  padding: 24px;
}

.login-panel {
  width: min(440px, 100%);
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 18px 45px rgba(28, 39, 56, 0.12);
  padding: 32px;
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 28px;
}

.brand-mark {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 8px;
  background: #2f72ff;
  color: #fff;
  font-weight: 800;
}

h1 {
  margin: 0;
  color: #172033;
  font-size: 22px;
  font-weight: 750;
}

p {
  margin: 4px 0 0;
  color: #69778d;
  font-size: 14px;
}

.login-alert {
  margin-bottom: 16px;
}

.login-form {
  display: block;
}

.submit-button {
  width: 100%;
  margin-top: 8px;
}
</style>
