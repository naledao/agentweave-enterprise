<template>
  <form class="chat-input" @submit.prevent="submit">
    <el-input
      v-model="draft"
      :disabled="disabled"
      :rows="3"
      type="textarea"
      resize="none"
      maxlength="4000"
      show-word-limit
      placeholder="输入问题，Agent 会结合知识库、工具调用和执行状态回答"
      @keydown.enter.exact.prevent="submit"
    />
    <div class="input-actions">
      <el-button v-if="streaming" @click="$emit('cancel')">停止</el-button>
      <el-button type="primary" native-type="submit" :loading="loading" :disabled="disabled || !draft.trim()">
        发送
      </el-button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  disabled?: boolean
  loading?: boolean
  streaming?: boolean
}>()

const emit = defineEmits<{
  send: [content: string]
  cancel: []
}>()

const draft = ref('')

function submit(): void {
  const content = draft.value.trim()
  if (!content) {
    return
  }

  emit('send', content)
  draft.value = ''
}
</script>

<style scoped>
.chat-input {
  display: grid;
  flex: none;
  gap: 12px;
  border-top: 1px solid #e6eaf0;
  padding: 16px;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
