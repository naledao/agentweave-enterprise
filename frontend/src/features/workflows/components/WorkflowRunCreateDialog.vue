<template>
  <el-dialog
    :close-on-click-modal="!loading"
    :close-on-press-escape="!loading"
    destroy-on-close
    :model-value="modelValue"
    title="新建工作流运行"
    width="640px"
    @closed="resetForm"
    @close="close"
  >
    <el-alert
      v-if="errorMessage"
      class="dialog-error"
      :title="errorMessage"
      type="error"
      :closable="false"
      show-icon
    />

    <TraceIdText v-if="traceId" :trace-id="traceId" />

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      @submit.prevent
    >
      <el-form-item label="任务目标" prop="goal">
        <el-input
          v-model="form.goal"
          :disabled="loading"
          maxlength="5000"
          placeholder="输入要交给 Agent 拆解和执行的任务"
          :rows="8"
          show-word-limit
          type="textarea"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="loading" @click="close">取消</el-button>
      <el-button type="primary" :loading="loading" @click="submit">启动运行</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { reactive, ref } from 'vue'

import type { CreateWorkflowRunPayload } from '@/features/workflows/types'
import TraceIdText from '@/shared/components/TraceIdText.vue'

interface CreateWorkflowRunForm {
  goal: string
}

const props = withDefaults(defineProps<{
  modelValue: boolean
  loading?: boolean
  errorMessage?: string
  traceId?: string | null
}>(), {
  loading: false,
  errorMessage: '',
  traceId: null,
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [payload: CreateWorkflowRunPayload]
}>()

const formRef = ref<FormInstance>()
const form = reactive<CreateWorkflowRunForm>({
  goal: '',
})

const rules: FormRules<CreateWorkflowRunForm> = {
  goal: [
    {
      validator: (_rule, value: string, callback) => {
        if (!value.trim()) {
          callback(new Error('请输入任务目标'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
    { max: 5000, message: '任务目标不能超过 5000 个字符', trigger: 'blur' },
  ],
}

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  emit('submit', {
    conversationId: null,
    goal: form.goal.trim(),
  })
}

function close(): void {
  if (props.loading) {
    return
  }
  emit('update:modelValue', false)
}

function resetForm(): void {
  form.goal = ''
  formRef.value?.clearValidate()
}
</script>

<style scoped>
.dialog-error {
  margin-bottom: 14px;
}
</style>
